package com.retailiq.datasage.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.Product
import com.retailiq.datasage.data.repository.InventoryRepository
import com.retailiq.datasage.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class CartItem(
    val product: Product,
    val quantity: Double = 1.0,
    val discount: Double = 0.0
) {
    val lineTotal: Double get() = (product.sellingPrice * quantity) - discount
}

sealed class SaleUiState {
    data object Idle : SaleUiState()
    data object Submitting : SaleUiState()
    data class Success(val transactionId: String) : SaleUiState()
    data class Error(val message: String) : SaleUiState()
}

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _saleState = MutableStateFlow<SaleUiState>(SaleUiState.Idle)
    val saleState: StateFlow<SaleUiState> = _saleState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadProducts()
    }

    private fun loadProducts() = viewModelScope.launch {
        when (val result = inventoryRepository.getProducts()) {
            is NetworkResult.Success -> _products.value = result.data
            is NetworkResult.Error -> Timber.w("Failed to load products: %s", result.message)
            is NetworkResult.Loading -> Unit
        }
    }

    fun searchProducts(query: String) {
        _searchQuery.value = query
        _products.value = inventoryRepository.searchCached(query)
    }

    fun addToCart(product: Product) {
        val current = _cart.value.toMutableList()
        val existing = current.indexOfFirst { it.product.productId == product.productId }
        if (existing >= 0) {
            current[existing] = current[existing].copy(quantity = current[existing].quantity + 1)
        } else {
            current.add(CartItem(product))
        }
        _cart.value = current
    }

    fun removeFromCart(productId: Int) {
        _cart.value = _cart.value.filter { it.product.productId != productId }
    }

    fun updateQuantity(productId: Int, quantity: Double) {
        if (quantity <= 0) {
            removeFromCart(productId)
            return
        }
        _cart.value = _cart.value.map { item ->
            if (item.product.productId == productId) item.copy(quantity = quantity) else item
        }
    }

    val cartTotal: Double get() = _cart.value.sumOf { it.lineTotal }

    fun submitSale(paymentMode: String) = viewModelScope.launch {
        val items = _cart.value
        if (items.isEmpty()) {
            _saleState.value = SaleUiState.Error("Cart is empty")
            return@launch
        }

        _saleState.value = SaleUiState.Submitting

        val payload = mapOf<String, Any>(
            "items" to items.map { item ->
                mapOf(
                    "product_id" to item.product.productId,
                    "quantity" to item.quantity,
                    "selling_price" to item.product.sellingPrice,
                    "discount_amount" to item.discount
                )
            },
            "payment_mode" to paymentMode
        )

        try {
            val txId = transactionRepository.createSaleOffline(payload)
            _saleState.value = SaleUiState.Success(txId)
            _cart.value = emptyList()
            Timber.d("Sale submitted offline: %s", txId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save sale")
            _saleState.value = SaleUiState.Error(e.message ?: "Failed to save sale")
        }
    }

    fun resetSaleState() {
        _saleState.value = SaleUiState.Idle
    }
}
