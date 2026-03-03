package com.retailiq.datasage.ui.pricing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _priceHistoryState = MutableStateFlow<PriceHistoryState>(PriceHistoryState.Loading)
    val priceHistoryState: StateFlow<PriceHistoryState> = _priceHistoryState.asStateFlow()

    fun loadPriceHistory(productId: Int) = viewModelScope.launch {
        _priceHistoryState.value = PriceHistoryState.Loading
        when (val result = inventoryRepository.getPriceHistory(productId)) {
            is NetworkResult.Success -> _priceHistoryState.value = PriceHistoryState.Loaded(result.data)
            is NetworkResult.Error   -> _priceHistoryState.value = PriceHistoryState.Error(result.message)
            is NetworkResult.Loading -> Unit
        }
    }
}
