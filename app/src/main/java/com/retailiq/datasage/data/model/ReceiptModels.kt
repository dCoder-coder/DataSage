package com.retailiq.datasage.data.model

import com.google.gson.annotations.SerializedName

data class ReceiptTemplateDto(
    @SerializedName("header_text") val header: String? = "",
    @SerializedName("footer_text") val footer: String? = "",
    @SerializedName("show_gstin") val showGstin: Boolean = false,
    @SerializedName("paper_width_mm") val paperWidth: Int = 80
)

data class ReceiptTemplateRequest(
    @SerializedName("header_text") val header: String,
    @SerializedName("footer_text") val footer: String,
    @SerializedName("show_gstin") val showGstin: Boolean,
    @SerializedName("paper_width_mm") val paperWidth: Int
)

data class PrintJobRequest(
    @SerializedName("transaction_id") val transactionId: String,
    @SerializedName("printer_mac_address") val printerMacAddress: String
)

data class PrintJobResponse(
    @SerializedName("job_id") val jobId: String
)

data class PrintJobStatusDto(
    @SerializedName("job_id") val jobId: String,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String
)

data class BarcodeProductDto(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("product_name") val productName: String,
    @SerializedName("current_stock") val currentStock: Double,
    @SerializedName("price") val price: Double
)

data class BarcodeDto(
    @SerializedName("barcode_id") val barcodeId: Int,
    @SerializedName("value") val value: String,
    @SerializedName("product_id") val productId: Int,
    @SerializedName("created_at") val createdAt: String
)

data class RegisterBarcodeRequest(
    @SerializedName("value") val value: String,
    @SerializedName("product_id") val productId: Int
)
