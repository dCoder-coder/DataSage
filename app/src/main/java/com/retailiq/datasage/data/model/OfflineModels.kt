package com.retailiq.datasage.data.model

import com.google.gson.annotations.SerializedName

data class SnapshotResponse(
    @SerializedName("built_at") val builtAt: String?,
    @SerializedName("size_bytes") val sizeBytes: Int?,
    @SerializedName("snapshot") val snapshot: SnapshotDto?
)

data class SnapshotDto(
    @SerializedName("kpis") val kpis: OfflineKpisDto?,
    @SerializedName("revenue_30d") val revenue30d: List<OfflineRevenueDto>? = emptyList(),
    @SerializedName("top_products_7d") val topProducts7d: List<OfflineTopProductDto>? = emptyList(),
    @SerializedName("alerts_open") val alertsOpen: List<OfflineAlertDto>? = emptyList(),
    @SerializedName("low_stock_products") val lowStockProducts: List<OfflineLowStockDto>? = emptyList(),
    @SerializedName("built_at") val builtAt: String?
)

data class OfflineKpisDto(
    @SerializedName("today_revenue") val todayRevenue: Double?,
    @SerializedName("today_profit") val todayProfit: Double?,
    @SerializedName("today_transactions") val todayTransactions: Int?,
    @SerializedName("yesterday_revenue") val yesterdayRevenue: Double?,
    @SerializedName("this_week_revenue") val thisWeekRevenue: Double?,
    @SerializedName("this_month_revenue") val thisMonthRevenue: Double?
)

data class OfflineRevenueDto(
    @SerializedName("date") val date: String?,
    @SerializedName("revenue") val revenue: Double?,
    @SerializedName("profit") val profit: Double?
)

data class OfflineTopProductDto(
    @SerializedName("product_id") val productId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("revenue") val revenue: Double?,
    @SerializedName("units_sold") val unitsSold: Int?
)

data class OfflineAlertDto(
    @SerializedName("id") val id: String?,
    @SerializedName("priority") val priority: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class OfflineLowStockDto(
    @SerializedName("product_id") val productId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("current_stock") val currentStock: Double?,
    @SerializedName("reorder_point") val reorderPoint: Double?
)
