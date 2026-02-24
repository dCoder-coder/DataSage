package com.retailiq.datasage.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.DashboardPayload
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository
) : ViewModel() {
    private val _state = MutableStateFlow<DashboardPayload?>(null)
    val state: StateFlow<DashboardPayload?> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            while (true) {
                delay(300_000)
                refresh()
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        when (val result = repository.fetchDashboard()) {
            is NetworkResult.Success -> _state.value = result.data
            is NetworkResult.Error -> _error.value = result.message
            is NetworkResult.Loading -> Unit
        }
    }
}
