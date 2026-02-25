package com.retailiq.datasage.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.AlertsApiService
import com.retailiq.datasage.data.api.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AlertItem(
    val id: String = "",
    val type: String = "",
    val severity: String = "info",
    val message: String = "",
    val createdAt: String = ""
)

sealed class AlertsUiState {
    data object Loading : AlertsUiState()
    data class Loaded(val alerts: List<AlertItem>) : AlertsUiState()
    data class Error(val message: String) : AlertsUiState()
}

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertsApi: AlertsApiService
) : ViewModel() {
    private val _uiState = MutableStateFlow<AlertsUiState>(AlertsUiState.Loading)
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init { loadAlerts() }

    fun loadAlerts() = viewModelScope.launch {
        _uiState.value = AlertsUiState.Loading
        try {
            val response = alertsApi.listAlerts()
            if (response.success && response.data != null) {
                val alerts = response.data.map { map ->
                    AlertItem(
                        id = map["alert_id"]?.toString() ?: "",
                        type = map["alert_type"]?.toString() ?: "",
                        severity = map["priority"]?.toString() ?: "info",
                        message = map["message"]?.toString() ?: "",
                        createdAt = map["created_at"]?.toString() ?: ""
                    )
                }
                _uiState.value = AlertsUiState.Loaded(alerts)
            } else {
                _uiState.value = AlertsUiState.Error(response.error?.message ?: "Failed to load alerts")
            }
        } catch (e: Exception) {
            Timber.e(e, "Alerts API error")
            _uiState.value = AlertsUiState.Error(e.message ?: "Failed to load alerts")
        }
    }
}
