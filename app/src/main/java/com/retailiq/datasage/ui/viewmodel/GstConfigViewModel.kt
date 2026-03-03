package com.retailiq.datasage.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.GstConfigDto
import com.retailiq.datasage.data.repository.GstRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class GstConfigUiState {
    data object Loading : GstConfigUiState()
    data class Success(val message: String? = null) : GstConfigUiState()
    data class Error(val message: String) : GstConfigUiState()
}

@HiltViewModel
class GstConfigViewModel @Inject constructor(
    private val repository: GstRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GstConfigUiState>(GstConfigUiState.Loading)
    val uiState: StateFlow<GstConfigUiState> = _uiState.asStateFlow()

    private val _gstEnabled = MutableStateFlow(false)
    val gstEnabled: StateFlow<Boolean> = _gstEnabled.asStateFlow()

    private val _registrationType = MutableStateFlow("REGULAR")
    val registrationType: StateFlow<String> = _registrationType.asStateFlow()

    private val _stateCode = MutableStateFlow("")
    val stateCode: StateFlow<String> = _stateCode.asStateFlow()

    private val _gstin = MutableStateFlow("")
    val gstin: StateFlow<String> = _gstin.asStateFlow()

    private val _isValidGstin = MutableStateFlow<Boolean?>(null)
    val isValidGstin: StateFlow<Boolean?> = _isValidGstin.asStateFlow()

    init {
        loadConfig()
    }

    private fun loadConfig() = viewModelScope.launch {
        _uiState.value = GstConfigUiState.Loading
        when (val result = repository.getConfig()) {
            is NetworkResult.Success -> {
                val config = result.data
                _gstEnabled.value = config.isGstEnabled
                _registrationType.value = config.registrationType
                _stateCode.value = config.stateCode ?: ""
                _gstin.value = config.gstin ?: ""
                validateGstinFormat(config.gstin ?: "")
                _uiState.value = GstConfigUiState.Success()
            }
            is NetworkResult.Error -> {
                // If 404/not found, we can just treat it as empty config rather than hard error
                _uiState.value = GstConfigUiState.Success()
            }
            is NetworkResult.Loading -> Unit
        }
    }

    fun updateGstEnabled(enabled: Boolean) {
        _gstEnabled.value = enabled
    }

    fun updateRegistrationType(type: String) {
        _registrationType.value = type
    }

    fun updateStateCode(code: String) {
        _stateCode.value = code
    }

    fun updateGstin(newGstin: String) {
        _gstin.value = newGstin.uppercase()
        // Reset validation until blur
        _isValidGstin.value = null
    }

    fun validateGstinFormat(gstinStr: String) {
        if (gstinStr.isBlank()) {
            _isValidGstin.value = null
            return
        }
        // Basic GSTIN format: 2 chars state code + 10 PAN + 1 Entity + Z + 1 checksum char (total 15)
        val regex = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
        _isValidGstin.value = regex.matches(gstinStr)
    }

    fun saveConfig() = viewModelScope.launch {
        _uiState.value = GstConfigUiState.Loading
        val config = GstConfigDto(
            isGstEnabled = _gstEnabled.value,
            registrationType = _registrationType.value,
            stateCode = _stateCode.value.ifBlank { null },
            gstin = _gstin.value.ifBlank { null }
        )
        when (val result = repository.updateConfig(config)) {
            is NetworkResult.Success -> {
                _uiState.value = GstConfigUiState.Success("GST Settings saved successfully")
            }
            is NetworkResult.Error -> {
                _uiState.value = GstConfigUiState.Error(result.message)
            }
            is NetworkResult.Loading -> Unit
        }
    }

    fun clearMessage() {
        if (_uiState.value is GstConfigUiState.Success || _uiState.value is GstConfigUiState.Error) {
            _uiState.value = GstConfigUiState.Success()
        }
    }
}
