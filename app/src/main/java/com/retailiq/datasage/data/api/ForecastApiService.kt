package com.retailiq.datasage.data.api

import retrofit2.http.GET

interface ForecastApiService {
    @GET("api/v1/forecast/store")
    suspend fun storeForecast(): ApiResponse<Map<String, Any>>
}
