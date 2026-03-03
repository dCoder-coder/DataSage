package com.retailiq.datasage.data.model

import com.google.gson.annotations.SerializedName

data class LoyaltyAccountDto(
    @SerializedName("points_balance") val pointsBalance: Int,
    @SerializedName("lifetime_earned") val lifetimeEarned: Int,
    @SerializedName("redeemable_points") val redeemablePoints: Int,
    @SerializedName("value_in_currency") val valueInCurrency: Double,
    @SerializedName("tier") val tier: String? = null
)

data class LoyaltyTransactionDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String, // "EARN", "REDEEM", "EXPIRE"
    @SerializedName("points") val points: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("description") val description: String? = null
)

data class LoyaltyProgramSettingsDto(
    @SerializedName("points_per_rupee") val pointsPerRupee: Double,
    @SerializedName("redemption_rate") val redemptionRate: Double,
    @SerializedName("min_redemption_points") val minRedemptionPoints: Int,
    @SerializedName("expiry_days") val expiryDays: Int,
    @SerializedName("is_active") val isActive: Boolean
)

data class LoyaltyAnalyticsDto(
    @SerializedName("enrolled_customers") val enrolledCustomers: Int,
    @SerializedName("points_issued_this_month") val pointsIssuedThisMonth: Int,
    @SerializedName("redemption_rate_percent") val redemptionRatePercent: Double,
    @SerializedName("active_liability") val activeLiability: Double
)

data class RedeemPointsRequest(
    @SerializedName("transaction_id") val transactionId: String,
    @SerializedName("points") val points: Int
)
