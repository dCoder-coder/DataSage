package com.retailiq.datasage.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.retailiq.datasage.core.ConnectivityObserver
import com.retailiq.datasage.data.api.DailySummary
import com.retailiq.datasage.data.api.DashboardPayload
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.local.AnalyticsSnapshot
import com.retailiq.datasage.data.local.AnalyticsSnapshotDao
import com.retailiq.datasage.data.local.LocalKpiEngine
import com.retailiq.datasage.data.local.PendingTransactionDao
import com.retailiq.datasage.data.model.SnapshotDto
import com.retailiq.datasage.data.repository.DashboardRepository
import com.retailiq.datasage.ui.components.CategoryBreakdown
import com.retailiq.datasage.ui.components.PaymentModeBreakdown
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data class Loaded(val dashboardData: DashboardPayload) : DashboardUiState()
    data class Offline(val snapshot: AnalyticsSnapshot, val kpiEngine: LocalKpiEngine) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val snapshotDao: AnalyticsSnapshotDao,
    private val gson: Gson,
    pendingDao: PendingTransactionDao
) : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _categoryBreakdown = MutableStateFlow<List<CategoryBreakdown>>(emptyList())
    val categoryBreakdown: StateFlow<List<CategoryBreakdown>> = _categoryBreakdown.asStateFlow()

    private val _paymentModes = MutableStateFlow<List<PaymentModeBreakdown>>(emptyList())
    val paymentModes: StateFlow<List<PaymentModeBreakdown>> = _paymentModes.asStateFlow()

    val pendingCount: StateFlow<Int> = pendingDao.countByStatusFlow("pending")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val failedCount: StateFlow<Int> = pendingDao.countByStatusFlow("failed")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { isOnline ->
                if (isOnline) {
                    refresh()
                } else {
                    loadOfflineSnapshot()
                }
            }
        }
    }

    private fun loadOfflineSnapshot() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            try {
                val entity = snapshotDao.getSnapshot("my_store")
                if (entity != null) {
                    val dto = gson.fromJson(entity.snapshotJson, SnapshotDto::class.java)
                    _uiState.value = DashboardUiState.Offline(entity, LocalKpiEngine(dto))
                } else {
                    _uiState.value = DashboardUiState.Error("No offline data available. Please connect to the internet.")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading offline snapshot")
                _uiState.value = DashboardUiState.Error("Failed to render offline dashboard.")
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        _uiState.value = DashboardUiState.Loading
        when (val result = repository.getDashboard()) {
            is NetworkResult.Success -> {
                _uiState.value = DashboardUiState.Loaded(result.data)
            }
            is NetworkResult.Error -> {
                Timber.w("Dashboard load failed: %s", result.message)
                _uiState.value = DashboardUiState.Error(result.message)
            }
            is NetworkResult.Loading -> Unit
        }

        // Fetch category breakdown
        when (val catResult = repository.getCategoryBreakdown()) {
            is NetworkResult.Success -> _categoryBreakdown.value = catResult.data
            is NetworkResult.Error -> Timber.w("Category breakdown failed: %s", catResult.message)
            is NetworkResult.Loading -> Unit
        }

        // Fetch payment modes
        when (val payResult = repository.getPaymentModes()) {
            is NetworkResult.Success -> _paymentModes.value = payResult.data
            is NetworkResult.Error -> Timber.w("Payment modes failed: %s", payResult.message)
            is NetworkResult.Loading -> Unit
        }
    }
}

