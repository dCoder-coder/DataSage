package com.retailiq.datasage.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface InventoryApiService {
    @GET("api/v1/products")
    suspend fun listProducts(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 100,
        @Query("search") search: String? = null
    ): ApiResponse<List<Product>>

    @GET("api/v1/products/{id}")
    suspend fun getProduct(@Path("id") id: Int): ApiResponse<ProductDetail>
}

data class Product(
    @SerializedName("product_id") val productId: Int,
    val name: String,
    val sku: String? = null,
    @SerializedName("selling_price") val sellingPrice: Double = 0.0,
    @SerializedName("cost_price") val costPrice: Double = 0.0,
    @SerializedName("current_stock") val currentStock: Double = 0.0,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("is_active") val isActive: Boolean = true
)

data class ProductDetail(
    @SerializedName("product_id") val productId: Int,
    val name: String,
    val sku: String? = null,
    val barcode: String? = null,
    @SerializedName("selling_price") val sellingPrice: Double = 0.0,
    @SerializedName("cost_price") val costPrice: Double = 0.0,
    @SerializedName("current_stock") val currentStock: Double = 0.0,
    @SerializedName("low_stock_threshold") val lowStockThreshold: Int = 0,
    @SerializedName("category_id") val categoryId: Int? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("is_active") val isActive: Boolean = true
)
