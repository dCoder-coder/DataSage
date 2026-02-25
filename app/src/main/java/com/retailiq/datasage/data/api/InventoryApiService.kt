package com.retailiq.datasage.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface InventoryApiService {
    @GET("api/v1/inventory/products")
    suspend fun listProducts(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("search") search: String? = null,
        @Query("category_id") categoryId: Int? = null,
        @Query("is_active") isActive: Boolean? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("sort_order") sortOrder: String? = null,
        @Query("low_stock") lowStock: Boolean? = null,
        @Query("slow_moving") slowMoving: Boolean? = null
    ): ApiResponse<List<Product>>

    @GET("api/v1/inventory/products/{id}")
    suspend fun getProduct(@Path("id") id: Int): ApiResponse<ProductDetail>

    @POST("api/v1/inventory/products")
    suspend fun createProduct(@Body request: CreateProductRequest): ApiResponse<Product>

    @PUT("api/v1/inventory/products/{id}")
    suspend fun updateProduct(
        @Path("id") id: Int,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): ApiResponse<Product>

    @DELETE("api/v1/inventory/products/{id}")
    suspend fun deleteProduct(@Path("id") id: Int): ApiResponse<Map<String, Any>>

    @POST("api/v1/inventory/products/{id}/stock")
    suspend fun updateStock(
        @Path("id") id: Int,
        @Body request: StockUpdateRequest
    ): ApiResponse<StockUpdateResponse>

    @POST("api/v1/inventory/audit")
    suspend fun submitAudit(@Body request: AuditRequest): ApiResponse<AuditResponse>

    @GET("api/v1/inventory/products/{id}/price-history")
    suspend fun getPriceHistory(@Path("id") id: Int): ApiResponse<List<PriceHistoryEntry>>
}

// ── Product Models ──

data class Product(
    @SerializedName("product_id") val productId: Int,
    val name: String,
    @SerializedName("sku_code") val skuCode: String? = null,
    @SerializedName("category_id") val categoryId: Int? = null,
    val uom: String? = null,
    @SerializedName("cost_price") val costPrice: Double = 0.0,
    @SerializedName("selling_price") val sellingPrice: Double = 0.0,
    @SerializedName("current_stock") val currentStock: Double = 0.0,
    @SerializedName("reorder_level") val reorderLevel: Double = 0.0,
    @SerializedName("supplier_name") val supplierName: String? = null,
    val barcode: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("lead_time_days") val leadTimeDays: Int = 3,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("is_slow_moving") val isSlowMoving: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ProductDetail(
    @SerializedName("product_id") val productId: Int,
    val name: String,
    @SerializedName("sku_code") val skuCode: String? = null,
    val barcode: String? = null,
    @SerializedName("category_id") val categoryId: Int? = null,
    val uom: String? = null,
    @SerializedName("cost_price") val costPrice: Double = 0.0,
    @SerializedName("selling_price") val sellingPrice: Double = 0.0,
    @SerializedName("current_stock") val currentStock: Double = 0.0,
    @SerializedName("reorder_level") val reorderLevel: Double = 0.0,
    @SerializedName("supplier_name") val supplierName: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("lead_time_days") val leadTimeDays: Int = 3,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("is_slow_moving") val isSlowMoving: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null
)

// ── Request Models ──

data class CreateProductRequest(
    val name: String,
    @SerializedName("category_id") val categoryId: Int? = null,
    @SerializedName("sku_code") val skuCode: String? = null,
    val uom: String? = null,
    @SerializedName("cost_price") val costPrice: Double,
    @SerializedName("selling_price") val sellingPrice: Double,
    @SerializedName("current_stock") val currentStock: Double = 0.0,
    @SerializedName("reorder_level") val reorderLevel: Double = 0.0,
    @SerializedName("supplier_name") val supplierName: String? = null,
    val barcode: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("lead_time_days") val leadTimeDays: Int = 3
)

data class StockUpdateRequest(
    @SerializedName("quantity_added") val quantityAdded: Double,
    @SerializedName("purchase_price") val purchasePrice: Double,
    val date: String? = null,
    @SerializedName("supplier_name") val supplierName: String? = null,
    @SerializedName("update_cost_price") val updateCostPrice: Boolean? = null
)

data class AuditRequest(
    val items: List<AuditItem>,
    val notes: String? = null
)

data class AuditItem(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("actual_qty") val actualQty: Double
)

// ── Response Models ──

data class StockUpdateResponse(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("new_stock") val newStock: Double,
    @SerializedName("stock_entry_id") val stockEntryId: Int
)

data class AuditResponse(
    @SerializedName("audit_id") val auditId: Int,
    val adjustments: List<AuditAdjustment> = emptyList()
)

data class AuditAdjustment(
    @SerializedName("product_id") val productId: Int,
    val expected: Double,
    val actual: Double,
    val difference: Double
)

data class PriceHistoryEntry(
    @SerializedName("cost_price") val costPrice: Double,
    @SerializedName("selling_price") val sellingPrice: Double,
    @SerializedName("changed_at") val changedAt: String,
    @SerializedName("changed_by") val changedBy: Int
)
