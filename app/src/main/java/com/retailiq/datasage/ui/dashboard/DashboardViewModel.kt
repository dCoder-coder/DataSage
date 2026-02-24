package com.retailiq.datasage.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.DailySummary
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.local.PendingTransactionDao
import com.retailiq.datasage.data.repository.DashboardRepository
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
    data class Loaded(val summary: DailySummary) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    pendingDao: PendingTransactionDao
) : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val pendingCount: StateFlow<Int> = pendingDao.countByStatusFlow("pending")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val failedCount: StateFlow<Int> = pendingDao.countByStatusFlow("failed")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _uiState.value = DashboardUiState.Loading
        when (val result = repository.fetchDailySummary()) {
            is NetworkResult.Success -> {
                _uiState.value = DashboardUiState.Loaded(result.data)
            }
            is NetworkResult.Error -> {
                Timber.w("Dashboard load failed: %s", result.message)
                _uiState.value = DashboardUiState.Error(result.message)
            }
            is NetworkResult.Loading -> Unit
        }
    }
}
