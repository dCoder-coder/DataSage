package com.retailiq.datasage.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CustomerApiService {
    @GET("api/v1/customers")
    suspend fun listCustomers(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("search") search: String? = null
    ): ApiResponse<List<Customer>>

    @POST("api/v1/customers")
    suspend fun createCustomer(@Body request: CreateCustomerRequest): ApiResponse<Customer>

    @GET("api/v1/customers/{id}")
    suspend fun getCustomer(@Path("id") id: Int): ApiResponse<Customer>

    @PUT("api/v1/customers/{id}")
    suspend fun updateCustomer(
        @Path("id") id: Int,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): ApiResponse<Customer>

    @GET("api/v1/customers/{id}/transactions")
    suspend fun getCustomerTransactions(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): ApiResponse<List<TransactionSummary>>

    @GET("api/v1/customers/{id}/summary")
    suspend fun getCustomerSummary(@Path("id") id: Int): ApiResponse<Map<String, Any>>

    @GET("api/v1/customers/top")
    suspend fun getTopCustomers(
        @Query("metric") metric: String? = null,
        @Query("limit") limit: Int? = null
    ): ApiResponse<List<Map<String, Any>>>

    @GET("api/v1/customers/analytics")
    suspend fun getCustomerAnalytics(): ApiResponse<Map<String, Any>>
}

// ── Models ──

data class Customer(
    @SerializedName("customer_id") val customerId: Int,
    val name: String,
    @SerializedName("mobile_number") val mobileNumber: String? = null,
    val email: String? = null,
    val gender: String? = null,
    @SerializedName("birth_date") val birthDate: String? = null,
    val address: String? = null,
    val notes: String? = null,
    @SerializedName("total_spend") val totalSpend: Double? = null,
    @SerializedName("visit_count") val visitCount: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class CreateCustomerRequest(
    val name: String,
    @SerializedName("mobile_number") val mobileNumber: String,
    val email: String? = null,
    val gender: String? = null,
    @SerializedName("birth_date") val birthDate: String? = null,
    val address: String? = null,
    val notes: String? = null
)
