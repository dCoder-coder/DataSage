package com.retailiq.datasage.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ForecastApiService {
    @GET("api/v1/forecasting/store")
    suspend fun storeForecast(
        @Query("horizon") horizon: Int? = null
    ): ApiResponse<List<ForecastPoint>>

    @GET("api/v1/forecasting/sku/{productId}")
    suspend fun skuForecast(
        @Path("productId") productId: Int,
        @Query("horizon") horizon: Int? = null
    ): ApiResponse<List<ForecastPoint>>
}

data class ForecastPoint(
    val date: String,
    @SerializedName("forecast_mean") val forecastMean: Double,
    @SerializedName("lower_bound") val lowerBound: Double,
    @SerializedName("upper_bound") val upperBound: Double
)
