package com.retailiq.datasage.data.model

import com.google.gson.annotations.SerializedName

data class StaffSessionDto(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("status") val status: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String? = null,
    @SerializedName("transactionsRecorded") val transactionsRecorded: Int = 0,
    @SerializedName("totalAmount") val totalAmount: Double = 0.0
)

data class StaffPerformanceSummaryDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("todayRevenue") val todayRevenue: Double,
    @SerializedName("todayTransactionCount") val todayTransactionCount: Int,
    @SerializedName("todayDiscountTotal") val todayDiscountTotal: Double,
    @SerializedName("avgDiscountPct") val avgDiscountPct: Double,
    @SerializedName("targetRevenue") val targetRevenue: Double?,
    @SerializedName("targetPctAchieved") val targetPctAchieved: Double?
)

data class DailyTargetRequest(
    @SerializedName("targetDate") val targetDate: String,
    @SerializedName("revenueTarget") val revenueTarget: Double,
    @SerializedName("countTarget") val countTarget: Int,
    @SerializedName("userId") val userId: String
)
