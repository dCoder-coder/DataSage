package com.retailiq.datasage.ui.chain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.ChainDashboardDto
import com.retailiq.datasage.data.model.TransferSuggestionDto
import com.retailiq.datasage.data.repository.ChainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ChainDashboardUiState {
    data object Loading : ChainDashboardUiState()
    data class Loaded(val dashboard: ChainDashboardDto) : ChainDashboardUiState()
    data class Error(val message: String) : ChainDashboardUiState()
}

@HiltViewModel
class ChainDashboardViewModel @Inject constructor(
    private val repository: ChainRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChainDashboardUiState>(ChainDashboardUiState.Loading)
    val uiState: StateFlow<ChainDashboardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init { loadDashboard() }

    fun loadDashboard(fromRefresh: Boolean = false) = viewModelScope.launch {
        if (fromRefresh) _isRefreshing.value = true
        else _uiState.value = ChainDashboardUiState.Loading
        when (val result = repository.getDashboard()) {
            is NetworkResult.Success -> _uiState.value = ChainDashboardUiState.Loaded(result.data)
            is NetworkResult.Error -> _uiState.value = ChainDashboardUiState.Error(result.message)
            is NetworkResult.Loading -> Unit
        }
        _isRefreshing.value = false
    }

    fun markTransferDone(transferId: String) = viewModelScope.launch {
        repository.confirmTransfer(transferId)
        // Optimistically remove from local state
        val current = (_uiState.value as? ChainDashboardUiState.Loaded) ?: return@launch
        val updated = current.dashboard.copy(
            transfer_suggestions = current.dashboard.transfer_suggestions.filter { it.id != transferId }
        )
        _uiState.value = ChainDashboardUiState.Loaded(updated)
    }
}
