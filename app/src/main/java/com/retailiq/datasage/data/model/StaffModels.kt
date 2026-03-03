package com.retailiq.datasage.data.model

import com.google.gson.annotations.SerializedName

data class StaffSessionDto(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("status") val status: String,
    @SerializedName("started_at") val startTime: String,
    @SerializedName("ended_at") val endTime: String? = null,
    @SerializedName("active") val active: Boolean = false,
    @SerializedName("target_revenue") val targetRevenue: Double? = null
)

data class StaffPerformanceSummaryDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("today_revenue") val todayRevenue: Double,
    @SerializedName("today_transaction_count") val todayTransactionCount: Int,
    @SerializedName("today_discount_total") val todayDiscountTotal: Double,
    @SerializedName("avg_discount_pct") val avgDiscountPct: Double,
    @SerializedName("target_revenue") val targetRevenue: Double?,
    @SerializedName("target_pct_achieved") val targetPctAchieved: Double?
)

data class DailyTargetRequest(
    @SerializedName("target_date") val targetDate: String,
    @SerializedName("revenue_target") val revenueTarget: Double,
    @SerializedName("transaction_count_target") val countTarget: Int,
    @SerializedName("user_id") val userId: String
)
