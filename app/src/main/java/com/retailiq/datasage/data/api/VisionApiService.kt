package com.retailiq.datasage.data.api

import com.google.gson.annotations.SerializedName

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

data class OcrItemDto(
    @SerializedName("item_id") val itemId: String,
    @SerializedName("raw_text") val rawText: String,
    @SerializedName("matched_product_id") val matchedProductId: Int?,
    @SerializedName("product_name") val matchedProductName: String?,
    @SerializedName("confidence") val confidence: Double?,
    @SerializedName("quantity") val qty: Double?,
    @SerializedName("unit_price") val unitPrice: Double?,
    @SerializedName("is_confirmed") val isConfirmed: Boolean
)

data class OcrJobResponse(
    @SerializedName("job_id") val jobId: String,
    @SerializedName("status") val status: String, // QUEUED, PROCESSING, REVIEW, APPLIED, FAILED
    @SerializedName("error_message") val errorMessage: String?,
    @SerializedName("items") val items: List<OcrItemDto>
)

data class OcrUploadResponse(
    @SerializedName("job_id") val jobId: String
)

data class ConfirmedItem(
    @SerializedName("item_id") val itemId: String,
    @SerializedName("matched_product_id") val matchedProductId: Int,
    @SerializedName("quantity") val qty: Double,
    @SerializedName("unit_price") val unitPrice: Double?
)

data class OcrConfirmRequest(
    @SerializedName("confirmed_items") val confirmedItems: List<ConfirmedItem>
)

interface VisionApiService {
    @Multipart
    @POST("api/v1/vision/ocr/upload")
    suspend fun uploadInvoice(
        @Part file: MultipartBody.Part
    ): ApiResponse<OcrUploadResponse>

    @GET("api/v1/vision/ocr/{jobId}")
    suspend fun getJobStatus(
        @Path("jobId") jobId: String
    ): ApiResponse<OcrJobResponse>

    @POST("api/v1/vision/ocr/{jobId}/confirm")
    suspend fun confirmJob(
        @Path("jobId") jobId: String,
        @Body request: OcrConfirmRequest
    ): ApiResponse<Any>

    @POST("api/v1/vision/ocr/{jobId}/dismiss")
    suspend fun dismissJob(
        @Path("jobId") jobId: String
    ): ApiResponse<Any>
}
