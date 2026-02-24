package com.retailiq.datasage.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface AnalyticsApiService {
    @GET("api/v1/analytics/dashboard")
    suspend fun dashboard(): ApiResponse<DashboardPayload>

    @GET("api/v1/analytics/revenue")
    suspend fun revenue(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): ApiResponse<Map<String, Any>>

    @GET("api/v1/analytics/top-products")
    suspend fun topProducts(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): ApiResponse<List<Map<String, Any>>>
}

data class DashboardPayload(
    @SerializedName("critical_count") val criticalCount: Int = 0,
    @SerializedName("high_count") val highCount: Int = 0,
    @SerializedName("total_revenue") val totalRevenue: Double = 0.0,
    @SerializedName("gross_profit") val grossProfit: Double = 0.0,
    val transactions: Int = 0,
    @SerializedName("avg_basket") val avgBasket: Double = 0.0,
    val trend: List<TrendPoint> = emptyList(),
    val insights: List<InsightItem> = emptyList(),
    @SerializedName("top_products") val topProducts: List<TopProduct> = emptyList(),
    @SerializedName("sync_pending") val syncPending: Int = 0,
    @SerializedName("sync_failed") val syncFailed: Int = 0
)

data class TrendPoint(val day: String = "", val revenue: Float = 0f)
data class InsightItem(val type: String = "", val headline: String = "", val detail: String = "")
data class TopProduct(val name: String = "", val revenue: Float = 0f)
