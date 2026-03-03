package com.retailiq.datasage.data.model.supplier

import com.google.gson.annotations.SerializedName

// ── Supplier List DTO ─────────────────────────────────────────────────────────
// Matches GET /api/v1/suppliers response per-item

data class SupplierDto(
    val id: String,
    val name: String,
    @SerializedName("contact_name") val contactName: String?,
    val phone: String?,
    val email: String?,
    @SerializedName("payment_terms_days") val paymentTermsDays: Int = 30,
    @SerializedName("avg_lead_time_days") val avgLeadTimeDays: Double?,
    @SerializedName("fill_rate_90d") val fillRate90d: Double?,           // backend key
    @SerializedName("price_change_6m_pct") val priceChange6mPct: Double?, // backend key
    @SerializedName("created_at") val createdAt: String? = null
)

// ── Supplier Profile DTO ──────────────────────────────────────────────────────
// Matches GET /api/v1/suppliers/{id} — backend nests contact & analytics

data class SupplierProfileDto(
    val id: String,
    val name: String,
    val contact: SupplierContactDto? = null,
    @SerializedName("payment_terms_days") val paymentTermsDays: Int = 30,
    @SerializedName("is_active") val isActive: Boolean = true,
    val analytics: SupplierAnalyticsDto? = null,
    @SerializedName("sourced_products") val sourcedProducts: List<SupplierProductDto> = emptyList(),
    @SerializedName("recent_purchase_orders") val recentPurchaseOrders: List<PurchaseOrderSummaryDto> = emptyList()
)

data class SupplierContactDto(
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null
)

data class SupplierAnalyticsDto(
    @SerializedName("avg_lead_time_days") val avgLeadTimeDays: Double? = null,
    @SerializedName("fill_rate_90d") val fillRate90d: Double? = null
)

// ── Sourced Product ───────────────────────────────────────────────────────────
// Backend returns "name" not "product_name", and no supplier_sku / last_updated

data class SupplierProductDto(
    @SerializedName("product_id") val productId: Int,
    val name: String,
    @SerializedName("quoted_price") val quotedPrice: Double = 0.0,
    @SerializedName("lead_time_days") val leadTimeDays: Int = 3
)

// ── PO Summary  (used inside profile "recent_purchase_orders") ────────────────

data class PurchaseOrderSummaryDto(
    val id: String,
    val status: String,
    @SerializedName("expected_delivery_date") val expectedDeliveryDate: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

// ── Full PO DTO (used by /purchase-orders list & detail) ──────────────────────

data class PurchaseOrderDto(
    val id: String,
    @SerializedName("supplier_id") val supplierId: String,
    @SerializedName("supplier_name") val supplierName: String? = null,
    val status: String,
    @SerializedName("expected_delivery_date") val expectedDeliveryDate: String? = null,
    @SerializedName("total_amount") val totalAmount: Double = 0.0,
    val notes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    val items: List<PurchaseOrderItemDto> = emptyList()
)

data class PurchaseOrderItemDto(
    val id: String? = null,
    @SerializedName("product_id") val productId: Int,
    @SerializedName("product_name") val productName: String? = null,
    @SerializedName("ordered_qty") val orderedQty: Double = 0.0,
    @SerializedName("received_qty") val receivedQty: Double = 0.0,
    @SerializedName("unit_price") val unitPrice: Double = 0.0,
    @SerializedName("total_price") val totalPrice: Double = 0.0
)

// ── Requests ──────────────────────────────────────────────────────────────────

data class CreateSupplierRequest(
    val name: String,
    @SerializedName("contact_name") val contactName: String?,
    val phone: String?,
    val email: String?,
    @SerializedName("payment_terms_days") val paymentTermsDays: Int
)

data class CreatePoRequest(
    @SerializedName("supplier_id") val supplierId: String,
    @SerializedName("expected_delivery_date") val expectedDeliveryDate: String?,
    val notes: String?,
    val status: String = "DRAFT",
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
    @SerializedName("product_id") val productId: Int,
    @SerializedName("received_qty") val receivedQty: Int,
    @SerializedName("unit_price") val unitPrice: Double
)
