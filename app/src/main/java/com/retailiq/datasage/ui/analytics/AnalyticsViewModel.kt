package com.retailiq.datasage.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.DashboardPayload
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.repository.DashboardRepository
import com.retailiq.datasage.ui.components.CategoryBreakdown
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.google.gson.Gson
import com.retailiq.datasage.core.ConnectivityObserver
import com.retailiq.datasage.data.local.AnalyticsSnapshot
import com.retailiq.datasage.data.local.AnalyticsSnapshotDao
import com.retailiq.datasage.data.local.LocalKpiEngine
import com.retailiq.datasage.data.model.SnapshotDto

sealed class AnalyticsUiState {
    data object Loading : AnalyticsUiState()
    data class Loaded(val data: DashboardPayload) : AnalyticsUiState()
    data class Offline(val snapshot: AnalyticsSnapshot, val kpiEngine: LocalKpiEngine) : AnalyticsUiState()
    data class Error(val message: String) : AnalyticsUiState()
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val snapshotDao: AnalyticsSnapshotDao,
    private val gson: Gson
) : ViewModel() {
    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val _categoryBreakdown = MutableStateFlow<List<CategoryBreakdown>>(emptyList())
    val categoryBreakdown: StateFlow<List<CategoryBreakdown>> = _categoryBreakdown.asStateFlow()

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { isOnline ->
                if (isOnline) {
                    loadAnalytics()
                } else {
                    loadOfflineSnapshot()
                }
            }
        }
    }

    private fun loadOfflineSnapshot() = viewModelScope.launch {
        _uiState.value = AnalyticsUiState.Loading
        try {
            val entity = snapshotDao.getSnapshot("my_store")
            if (entity != null) {
                val dto = gson.fromJson(entity.snapshotJson, SnapshotDto::class.java)
                _uiState.value = AnalyticsUiState.Offline(entity, LocalKpiEngine(dto))
            } else {
                _uiState.value = AnalyticsUiState.Error("No offline data available.")
            }
        } catch(e: Exception) {
            _uiState.value = AnalyticsUiState.Error("Failed to load offline data.")
        }
    }

    fun loadAnalytics() = viewModelScope.launch {
        _uiState.value = AnalyticsUiState.Loading
        when (val result = repository.getDashboard()) {
            is NetworkResult.Success -> _uiState.value = AnalyticsUiState.Loaded(result.data)
            is NetworkResult.Error -> {
                Timber.w("Analytics load failed: %s", result.message)
                _uiState.value = AnalyticsUiState.Error(result.message)
            }
            is NetworkResult.Loading -> Unit
        }

        // Fetch category breakdown from real API
        when (val catResult = repository.getCategoryBreakdown()) {
            is NetworkResult.Success -> _categoryBreakdown.value = catResult.data
            is NetworkResult.Error -> Timber.w("Category breakdown failed: %s", catResult.message)
            is NetworkResult.Loading -> Unit
        }
    }
}

