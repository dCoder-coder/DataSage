package com.retailiq.datasage.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.WhatsAppLogDto
import com.retailiq.datasage.data.repository.WhatsAppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class WhatsAppLogUiState {
    data object Loading : WhatsAppLogUiState()
    data class Success(val logs: List<WhatsAppLogDto>) : WhatsAppLogUiState()
    data class Error(val message: String) : WhatsAppLogUiState()
}

@HiltViewModel
class WhatsAppLogViewModel @Inject constructor(
    private val repository: WhatsAppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WhatsAppLogUiState>(WhatsAppLogUiState.Loading)
    val uiState: StateFlow<WhatsAppLogUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadLogs()
    }

    fun loadLogs() = viewModelScope.launch {
        _uiState.value = WhatsAppLogUiState.Loading
        fetchFromApi()
    }

    fun refreshLogs() = viewModelScope.launch {
        _isRefreshing.value = true
        fetchFromApi()
        _isRefreshing.value = false
    }

    private suspend fun fetchFromApi() {
        when (val result = repository.getLogs(page = 1, pageSize = 50)) {
            is NetworkResult.Success -> {
                _uiState.value = WhatsAppLogUiState.Success(result.data.logs)
            }
            is NetworkResult.Error -> {
                _uiState.value = WhatsAppLogUiState.Error(result.message)
            }
            is NetworkResult.Loading -> Unit
        }
    }
}
