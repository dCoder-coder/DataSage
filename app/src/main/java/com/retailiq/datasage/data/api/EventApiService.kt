package com.retailiq.datasage.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface EventApiService {

    @GET("api/v1/events")
    suspend fun getEvents(): ApiResponse<List<EventDto>>

    @GET("api/v1/events/upcoming")
    suspend fun getUpcomingEvents(): ApiResponse<List<EventDto>>

    @POST("api/v1/events")
    suspend fun createEvent(@Body request: CreateEventRequest): ApiResponse<EventDto>
}

// ── DTOs ──────────────────────────────────────────────────────────────────────

data class EventDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String, // HOLIDAY, FESTIVAL, PROMOTION, CLOSURE
    @SerializedName("start_date") val startDate: String, // YYYY-MM-DD
    @SerializedName("end_date") val endDate: String,     // YYYY-MM-DD
    @SerializedName("expected_impact_pct") val expectedImpactPct: Double? = null
)

data class CreateEventRequest(
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("expected_impact_pct") val expectedImpactPct: Double? = null
)

data class EventMarkerDto(
    @SerializedName("date") val date: String,     // YYYY-MM-DD
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String
)

data class DemandSensingResponse(
    @SerializedName("base_forecast") val baseForecast: List<ForecastPoint>,
    @SerializedName("adjusted_forecast") val adjustedForecast: List<ForecastPoint>,
    @SerializedName("events") val events: List<EventMarkerDto>
)
