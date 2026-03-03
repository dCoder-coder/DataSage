package com.retailiq.datasage.ui.pricing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.PricingSuggestion
import com.retailiq.datasage.data.repository.PricingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class PricingUiState {
    data object Loading : PricingUiState()
    data class Loaded(val suggestions: List<PricingSuggestion>) : PricingUiState()
    data class Error(val message: String) : PricingUiState()
}

sealed class PricingActionState {
    data object Idle : PricingActionState()
    data object InFlight : PricingActionState()
    data class Success(val message: String) : PricingActionState()
    data class Error(val message: String) : PricingActionState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class PricingViewModel @Inject constructor(
    private val repository: PricingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PricingUiState>(PricingUiState.Loading)
    val uiState: StateFlow<PricingUiState> = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow<PricingActionState>(PricingActionState.Idle)
    val actionState: StateFlow<PricingActionState> = _actionState.asStateFlow()

    init { loadSuggestions() }

    fun loadSuggestions() = viewModelScope.launch {
        _uiState.value = PricingUiState.Loading
        when (val result = repository.getSuggestions()) {
            is NetworkResult.Success -> _uiState.value = PricingUiState.Loaded(result.data)
            is NetworkResult.Error   -> _uiState.value = PricingUiState.Error(result.message)
            is NetworkResult.Loading -> Unit
        }
    }

    /**
     * Applies a pricing suggestion. On success, the card is removed from the list state.
     */
    fun applySuggestion(id: Int) = viewModelScope.launch {
        _actionState.value = PricingActionState.InFlight
        when (val result = repository.applySuggestion(id)) {
            is NetworkResult.Success -> {
                removeSuggestion(id)
                _actionState.value = PricingActionState.Success("Price updated successfully")
            }
            is NetworkResult.Error -> {
                Timber.e("Apply suggestion failed: ${result.message}")
                _actionState.value = PricingActionState.Error(result.message)
            }
            is NetworkResult.Loading -> Unit
        }
    }

    /**
     * Dismisses a pricing suggestion directly (no confirmation). Card is removed on success.
     */
    fun dismissSuggestion(id: Int) = viewModelScope.launch {
        _actionState.value = PricingActionState.InFlight
        when (val result = repository.dismissSuggestion(id)) {
            is NetworkResult.Success -> {
                removeSuggestion(id)
                _actionState.value = PricingActionState.Success("Suggestion dismissed")
            }
            is NetworkResult.Error -> {
                Timber.e("Dismiss suggestion failed: ${result.message}")
                _actionState.value = PricingActionState.Error(result.message)
            }
            is NetworkResult.Loading -> Unit
        }
    }

    /** Clears action state back to Idle (call after consuming snackbar/toast). */
    fun resetAction() {
        _actionState.value = PricingActionState.Idle
    }

    private fun removeSuggestion(id: Int) {
        val current = _uiState.value
        if (current is PricingUiState.Loaded) {
            _uiState.value = PricingUiState.Loaded(
                current.suggestions.filter { it.id != id }
            )
        }
    }
}
