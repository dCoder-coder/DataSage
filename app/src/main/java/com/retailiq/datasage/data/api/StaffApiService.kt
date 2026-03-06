package com.retailiq.datasage.data.api

import com.retailiq.datasage.data.model.DailyTargetRequest
import com.retailiq.datasage.data.model.StaffPerformanceSummaryDto
import com.retailiq.datasage.data.model.StaffSessionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface StaffApiService {

    @POST("api/v1/staff/sessions/start")
    suspend fun startSession(): Response<StaffSessionDto>

    @POST("api/v1/staff/sessions/end")
    suspend fun endSession(): Response<StaffSessionDto>

    @GET("api/v1/staff/performance")
    suspend fun getDailyPerformance(
        @Query("date") date: String
    ): ApiResponse<List<StaffPerformanceSummaryDto>>

    @POST("api/v1/staff/targets")
    suspend fun setDailyTarget(
        @Body request: DailyTargetRequest
    ): Response<Unit>
}
