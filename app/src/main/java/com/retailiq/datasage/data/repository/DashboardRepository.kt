package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.AnalyticsApiService
import com.retailiq.datasage.data.api.DashboardPayload
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.TransactionApiService
import com.retailiq.datasage.data.api.DailySummary
import com.retailiq.datasage.data.api.toUserMessage
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

class DashboardRepository @Inject constructor(
    private val analyticsApi: AnalyticsApiService,
    private val transactionApi: TransactionApiService
) {
    suspend fun getDashboard(): NetworkResult<DashboardPayload> = safeCall {
        val response = analyticsApi.dashboard()
        if (response.success && response.data != null) {
            NetworkResult.Success(response.data)
        } else {
            NetworkResult.Error(422, response.error.toUserMessage())
        }
    }

    suspend fun getDailySummary(date: String? = null): NetworkResult<DailySummary> = safeCall {
        val response = transactionApi.getDailySummary(date)
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
        Timber.e(ex, "Dashboard API error")
        NetworkResult.Error(500, ex.message ?: "Unexpected error")
    }
}
