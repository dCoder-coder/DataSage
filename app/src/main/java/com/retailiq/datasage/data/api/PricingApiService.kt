package com.retailiq.datasage.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PricingApiService {

    @GET("api/v1/pricing/suggestions")
    suspend fun getSuggestions(): ApiResponse<List<PricingSuggestion>>

    @POST("api/v1/pricing/suggestions/{id}/apply")
    suspend fun applySuggestion(@Path("id") id: Int): ApiResponse<Map<String, Any>>

    @POST("api/v1/pricing/suggestions/{id}/dismiss")
    suspend fun dismissSuggestion(@Path("id") id: Int): ApiResponse<Map<String, Any>>
}

// ── DTOs ──────────────────────────────────────────────────────────────────────

data class PricingSuggestion(
    @SerializedName("id") val id: Int,
    @SerializedName("product_id") val productId: Int,
    @SerializedName("product_name") val productName: String,
    @SerializedName("current_price") val currentPrice: Double,
    @SerializedName("suggested_price") val suggestedPrice: Double,
    @SerializedName("reason") val reason: String,
    @SerializedName("margin_current") val marginCurrent: Double,
    @SerializedName("margin_suggested") val marginSuggested: Double,
    @SerializedName("confidence") val confidence: String, // HIGH | MEDIUM | LOW
    @SerializedName("status") val status: String = "PENDING"
)

data class PriceHistoryPoint(
    val date: String,
    val price: Double,
    val reason: String? = null
)
