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

    @POST("staff/sessions/start")
    suspend fun startSession(): Response<StaffSessionDto>

    @POST("staff/sessions/end")
    suspend fun endSession(): Response<StaffSessionDto>

    @GET("staff/performance")
    suspend fun getDailyPerformance(
        @Query("date") date: String
    ): Response<List<StaffPerformanceSummaryDto>>

    @POST("staff/targets")
    suspend fun setDailyTarget(
        @Body request: DailyTargetRequest
    ): Response<Unit>
}
