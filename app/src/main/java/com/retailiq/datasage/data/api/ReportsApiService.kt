package com.retailiq.datasage.data.api

import retrofit2.http.GET

interface ReportsApiService {
    @GET("api/v1/reports/export")
    suspend fun exportReport(): ApiResponse<Map<String, Any>>
}
