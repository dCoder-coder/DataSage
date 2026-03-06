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
import com.retailiq.datasage.data.repository.WhatsAppRepository

data class AlertItem(
    val id: String = "",
    val type: String = "",
    val severity: String = "info",
    val message: String = "",
    val createdAt: String = "",
    val metadata: Map<String, Any>? = null
)

sealed class AlertsUiState {
    data object Loading : AlertsUiState()
    data class Loaded(val alerts: List<AlertItem>) : AlertsUiState()
    data class Error(val message: String) : AlertsUiState()
}

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertsApi: AlertsApiService,
    private val whatsappRepository: WhatsAppRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<AlertsUiState>(AlertsUiState.Loading)
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    private val _whatsappEnabled = MutableStateFlow(false)
    val whatsappEnabled: StateFlow<Boolean> = _whatsappEnabled.asStateFlow()

    init { 
        loadAlerts() 
        checkWhatsappStatus()
    }

    private fun checkWhatsappStatus() = viewModelScope.launch {
        val result = whatsappRepository.getConfig()
        if (result is NetworkResult.Success) {
            _whatsappEnabled.value = result.data.is_active
        }
    }

    fun sendWhatsAppAlert(alertId: Int) = viewModelScope.launch {
        whatsappRepository.sendAlert(alertId = alertId)
    }

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
                        createdAt = map["created_at"]?.toString() ?: "",
                        metadata = map["metadata"] as? Map<String, Any>
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

    fun dismissAlert(alertId: String) = viewModelScope.launch {
        // Optimistically remove from UI
        val current = _uiState.value
        if (current is AlertsUiState.Loaded) {
            _uiState.value = AlertsUiState.Loaded(current.alerts.filter { it.id != alertId })
        }
        try {
            val id = alertId.toIntOrNull() ?: return@launch
            alertsApi.dismissAlert(id)
            Timber.d("Alert $alertId dismissed")
        } catch (e: Exception) {
            Timber.e(e, "Failed to dismiss alert $alertId — reloading")
            loadAlerts() // Restore on failure
        }
    }
}
