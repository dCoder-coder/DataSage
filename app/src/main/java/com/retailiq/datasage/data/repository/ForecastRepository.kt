package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.ForecastApiService
import com.retailiq.datasage.data.api.ForecastPoint
import com.retailiq.datasage.data.api.DemandSensingResponse
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.toUserMessage
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

class ForecastRepository @Inject constructor(
    private val forecastApi: ForecastApiService
) {
    suspend fun getStoreForecast(horizon: Int? = null): NetworkResult<List<ForecastPoint>> = safeCall {
        val response = forecastApi.storeForecast(horizon)
        if (response.success && response.data != null) {
            NetworkResult.Success(response.data)
        } else {
            NetworkResult.Error(422, response.error.toUserMessage())
        }
    }

    suspend fun getDemandSensing(productId: Int, horizon: Int? = null): NetworkResult<DemandSensingResponse> = safeCall {
        val response = forecastApi.demandSensing(productId, horizon)
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
        Timber.e(ex, "Forecast API error")
        NetworkResult.Error(500, ex.message ?: "Unexpected error")
    }
}
