package com.retailiq.datasage.data.model

import com.google.gson.annotations.SerializedName

data class CreditAccountDto(
    @SerializedName("current_balance") val currentBalance: Double,
    @SerializedName("credit_limit") val creditLimit: Double,
    @SerializedName("available_credit") val availableCredit: Double,
    @SerializedName("is_blocked") val isBlocked: Boolean,
    @SerializedName("last_repayment_date") val lastRepaymentDate: String? = null
)

data class CreditTransactionDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String, // "CREDIT", "REPAYMENT"
    @SerializedName("amount") val amount: Double,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("notes") val notes: String? = null
)

data class RepayCreditRequest(
    @SerializedName("amount") val amount: Double,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("payment_mode") val paymentMode: String = "cash"
)
