package com.retailiq.datasage.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface AnalyticsApiService {
    @GET("api/v1/analytics/dashboard")
    suspend fun dashboard(): ApiResponse<DashboardPayload>

    @GET("api/v1/analytics/revenue")
    suspend fun revenue(
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("group_by") groupBy: String? = null
    ): ApiResponse<List<Map<String, Any>>>

    @GET("api/v1/analytics/profit")
    suspend fun profit(
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("group_by") groupBy: String? = null
    ): ApiResponse<List<Map<String, Any>>>

    @GET("api/v1/analytics/top-products")
    suspend fun topProducts(
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("metric") metric: String? = null,
        @Query("limit") limit: Int? = null
    ): ApiResponse<List<Map<String, Any>>>

    @GET("api/v1/analytics/category-breakdown")
    suspend fun categoryBreakdown(
        @Query("start") start: String? = null,
        @Query("end") end: String? = null
    ): ApiResponse<List<Map<String, Any>>>

    @GET("api/v1/analytics/contribution")
    suspend fun contribution(
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("compare_start") compareStart: String? = null,
        @Query("compare_end") compareEnd: String? = null
    ): ApiResponse<Map<String, Any>>

    @GET("api/v1/analytics/payment-modes")
    suspend fun paymentModes(
        @Query("start") start: String? = null,
        @Query("end") end: String? = null
    ): ApiResponse<List<Map<String, Any>>>

    @GET("api/v1/analytics/customers/summary")
    suspend fun customersSummary(
        @Query("start") start: String? = null,
        @Query("end") end: String? = null
    ): ApiResponse<Map<String, Any>>

    @GET("api/v1/analytics/diagnostics")
    suspend fun diagnostics(
        @Query("start") start: String? = null,
        @Query("end") end: String? = null
    ): ApiResponse<Map<String, Any>>
}

// ── Dashboard Models ──
// These match the ACTUAL backend routes.py implementation, not the API guide.

data class DashboardPayload(
    @SerializedName("today_kpis") val todayKpis: TodayKpis = TodayKpis(),
    @SerializedName("revenue_7d") val revenue7d: List<Revenue7dPoint> = emptyList(),
    @SerializedName("moving_avg_7d") val movingAvg7d: List<MovingAvgPoint> = emptyList(),
    @SerializedName("alerts_summary") val alertsSummary: Map<String, Int> = emptyMap(),
    @SerializedName("top_products_today") val topProductsToday: List<TopProduct> = emptyList(),
    val insights: List<InsightItem> = emptyList()
)

data class TodayKpis(
    val date: String = "",
    val revenue: Double = 0.0,
    val profit: Double = 0.0,
    val transactions: Int = 0,
    @SerializedName("avg_basket") val avgBasket: Double = 0.0,
    @SerializedName("units_sold") val unitsSold: Double = 0.0
)

data class Revenue7dPoint(
    val date: String = "",
    val revenue: Double = 0.0,
    val profit: Double = 0.0,
    val transactions: Int = 0,
    @SerializedName("avg_basket") val avgBasket: Double = 0.0,
    @SerializedName("units_sold") val unitsSold: Double = 0.0,
    @SerializedName("moving_avg_7d") val movingAvg7d: Double = 0.0
)

data class MovingAvgPoint(
    val date: String = "",
    @SerializedName("moving_avg") val movingAvg: Double = 0.0
)

data class InsightItem(
    val type: String = "",
    val title: String = "",
    val body: String = ""
)

data class TopProduct(
    @SerializedName("product_id") val productId: Int = 0,
    val name: String = "",
    val revenue: Double = 0.0,
    @SerializedName("units_sold") val unitsSold: Double = 0.0
)
