package com.retailiq.datasage.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.model.DailyTargetRequest
import com.retailiq.datasage.data.model.StaffPerformanceSummaryDto
import com.retailiq.datasage.data.model.StaffSessionDto
import com.retailiq.datasage.data.repository.StaffRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

sealed class StaffSessionState {
    object Loading : StaffSessionState()
    object NoSession : StaffSessionState()
    data class Active(val session: StaffSessionDto, val durationFormatted: String) : StaffSessionState()
    data class Ended(val session: StaffSessionDto) : StaffSessionState()
    data class Error(val message: String) : StaffSessionState()
}

sealed class StaffPerformanceState {
    object Loading : StaffPerformanceState()
    data class Success(val performanceList: List<StaffPerformanceSummaryDto>) : StaffPerformanceState()
    data class Error(val message: String) : StaffPerformanceState()
}

@HiltViewModel
class StaffViewModel @Inject constructor(
    private val repository: StaffRepository
) : ViewModel() {

    private val _sessionState = MutableStateFlow<StaffSessionState>(StaffSessionState.NoSession)
    val sessionState: StateFlow<StaffSessionState> = _sessionState.asStateFlow()

    private val _performanceState = MutableStateFlow<StaffPerformanceState>(StaffPerformanceState.Loading)
    val performanceState: StateFlow<StaffPerformanceState> = _performanceState.asStateFlow()

    fun startSession() {
        viewModelScope.launch {
            _sessionState.value = StaffSessionState.Loading
            repository.startSession()
                .onSuccess { session ->
                    _sessionState.value = StaffSessionState.Active(session, "00:00")
                }
                .onFailure { exception ->
                    _sessionState.value = StaffSessionState.Error(exception.message ?: "Failed to start session")
                }
        }
    }

    fun endSession() {
        viewModelScope.launch {
            _sessionState.value = StaffSessionState.Loading
            repository.endSession()
                .onSuccess { session ->
                    _sessionState.value = StaffSessionState.Ended(session)
                }
                .onFailure { exception ->
                    _sessionState.value = StaffSessionState.Error(exception.message ?: "Failed to end session")
                }
        }
    }

    fun resetSessionState() {
        _sessionState.value = StaffSessionState.NoSession
    }

    fun updateSessionDuration() {
        val currentState = _sessionState.value
        if (currentState is StaffSessionState.Active) {
            val startTimeStr = currentState.session.startTime
            try {
                val startTime = Instant.parse(startTimeStr)
                val now = Instant.now()
                val duration = Duration.between(startTime, now)
                
                val hours = duration.toHours()
                val minutes = duration.toMinutesPart()
                
                val formattedDuration = if (hours > 0) {
                    String.format("%02d:%02d", hours, minutes)
                } else {
                    String.format("%02d min", minutes)
                }
                
                _sessionState.value = currentState.copy(durationFormatted = formattedDuration)
            } catch (ex: java.lang.Exception) {
                // Formatting fallback or ignore
            }
        }
    }

    fun fetchPerformance(date: String) {
        viewModelScope.launch {
            _performanceState.value = StaffPerformanceState.Loading
            repository.getDailyPerformance(date)
                .onSuccess { data ->
                    _performanceState.value = StaffPerformanceState.Success(data)
                }
                .onFailure { exception ->
                    _performanceState.value = StaffPerformanceState.Error(exception.message ?: "Failed to fetch performance")
                }
        }
    }

    fun setTarget(request: DailyTargetRequest, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            repository.setDailyTarget(request)
                .onSuccess {
                    onComplete(true)
                    // Refresh data after setting target
                    fetchPerformance(request.targetDate)
                }
                .onFailure {
                    onComplete(false)
                }
        }
    }
}
