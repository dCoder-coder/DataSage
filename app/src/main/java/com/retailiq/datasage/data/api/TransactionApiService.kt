package com.retailiq.datasage.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface TransactionApiService {
    @POST("api/v1/transactions")
    suspend fun createTransaction(@Body request: Map<String, Any>): ApiResponse<Map<String, Any>>

    @POST("api/v1/transactions/batch")
    suspend fun createTransactionBatch(@Body request: BatchTransactionsRequest): Response<ApiResponse<SimpleMessage>>

    @GET("api/v1/transactions")
    suspend fun listTransactions(): ApiResponse<List<Map<String, Any>>>
}

data class BatchTransactionsRequest(val transactions: List<Map<String, Any>>)
