package com.retailiq.datasage.data.api

import com.retailiq.datasage.data.model.CreditAccountDto
import com.retailiq.datasage.data.model.CreditTransactionDto
import com.retailiq.datasage.data.model.RepayCreditRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CreditApiService {

    @GET("api/v1/credit/customers/{customerId}/account")
    suspend fun getAccount(@Path("customerId") customerId: Int): ApiResponse<CreditAccountDto>

    @GET("api/v1/credit/customers/{customerId}/transactions")
    suspend fun getTransactions(@Path("customerId") customerId: Int): ApiResponse<List<CreditTransactionDto>>

    @POST("api/v1/credit/customers/{customerId}/repay")
    suspend fun repay(
        @Path("customerId") customerId: Int,
        @Body request: RepayCreditRequest
    ): ApiResponse<CreditTransactionDto>
}
