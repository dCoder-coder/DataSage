package com.retailiq.datasage.data.model

import com.google.gson.annotations.SerializedName

data class GstConfigDto(
    @SerializedName("is_gst_enabled") val isGstEnabled: Boolean = false,
    @SerializedName("registration_type") val registrationType: String = "REGULAR",
    @SerializedName("state_code") val stateCode: String? = null,
    val gstin: String? = null
)

data class HsnDto(
    @SerializedName("hsn_code") val hsn_code: String = "",
    val description: String = "",
    @SerializedName("default_gst_rate") val default_rate: Double? = null
)

data class GstSummaryDto(
    val period: String = "",
    @SerializedName("total_taxable") val totalTaxable: Double = 0.0,
    @SerializedName("total_cgst") val totalCgst: Double = 0.0,
    @SerializedName("total_sgst") val totalSgst: Double = 0.0,
    @SerializedName("total_igst") val totalIgst: Double = 0.0,
    @SerializedName("invoice_count") val invoiceCount: Int = 0,
    val status: String? = null,
    @SerializedName("compiled_at") val compiledAt: String? = null
)

data class GstSlabDto(
    val rate: Double = 0.0,
    @SerializedName("taxable_value") val taxableValue: Double = 0.0,
    @SerializedName("tax_amount") val taxAmount: Double = 0.0
)
