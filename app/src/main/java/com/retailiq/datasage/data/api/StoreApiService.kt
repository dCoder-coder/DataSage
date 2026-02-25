package com.retailiq.datasage.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path

interface StoreApiService {
    @GET("api/v1/store/profile")
    suspend fun getStoreProfile(): StoreApiResponse<StoreProfile>

    @PUT("api/v1/store/profile")
    suspend fun updateStoreProfile(@Body request: Map<String, @JvmSuppressWildcards Any?>): StoreApiResponse<StoreProfile>

    @GET("api/v1/store/categories")
    suspend fun getCategories(): StoreApiResponse<List<Category>>

    @POST("api/v1/store/categories")
    suspend fun createCategory(@Body request: CreateCategoryRequest): StoreApiResponse<Category>

    @PUT("api/v1/store/categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: Int,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): StoreApiResponse<Category>

    @DELETE("api/v1/store/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Int): StoreApiResponse<Map<String, Any>>

    @GET("api/v1/store/tax-config")
    suspend fun getTaxConfig(): StoreApiResponse<TaxConfigResponse>

    @PUT("api/v1/store/tax-config")
    suspend fun updateTaxConfig(@Body request: TaxConfigUpdateRequest): StoreApiResponse<Map<String, Any>>
}

// ── Models ──

data class StoreProfile(
    @SerializedName("store_id") val storeId: Int,
    @SerializedName("store_name") val storeName: String? = null,
    @SerializedName("store_type") val storeType: String? = null,
    val address: String? = null,
    val phone: String? = null,
    @SerializedName("gst_number") val gstNumber: String? = null,
    val currency: String? = null
)

data class Category(
    @SerializedName("category_id") val categoryId: Int,
    val name: String,
    @SerializedName("gst_rate") val gstRate: Double = 0.0
)

data class CreateCategoryRequest(
    val name: String,
    @SerializedName("gst_rate") val gstRate: Double? = null
)

data class TaxConfigResponse(
    val taxes: List<TaxEntry> = emptyList()
)

data class TaxEntry(
    @SerializedName("category_id") val categoryId: Int,
    val name: String,
    @SerializedName("gst_rate") val gstRate: Double
)

data class TaxConfigUpdateRequest(
    val taxes: List<TaxUpdate>
)

data class TaxUpdate(
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("gst_rate") val gstRate: Double
)
