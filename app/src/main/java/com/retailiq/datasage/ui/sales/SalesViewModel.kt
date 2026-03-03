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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import com.retailiq.datasage.data.api.Customer
import com.retailiq.datasage.data.api.CustomerApiService
import com.retailiq.datasage.data.model.CreditAccountDto
import com.retailiq.datasage.data.model.LoyaltyAccountDto
import com.retailiq.datasage.data.repository.CreditRepository
import com.retailiq.datasage.data.repository.LoyaltyRepository
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
    private val transactionRepository: TransactionRepository,
    private val customerApi: CustomerApiService,
    private val loyaltyRepo: LoyaltyRepository,
    private val creditRepo: CreditRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _saleState = MutableStateFlow<SaleUiState>(SaleUiState.Idle)
    val saleState: StateFlow<SaleUiState> = _saleState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    private val _loyaltyAccount = MutableStateFlow<LoyaltyAccountDto?>(null)
    val loyaltyAccount: StateFlow<LoyaltyAccountDto?> = _loyaltyAccount.asStateFlow()

    private val _creditAccount = MutableStateFlow<CreditAccountDto?>(null)
    val creditAccount: StateFlow<CreditAccountDto?> = _creditAccount.asStateFlow()

    private val _redemptionPoints = MutableStateFlow(0)
    val redemptionPoints: StateFlow<Int> = _redemptionPoints.asStateFlow()

    var lastTransactionId: String? = null
        private set

    init {
        loadProducts()
    }

    fun loadProducts() = viewModelScope.launch {
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

    fun searchCustomers(query: String) = viewModelScope.launch {
        if (query.length < 2) {
            _customers.value = emptyList()
            return@launch
        }
        try {
            val res = customerApi.listCustomers(search = query)
            if (res.success && res.data != null) {
                _customers.value = res.data
            }
        } catch (e: Exception) {
            Timber.e(e, "Error searching customers")
        }
    }

    fun selectCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
        _loyaltyAccount.value = null
        _creditAccount.value = null
        _redemptionPoints.value = 0
        _customers.value = emptyList()
        if (customer != null) {
            viewModelScope.launch {
                when (val lRes = loyaltyRepo.getAccount(customer.customerId)) {
                    is NetworkResult.Success -> _loyaltyAccount.value = lRes.data
                    else -> Unit
                }
                when (val cRes = creditRepo.getAccount(customer.customerId)) {
                    is NetworkResult.Success -> _creditAccount.value = cRes.data
                    else -> Unit
                }
            }
        }
    }

    fun setRedemptionPoints(points: Int) {
        _redemptionPoints.value = points
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

    val cartTotal: Double get() {
        val base = _cart.value.sumOf { it.lineTotal }
        val discount = _loyaltyAccount.value?.let { acc ->
            if (acc.redeemablePoints > 0) {
                (_redemptionPoints.value.toDouble() / acc.redeemablePoints) * acc.valueInCurrency
            } else 0.0
        } ?: 0.0
        return maxOf(0.0, base - discount)
    }

    val gstTotal: Double get() {
        return _cart.value.sumOf { item ->
            val rate = item.product.taxRate ?: 0.0
            item.lineTotal * (rate / 100.0)
        }
    }

    val loyaltyDiscount: Double get() {
        val base = _cart.value.sumOf { it.lineTotal }
        return base - cartTotal
    }

    fun submitSale(paymentMode: String) = viewModelScope.launch {
        val items = _cart.value
        if (items.isEmpty()) {
            _saleState.value = SaleUiState.Error("Cart is empty")
            return@launch
        }
        
        _saleState.value = SaleUiState.Submitting
        
        val payload = mutableMapOf<String, Any>(
            "transaction_id" to UUID.randomUUID().toString(),
            "timestamp" to DateTimeFormatter.ISO_LOCAL_DATE_TIME
                .withZone(ZoneId.of("Asia/Kolkata"))
                .format(Instant.now()),
            "line_items" to items.map { item ->
                mapOf(
                    "product_id" to item.product.productId,
                    "quantity" to item.quantity,
                    "selling_price" to item.product.sellingPrice,
                    "discount_amount" to item.discount
                )
            },
            "payment_mode" to paymentMode.uppercase()
        )
        
        _selectedCustomer.value?.let {
            payload["customer_id"] = it.customerId
        }
        // Removed adding loyalty points to payload because backend BatchTransactionsRequest schema rejects unknown fields
        
        try {
            val txId = transactionRepository.createSaleOffline(payload)
            lastTransactionId = txId
            Timber.d("Sale submitted offline: %s", txId)
            
            // Deduct stock locally so UI updates instantly
            val soldQuantities = items.associate { it.product.productId to it.quantity }
            inventoryRepository.deductStockLocally(soldQuantities)
            
            // Post-sale loyalty redemption
            val custId = _selectedCustomer.value?.customerId
            val pts = _redemptionPoints.value
            if (custId != null && pts > 0) {
                val redeemRes = loyaltyRepo.redeemPoints(custId, txId, pts)
                if (redeemRes is NetworkResult.Error) {
                    Timber.w("Network error redeeming points, backend should sync: ${redeemRes.message}")
                    // we show Success anyway as instructed: "do NOT rollback the sale"
                }
            }
            
            _saleState.value = SaleUiState.Success(txId)
            _cart.value = emptyList()
            selectCustomer(null) // clear customer state
        } catch (e: Exception) {
            Timber.e(e, "Failed to save sale")
            _saleState.value = SaleUiState.Error(e.message ?: "Failed to save sale")
        }
    }

    fun resetSaleState() {
        _saleState.value = SaleUiState.Idle
    }
}
