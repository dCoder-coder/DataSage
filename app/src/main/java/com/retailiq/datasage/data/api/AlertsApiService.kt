package com.retailiq.datasage.data.api

import retrofit2.http.GET

interface AlertsApiService {
    @GET("api/v1/alerts")
    suspend fun listAlerts(): ApiResponse<List<Map<String, Any>>>
}
