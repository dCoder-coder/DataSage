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

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: InventoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<InventoryUiState>(InventoryUiState.Loading)
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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
}
