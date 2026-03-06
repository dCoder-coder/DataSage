package com.retailiq.datasage.data.api

import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AlertsApiService {
    @GET("api/v1/inventory/alerts")
    suspend fun listAlerts(
        @Query("alert_type") alertType: String? = null,
        @Query("priority") priority: String? = null
    ): ApiResponse<List<Map<String, Any>>>

    @DELETE("api/v1/inventory/alerts/{alertId}")
    suspend fun dismissAlert(
        @Path("alertId") alertId: Int
    ): ApiResponse<Map<String, Any>>
}
