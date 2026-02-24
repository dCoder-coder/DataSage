package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.AnalyticsApiService
import com.retailiq.datasage.data.api.DashboardPayload
import com.retailiq.datasage.data.api.NetworkResult
import javax.inject.Inject

class DashboardRepository @Inject constructor(private val api: AnalyticsApiService) {
    private var cache: DashboardPayload? = null

    suspend fun fetchDashboard(): NetworkResult<DashboardPayload> = try {
        val response = api.dashboard()
        val data = response.data ?: return NetworkResult.Error(500, response.error?.message ?: "No data")
        cache = data
        NetworkResult.Success(data)
    } catch (_: Exception) {
        cache?.let { NetworkResult.Success(it) } ?: NetworkResult.Error(503, "Unable to load dashboard")
    }
}
