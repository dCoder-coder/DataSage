package com.retailiq.datasage.data.model.supplier

import com.google.gson.annotations.SerializedName

data class SupplierDto(
    val id: Int,
    val name: String,
    @SerializedName("contact_name") val contactName: String?,
    val phone: String?,
    val email: String?,
    @SerializedName("payment_terms_days") val paymentTermsDays: Int,
    @SerializedName("avg_lead_time_days") val avgLeadTimeDays: Double?,
    @SerializedName("fill_rate") val fillRate: Double?, // 0.0 to 1.0 (or percentage)
    @SerializedName("created_at") val createdAt: String?
)

data class SupplierProfileDto(
    val id: Int,
    val name: String,
    @SerializedName("contact_name") val contactName: String?,
    val phone: String?,
    val email: String?,
    @SerializedName("payment_terms_days") val paymentTermsDays: Int,
    @SerializedName("avg_lead_time_days") val avgLeadTimeDays: Double?,
    @SerializedName("fill_rate") val fillRate: Double?,
    @SerializedName("price_change_6m") val priceChange6m: Double?,
    @SerializedName("sourced_products") val sourcedProducts: List<SupplierProductDto> = emptyList(),
    @SerializedName("recent_pos") val recentPos: List<PurchaseOrderDto> = emptyList()
)

data class SupplierProductDto(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("product_name") val productName: String,
    @SerializedName("supplier_sku") val supplierSku: String?,
    @SerializedName("quoted_price") val quotedPrice: Double,
    @SerializedName("lead_time_days") val leadTimeDays: Int,
    @SerializedName("last_updated") val lastUpdated: String?
)

data class PurchaseOrderDto(
    val id: Int,
    @SerializedName("supplier_id") val supplierId: Int,
    @SerializedName("supplier_name") val supplierName: String?,
    val status: String, // DRAFT, SENT, PARTIAL, FULFILLED, CANCELLED
    @SerializedName("expected_delivery") val expectedDelivery: String?,
    @SerializedName("total_amount") val totalAmount: Double,
    val notes: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    val items: List<PurchaseOrderItemDto> = emptyList()
)

data class PurchaseOrderItemDto(
    val id: Int,
    @SerializedName("product_id") val productId: Int,
    @SerializedName("product_name") val productName: String?, // Enriched by backend ideally
    @SerializedName("ordered_qty") val orderedQty: Int,
    @SerializedName("received_qty") val receivedQty: Int,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("total_price") val totalPrice: Double
)

// Requests

data class CreateSupplierRequest(
    val name: String,
    @SerializedName("contact_name") val contactName: String?,
    val phone: String?,
    val email: String?,
    @SerializedName("payment_terms_days") val paymentTermsDays: Int
)

data class CreatePoRequest(
    @SerializedName("supplier_id") val supplierId: Int,
    @SerializedName("expected_delivery") val expectedDelivery: String?,
    val notes: String?,
    val status: String = "DRAFT", // Or "SENT"
    val items: List<CreatePoItemRequest>
)

data class CreatePoItemRequest(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("ordered_qty") val orderedQty: Int,
    @SerializedName("unit_price") val unitPrice: Double
)

data class GoodsReceiptRequest(
    val items: List<GoodsReceiptItemRequest>
)

data class GoodsReceiptItemRequest(
    @SerializedName("po_item_id") val poItemId: Int,
    @SerializedName("received_qty") val receivedQty: Int,
    @SerializedName("unit_price") val unitPrice: Double
)
