package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.AnalyticsApiService
import com.retailiq.datasage.data.api.DailySummary
import com.retailiq.datasage.data.api.DashboardPayload
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.TransactionApiService
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

class DashboardRepository @Inject constructor(
    private val analyticsApi: AnalyticsApiService,
    private val transactionApi: TransactionApiService
) {
    private var dashboardCache: DashboardPayload? = null
    private var summaryCache: DailySummary? = null

    suspend fun fetchDashboard(): NetworkResult<DashboardPayload> = safeCall {
        val response = analyticsApi.dashboard()
        val data = response.data ?: return@safeCall NetworkResult.Error(
            500, response.error?.message ?: "No data"
        )
        dashboardCache = data
        NetworkResult.Success(data)
    }

    suspend fun fetchDailySummary(date: String? = null): NetworkResult<DailySummary> = safeCall {
        val response = transactionApi.getDailySummary(date)
        if (response.success && response.data != null) {
            summaryCache = response.data
            NetworkResult.Success(response.data)
        } else {
            NetworkResult.Error(422, response.error?.message ?: "No data")
        }
    }

    fun getCachedDashboard(): DashboardPayload? = dashboardCache
    fun getCachedSummary(): DailySummary? = summaryCache

    private suspend fun <T> safeCall(block: suspend () -> NetworkResult<T>): NetworkResult<T> = try {
        block()
    } catch (_: SocketTimeoutException) {
        NetworkResult.Error(408, "Request timed out")
    } catch (e: Exception) {
        Timber.e(e, "Dashboard API error")
        // Return cached data if available
        NetworkResult.Error(503, e.message ?: "Unable to load dashboard")
    }
}
