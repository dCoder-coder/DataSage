package com.retailiq.datasage.ui.viewmodel

import com.retailiq.datasage.data.api.CreateEventRequest
import com.retailiq.datasage.data.api.EventDto
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.repository.EventRepository
import com.retailiq.datasage.ui.events.EventCalendarViewModel
import com.retailiq.datasage.ui.events.EventCreateState
import com.retailiq.datasage.ui.events.EventListState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.mockito.Mockito.times

@OptIn(ExperimentalCoroutinesApi::class)
class EventCalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: EventRepository
    private lateinit var viewModel: EventCalendarViewModel

    private val mockEvent = EventDto(1, "Test Event", "PROMOTION", "2024-01-01", "2024-01-05", 15.0)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock(EventRepository::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadEvents sets Loaded state on success`() = runTest {
        whenever(repository.getEvents()).thenReturn(NetworkResult.Success(listOf(mockEvent)))
        whenever(repository.getUpcomingEvents()).thenReturn(NetworkResult.Success(emptyList()))

        viewModel = EventCalendarViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.monthEventsState.value
        assertTrue(state is EventListState.Loaded)
        assertEquals(1, (state as EventListState.Loaded).events.size)
    }

    @Test
    fun `createEvent success updates state and refreshes lists`() = runTest {
        whenever(repository.getEvents()).thenReturn(NetworkResult.Success(emptyList()))
        whenever(repository.getUpcomingEvents()).thenReturn(NetworkResult.Success(emptyList()))
        
        viewModel = EventCalendarViewModel(repository)
        advanceUntilIdle()

        val req = CreateEventRequest("New", "HOLIDAY", "2025-01-01", "2025-01-02", null)
        whenever(repository.createEvent(req)).thenReturn(NetworkResult.Success(mockEvent))

        viewModel.createEvent(req)
        advanceUntilIdle()

        val state = viewModel.createState.value
        assertTrue(state is EventCreateState.Success)
        assertEquals(mockEvent, (state as EventCreateState.Success).event)

        // Verify that loadEvents and loadUpcomingEvents were called again (init + create = 2 times each)
        verify(repository, times(2)).getEvents()
        verify(repository, times(2)).getUpcomingEvents()
    }

    @Test
    fun `createEvent failure sets Error state`() = runTest {
        whenever(repository.getEvents()).thenReturn(NetworkResult.Success(emptyList()))
        whenever(repository.getUpcomingEvents()).thenReturn(NetworkResult.Success(emptyList()))
        
        viewModel = EventCalendarViewModel(repository)
        advanceUntilIdle()

        val req = CreateEventRequest("New", "HOLIDAY", "2025-01-01", "2025-01-02", null)
        whenever(repository.createEvent(req)).thenReturn(NetworkResult.Error(422, "Invalid dates"))

        viewModel.createEvent(req)
        advanceUntilIdle()

        val state = viewModel.createState.value
        assertTrue(state is EventCreateState.Error)
        assertEquals("Invalid dates", (state as EventCreateState.Error).message)
    }
}
