package com.retailiq.datasage.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TransactionApiService {
    @POST("api/v1/transactions")
    suspend fun createTransaction(@Body request: CreateTransactionRequest): ApiResponse<CreateTransactionResponse>

    @POST("api/v1/transactions/batch")
    suspend fun createTransactionBatch(@Body request: BatchTransactionsRequest): Response<ApiResponse<BatchTransactionResponse>>

    @GET("api/v1/transactions")
    suspend fun listTransactions(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("payment_mode") paymentMode: String? = null
    ): ApiResponse<List<TransactionSummary>>

    @GET("api/v1/transactions/{id}")
    suspend fun getTransaction(@Path("id") id: String): ApiResponse<TransactionDetail>

    @GET("api/v1/transactions/summary/daily")
    suspend fun getDailySummary(
        @Query("date") date: String? = null
    ): ApiResponse<DailySummary>
}

// ── Request Models ──

data class CreateTransactionRequest(
    val items: List<TransactionItemRequest>,
    @SerializedName("payment_mode") val paymentMode: String,
    @SerializedName("customer_id") val customerId: Int? = null,
    val notes: String? = null
)

data class TransactionItemRequest(
    @SerializedName("product_id") val productId: Int,
    val quantity: Double,
    @SerializedName("selling_price") val sellingPrice: Double,
    @SerializedName("discount_amount") val discountAmount: Double = 0.0
)

data class BatchTransactionsRequest(val transactions: List<Map<String, Any>>)

// ── Response Models ──

data class CreateTransactionResponse(
    @SerializedName("transaction_id") val transactionId: String
)

data class BatchTransactionResponse(
    val message: String? = null,
    val created: Int = 0,
    val failed: Int = 0
)

data class TransactionSummary(
    @SerializedName("transaction_id") val transactionId: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("payment_mode") val paymentMode: String,
    @SerializedName("customer_id") val customerId: Int?,
    @SerializedName("is_return") val isReturn: Boolean = false
)

data class TransactionDetail(
    @SerializedName("transaction_id") val transactionId: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("payment_mode") val paymentMode: String,
    @SerializedName("customer_id") val customerId: Int?,
    val notes: String?,
    @SerializedName("is_return") val isReturn: Boolean = false,
    @SerializedName("original_transaction_id") val originalTransactionId: String? = null,
    @SerializedName("line_items") val lineItems: List<TransactionLineItem> = emptyList()
)

data class TransactionLineItem(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("product_name") val productName: String?,
    val quantity: Double,
    @SerializedName("selling_price") val sellingPrice: Double,
    @SerializedName("discount_amount") val discountAmount: Double = 0.0
)

data class DailySummary(
    @SerializedName("revenue_by_payment_mode") val revenueByPaymentMode: Map<String, Double> = emptyMap(),
    @SerializedName("top_5_products") val topProducts: List<TopProductSummary> = emptyList(),
    @SerializedName("transaction_count") val transactionCount: Int = 0,
    @SerializedName("avg_basket") val avgBasket: Double = 0.0,
    @SerializedName("gross_profit") val grossProfit: Double = 0.0,
    @SerializedName("returns_count") val returnsCount: Int = 0
)

data class TopProductSummary(
    @SerializedName("product_id") val productId: Int,
    val name: String,
    @SerializedName("quantity_sold") val quantitySold: Double
)
