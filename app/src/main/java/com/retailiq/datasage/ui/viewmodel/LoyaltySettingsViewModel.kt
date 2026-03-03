package com.retailiq.datasage.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.LoyaltyProgramSettingsDto
import com.retailiq.datasage.data.repository.LoyaltyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed class LoyaltySettingsUiState {
    data object Loading : LoyaltySettingsUiState()
    data class Loaded(val settings: LoyaltyProgramSettingsDto) : LoyaltySettingsUiState()
    data class Saving(val settings: LoyaltyProgramSettingsDto) : LoyaltySettingsUiState()
    data class Success(val settings: LoyaltyProgramSettingsDto) : LoyaltySettingsUiState()
    data class Error(val message: String) : LoyaltySettingsUiState()
}

@HiltViewModel
class LoyaltySettingsViewModel @Inject constructor(
    private val loyaltyRepo: LoyaltyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoyaltySettingsUiState>(LoyaltySettingsUiState.Loading)
    val uiState: StateFlow<LoyaltySettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = LoyaltySettingsUiState.Loading
            when (val result = loyaltyRepo.getSettings()) {
                is NetworkResult.Success -> {
                    _uiState.value = LoyaltySettingsUiState.Loaded(result.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = LoyaltySettingsUiState.Error(result.message)
                }
                is NetworkResult.Loading -> Unit // Handled above
            }
        }
    }

    fun saveSettings(settings: LoyaltyProgramSettingsDto) {
        viewModelScope.launch {
            _uiState.value = LoyaltySettingsUiState.Saving(settings)
            when (val result = loyaltyRepo.updateSettings(settings)) {
                is NetworkResult.Success -> {
                    _uiState.value = LoyaltySettingsUiState.Success(result.data)
                }
                is NetworkResult.Error -> {
                    Timber.e("Failed to update loyalty settings: ${result.message}")
                    _uiState.value = LoyaltySettingsUiState.Error(result.message)
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun resetStateToLoaded() {
        val current = _uiState.value
        if (current is LoyaltySettingsUiState.Success) {
            _uiState.value = LoyaltySettingsUiState.Loaded(current.settings)
        } else if (current is LoyaltySettingsUiState.Error) {
            // Attempt to reload or just stay in error if initial load failed
            loadSettings()
        }
    }
}
