package com.retailiq.datasage.ui.chain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.StoreCompareResponseDto
import com.retailiq.datasage.data.repository.ChainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StoreCompareUiState {
    data object Loading : StoreCompareUiState()
    data class Loaded(val data: StoreCompareResponseDto) : StoreCompareUiState()
    data class Error(val message: String) : StoreCompareUiState()
}

@HiltViewModel
class StoreCompareViewModel @Inject constructor(
    private val repository: ChainRepository
) : ViewModel() {

    val periods = listOf("today", "week", "month")
    val periodLabels = listOf("Today", "This Week", "This Month")

    private val _selectedPeriod = MutableStateFlow("today")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _uiState = MutableStateFlow<StoreCompareUiState>(StoreCompareUiState.Loading)
    val uiState: StateFlow<StoreCompareUiState> = _uiState.asStateFlow()

    init { load("today") }

    fun selectPeriod(period: String) {
        _selectedPeriod.value = period
        load(period)
    }

    private fun load(period: String) = viewModelScope.launch {
        _uiState.value = StoreCompareUiState.Loading
        when (val result = repository.getComparison(period)) {
            is NetworkResult.Success -> _uiState.value = StoreCompareUiState.Loaded(result.data)
            is NetworkResult.Error -> _uiState.value = StoreCompareUiState.Error(result.message)
            is NetworkResult.Loading -> Unit
        }
    }
}
