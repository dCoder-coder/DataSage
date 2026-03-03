package com.retailiq.datasage.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.CreateEventRequest
import com.retailiq.datasage.data.api.EventDto
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed class EventListState {
    data object Loading : EventListState()
    data class Loaded(val events: List<EventDto>) : EventListState()
    data class Error(val message: String) : EventListState()
}

sealed class EventCreateState {
    data object Idle : EventCreateState()
    data object InFlight : EventCreateState()
    data class Success(val event: EventDto) : EventCreateState()
    data class Error(val message: String) : EventCreateState()
}

@HiltViewModel
class EventCalendarViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    private val _monthEventsState = MutableStateFlow<EventListState>(EventListState.Loading)
    val monthEventsState: StateFlow<EventListState> = _monthEventsState.asStateFlow()

    private val _upcomingEventsState = MutableStateFlow<EventListState>(EventListState.Loading)
    val upcomingEventsState: StateFlow<EventListState> = _upcomingEventsState.asStateFlow()

    private val _createState = MutableStateFlow<EventCreateState>(EventCreateState.Idle)
    val createState: StateFlow<EventCreateState> = _createState.asStateFlow()

    init {
        loadEvents()
        loadUpcomingEvents()
    }

    fun loadEvents() = viewModelScope.launch {
        _monthEventsState.value = EventListState.Loading
        when (val result = repository.getEvents()) {
            is NetworkResult.Success -> _monthEventsState.value = EventListState.Loaded(result.data)
            is NetworkResult.Error -> _monthEventsState.value = EventListState.Error(result.message)
            is NetworkResult.Loading -> Unit
        }
    }

    fun loadUpcomingEvents() = viewModelScope.launch {
        _upcomingEventsState.value = EventListState.Loading
        when (val result = repository.getUpcomingEvents()) {
            is NetworkResult.Success -> _upcomingEventsState.value = EventListState.Loaded(result.data)
            is NetworkResult.Error -> _upcomingEventsState.value = EventListState.Error(result.message)
            is NetworkResult.Loading -> Unit
        }
    }

    fun createEvent(request: CreateEventRequest) = viewModelScope.launch {
        _createState.value = EventCreateState.InFlight
        when (val result = repository.createEvent(request)) {
            is NetworkResult.Success -> {
                _createState.value = EventCreateState.Success(result.data)
                // Refresh lists after creation
                loadEvents()
                loadUpcomingEvents()
            }
            is NetworkResult.Error -> {
                Timber.e("Create event failed: ${result.message}")
                _createState.value = EventCreateState.Error(result.message)
            }
            is NetworkResult.Loading -> Unit
        }
    }

    fun resetCreateState() {
        _createState.value = EventCreateState.Idle
    }
}
