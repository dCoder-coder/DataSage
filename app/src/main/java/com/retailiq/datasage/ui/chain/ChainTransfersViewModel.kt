package com.retailiq.datasage.ui.chain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.TransferSuggestionDto
import com.retailiq.datasage.data.repository.ChainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ChainTransfersUiState {
    data object Loading : ChainTransfersUiState()
    data class Loaded(val transfers: List<TransferSuggestionDto>) : ChainTransfersUiState()
    data class Error(val message: String) : ChainTransfersUiState()
}

@HiltViewModel
class ChainTransfersViewModel @Inject constructor(
    private val repository: ChainRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChainTransfersUiState>(ChainTransfersUiState.Loading)
    val uiState: StateFlow<ChainTransfersUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _uiState.value = ChainTransfersUiState.Loading
        when (val result = repository.getTransfers()) {
            is NetworkResult.Success -> _uiState.value = ChainTransfersUiState.Loaded(
                result.data.filter { it.status == "PENDING" }
            )
            is NetworkResult.Error -> _uiState.value = ChainTransfersUiState.Error(result.message)
            is NetworkResult.Loading -> Unit
        }
    }

    fun markDone(id: String) = viewModelScope.launch {
        // Optimistic removal
        val current = (_uiState.value as? ChainTransfersUiState.Loaded) ?: return@launch
        _uiState.value = ChainTransfersUiState.Loaded(current.transfers.filter { it.id != id })
        repository.confirmTransfer(id)
    }
}
