package com.retailiq.datasage.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.Product
import com.retailiq.datasage.data.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed class InventoryUiState {
    data object Loading : InventoryUiState()
    data class Loaded(val products: List<Product>) : InventoryUiState()
    data class Error(val message: String) : InventoryUiState()
}

sealed class ProductCreateState {
    data object Idle : ProductCreateState()
    data object Saving : ProductCreateState()
    data class Success(val product: Product) : ProductCreateState()
    data class Error(val message: String) : ProductCreateState()
}

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: InventoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<InventoryUiState>(InventoryUiState.Loading)
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _stockUpdateState = MutableStateFlow<NetworkResult<com.retailiq.datasage.data.api.StockUpdateResponse>?>(null)
    val stockUpdateState: StateFlow<NetworkResult<com.retailiq.datasage.data.api.StockUpdateResponse>?> = _stockUpdateState.asStateFlow()

    private val _auditState = MutableStateFlow<NetworkResult<com.retailiq.datasage.data.api.AuditResponse>?>(null)
    val auditState: StateFlow<NetworkResult<com.retailiq.datasage.data.api.AuditResponse>?> = _auditState.asStateFlow()

    private val _createState = MutableStateFlow<ProductCreateState>(ProductCreateState.Idle)
    val createState: StateFlow<ProductCreateState> = _createState.asStateFlow()

    init { loadProducts() }

    fun loadProducts() = viewModelScope.launch {
        _uiState.value = InventoryUiState.Loading
        when (val result = repository.getProducts(forceRefresh = true)) {
            is NetworkResult.Success -> _uiState.value = InventoryUiState.Loaded(result.data)
            is NetworkResult.Error -> _uiState.value = InventoryUiState.Error(result.message)
            is NetworkResult.Loading -> Unit
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
        val filtered = repository.searchCached(query)
        _uiState.value = InventoryUiState.Loaded(filtered)
    }

    /**
     * Creates a new product via API and automatically refreshes the product list on success.
     * This is called from ProductFormScreen via AppNav's inventory/add composable.
     */
    fun createProduct(
        name: String,
        costPrice: Double,
        sellingPrice: Double,
        hsnCode: String? = null,
        gstRate: Double? = null
    ) = viewModelScope.launch {
        if (name.isBlank()) {
            _createState.value = ProductCreateState.Error("Product name is required")
            return@launch
        }
        _createState.value = ProductCreateState.Saving
        when (val result = repository.createProduct(name, costPrice, sellingPrice, hsnCode, gstRate)) {
            is NetworkResult.Success -> {
                _createState.value = ProductCreateState.Success(result.data)
                // Reload the product list so the new product appears immediately
                loadProducts()
            }
            is NetworkResult.Error -> {
                _createState.value = ProductCreateState.Error(result.message)
                Timber.e("createProduct failed: ${result.message}")
            }
            is NetworkResult.Loading -> Unit
        }
    }

    fun resetCreateState() { _createState.value = ProductCreateState.Idle }

    fun submitStockUpdate(productId: Int, request: com.retailiq.datasage.data.api.StockUpdateRequest) = viewModelScope.launch {
        _stockUpdateState.value = NetworkResult.Loading()
        _stockUpdateState.value = repository.updateStock(productId, request)
        if (_stockUpdateState.value is NetworkResult.Success) {
            loadProducts()
        }
    }

    fun resetStockUpdateState() {
        _stockUpdateState.value = null
    }

    fun submitAudit(request: com.retailiq.datasage.data.api.AuditRequest) = viewModelScope.launch {
        _auditState.value = NetworkResult.Loading()
        _auditState.value = repository.submitAudit(request)
        if (_auditState.value is NetworkResult.Success) {
            loadProducts()
        }
    }

    fun resetAuditState() {
        _auditState.value = null
    }
}
