package com.retailiq.datasage.data.api

import retrofit2.http.GET

interface AnalyticsApiService {
    @GET("api/v1/dashboard")
    suspend fun dashboard(): ApiResponse<DashboardPayload>
}

data class DashboardPayload(
    val critical_count: Int = 0,
    val high_count: Int = 0,
    val total_revenue: Double = 0.0,
    val gross_profit: Double = 0.0,
    val transactions: Int = 0,
    val avg_basket: Double = 0.0,
    val trend: List<TrendPoint> = emptyList(),
    val insights: List<InsightItem> = emptyList(),
    val top_products: List<TopProduct> = emptyList(),
    val sync_pending: Int = 0,
    val sync_failed: Int = 0
)

data class TrendPoint(val day: String, val revenue: Float)
data class InsightItem(val type: String, val headline: String, val detail: String)
data class TopProduct(val name: String, val revenue: Float)
