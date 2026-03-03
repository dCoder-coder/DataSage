package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.CreateEventRequest
import com.retailiq.datasage.data.api.EventApiService
import com.retailiq.datasage.data.api.EventDto
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.toUserMessage
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

class EventRepository @Inject constructor(
    private val eventApi: EventApiService
) {

    suspend fun getEvents(): NetworkResult<List<EventDto>> = safeCall {
        val response = eventApi.getEvents()
        if (response.success && response.data != null) {
            NetworkResult.Success(response.data)
        } else {
            NetworkResult.Error(422, response.error.toUserMessage())
        }
    }

    suspend fun getUpcomingEvents(): NetworkResult<List<EventDto>> = safeCall {
        val response = eventApi.getUpcomingEvents()
        if (response.success && response.data != null) {
            NetworkResult.Success(response.data)
        } else {
            NetworkResult.Error(422, response.error.toUserMessage())
        }
    }

    suspend fun createEvent(request: CreateEventRequest): NetworkResult<EventDto> = safeCall {
        val response = eventApi.createEvent(request)
        if (response.success && response.data != null) {
            NetworkResult.Success(response.data)
        } else {
            NetworkResult.Error(422, response.error.toUserMessage())
        }
    }

    private suspend fun <T> safeCall(block: suspend () -> NetworkResult<T>): NetworkResult<T> = try {
        block()
    } catch (_: SocketTimeoutException) {
        NetworkResult.Error(408, "Request timed out. Please try again.")
    } catch (ex: Exception) {
        Timber.e(ex, "Event API error")
        NetworkResult.Error(500, ex.message ?: "Unexpected error")
    }
}
