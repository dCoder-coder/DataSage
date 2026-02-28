package com.retailiq.datasage.ui.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.ForecastPoint
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.repository.ForecastRepository
import com.retailiq.datasage.ui.components.HistoricalPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed class ForecastUiState {
    data object Loading : ForecastUiState()
    data class Loaded(
        val historical: List<HistoricalPoint>,
        val forecast: List<ForecastPoint>,
        val confidenceTier: String
    ) : ForecastUiState()
    data class Error(val message: String) : ForecastUiState()
}

@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val repository: ForecastRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<ForecastUiState>(ForecastUiState.Loading)
    val uiState: StateFlow<ForecastUiState> = _uiState.asStateFlow()

    init {
        loadForecast()
    }

    fun loadForecast() = viewModelScope.launch {
        _uiState.value = ForecastUiState.Loading
        when (val result = repository.getStoreForecast()) {
            is NetworkResult.Success -> {
                // Determine confidence tier based on the length or arbitrary mock since payload doesn't provide it
                val confidence = if (result.data.size > 14) "Prophet (high)" else if (result.data.isNotEmpty()) "Ridge (medium)" else "Insufficient data"
                
                // Mocking some historical points since the payload only returns forecast naturally
                val historical = listOf(
                    HistoricalPoint("D-2", 100.0),
                    HistoricalPoint("D-1", 120.0),
                    HistoricalPoint("Today", 110.0)
                )

                _uiState.value = ForecastUiState.Loaded(historical, result.data, confidence)
            }
            is NetworkResult.Error -> {
                Timber.w("Forecast load failed: %s", result.message)
                _uiState.value = ForecastUiState.Error(result.message)
            }
            is NetworkResult.Loading -> Unit
        }
    }
}
