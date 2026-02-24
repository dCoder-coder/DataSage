package com.retailiq.datasage.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.DashboardPayload
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed class AnalyticsUiState {
    data object Loading : AnalyticsUiState()
    data class Loaded(val data: DashboardPayload) : AnalyticsUiState()
    data class Error(val message: String) : AnalyticsUiState()
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: DashboardRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init { loadAnalytics() }

    fun loadAnalytics() = viewModelScope.launch {
        _uiState.value = AnalyticsUiState.Loading
        when (val result = repository.fetchDashboard()) {
            is NetworkResult.Success -> _uiState.value = AnalyticsUiState.Loaded(result.data)
            is NetworkResult.Error -> {
                Timber.w("Analytics load failed: %s", result.message)
                _uiState.value = AnalyticsUiState.Error(result.message)
            }
            is NetworkResult.Loading -> Unit
        }
    }
}
