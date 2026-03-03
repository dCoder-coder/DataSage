package com.retailiq.datasage.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.WhatsAppConfigDto
import com.retailiq.datasage.data.repository.WhatsAppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class WhatsAppConfigUiState {
    data object Loading : WhatsAppConfigUiState()
    data class Success(val config: WhatsAppConfigDto) : WhatsAppConfigUiState()
    data class Error(val message: String) : WhatsAppConfigUiState()
}

@HiltViewModel
class WhatsAppConfigViewModel @Inject constructor(
    private val repository: WhatsAppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WhatsAppConfigUiState>(WhatsAppConfigUiState.Loading)
    val uiState: StateFlow<WhatsAppConfigUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<String?>(null)
    val saveState: StateFlow<String?> = _saveState.asStateFlow()

    private val _testMessageState = MutableStateFlow<String?>(null)
    val testMessageState: StateFlow<String?> = _testMessageState.asStateFlow()

    init {
        loadConfig()
    }

    private fun loadConfig() = viewModelScope.launch {
        _uiState.value = WhatsAppConfigUiState.Loading
        when (val result = repository.getConfig()) {
            is NetworkResult.Success -> {
                _uiState.value = WhatsAppConfigUiState.Success(result.data)
            }
            is NetworkResult.Error -> {
                // If not found or error, default to empty to allow creation
                _uiState.value = WhatsAppConfigUiState.Success(
                    WhatsAppConfigDto("", "", null, "", false)
                )
            }
            is NetworkResult.Loading -> Unit
        }
    }

    fun saveConfig(config: WhatsAppConfigDto) = viewModelScope.launch {
        _saveState.value = "Saving..."
        when (val result = repository.updateConfig(config)) {
            is NetworkResult.Success -> {
                _uiState.value = WhatsAppConfigUiState.Success(result.data)
                _saveState.value = "Settings saved successfully"
            }
            is NetworkResult.Error -> {
                _saveState.value = "Error: ${result.message}"
            }
            is NetworkResult.Loading -> Unit
        }
    }

    fun testConnection() = viewModelScope.launch {
        _testMessageState.value = "Sending test message..."
        // using a dummy alert ID for testing connection
        when (val result = repository.sendAlert(alertId = 1)) {
            is NetworkResult.Success -> {
                _testMessageState.value = "✓ Message sent"
            }
            is NetworkResult.Error -> {
                _testMessageState.value = "Error: ${result.message}"
            }
            is NetworkResult.Loading -> Unit
        }
    }

    fun clearMessages() {
        _saveState.value = null
        _testMessageState.value = null
    }
}
