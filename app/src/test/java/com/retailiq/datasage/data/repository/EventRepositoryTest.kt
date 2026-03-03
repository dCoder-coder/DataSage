package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.CreateEventRequest
import com.retailiq.datasage.data.api.EventApiService
import com.retailiq.datasage.data.api.EventDto
import com.retailiq.datasage.data.api.NetworkResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeEventApiService : EventApiService {
    var shouldReturnError = false
    val mockEvent = EventDto(1, "Summer Sale", "PROMOTION", "2024-06-01", "2024-06-15", 25.0)

    override suspend fun getEvents(): ApiResponse<List<EventDto>> {
        if (shouldReturnError) return ApiResponse(false, null, com.retailiq.datasage.data.api.ApiError("error", "Bad request"), null)
        return ApiResponse(true, listOf(mockEvent), null, null)
    }

    override suspend fun getUpcomingEvents(): ApiResponse<List<EventDto>> {
        if (shouldReturnError) return ApiResponse(false, null, com.retailiq.datasage.data.api.ApiError("error", "Bad request"), null)
        return ApiResponse(true, listOf(mockEvent), null, null)
    }

    override suspend fun createEvent(request: CreateEventRequest): ApiResponse<EventDto> {
        if (shouldReturnError) return ApiResponse(false, null, com.retailiq.datasage.data.api.ApiError("error", "Failed to create"), null)
        return ApiResponse(true, mockEvent.copy(name = request.name), null, null)
    }
}

class EventRepositoryTest {

    private lateinit var repository: EventRepository
    private lateinit var fakeApi: FakeEventApiService

    @Before
    fun setup() {
        fakeApi = FakeEventApiService()
        repository = EventRepository(fakeApi)
    }

    @Test
    fun getEvents_returnsList_onSuccess() = runTest {
        val result = repository.getEvents()
        assertTrue(result is NetworkResult.Success)
        assertEquals(1, (result as NetworkResult.Success).data.size)
        assertEquals("Summer Sale", result.data[0].name)
    }

    @Test
    fun getEvents_returnsError_onFailure() = runTest {
        fakeApi.shouldReturnError = true
        val result = repository.getEvents()
        assertTrue(result is NetworkResult.Error)
    }

    @Test
    fun getUpcomingEvents_returnsSuccess() = runTest {
        val result = repository.getUpcomingEvents()
        assertTrue(result is NetworkResult.Success)
    }

    @Test
    fun createEvent_returnsSuccess() = runTest {
        val req = CreateEventRequest("New Event", "FESTIVAL", "2025-01-01", "2025-01-02", 10.0)
        val result = repository.createEvent(req)
        assertTrue(result is NetworkResult.Success)
        assertEquals("New Event", (result as NetworkResult.Success).data.name)
    }
}
