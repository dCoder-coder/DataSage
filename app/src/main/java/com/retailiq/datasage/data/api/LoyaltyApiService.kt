package com.retailiq.datasage.data.api

import com.retailiq.datasage.data.model.LoyaltyAccountDto
import com.retailiq.datasage.data.model.LoyaltyAnalyticsDto
import com.retailiq.datasage.data.model.LoyaltyProgramSettingsDto
import com.retailiq.datasage.data.model.LoyaltyTransactionDto
import com.retailiq.datasage.data.model.RedeemPointsRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface LoyaltyApiService {

    @GET("api/v1/loyalty/customers/{customerId}/account")
    suspend fun getAccount(@Path("customerId") customerId: Int): ApiResponse<LoyaltyAccountDto>

    @GET("api/v1/loyalty/customers/{customerId}/transactions")
    suspend fun getTransactions(@Path("customerId") customerId: Int): ApiResponse<List<LoyaltyTransactionDto>>

    @POST("api/v1/loyalty/customers/{customerId}/redeem")
    suspend fun redeemPoints(
        @Path("customerId") customerId: Int,
        @Body request: RedeemPointsRequest
    ): ApiResponse<LoyaltyTransactionDto>

    @GET("api/v1/loyalty/program")
    suspend fun getSettings(): ApiResponse<LoyaltyProgramSettingsDto>

    @PUT("api/v1/loyalty/program")
    suspend fun updateSettings(@Body settings: LoyaltyProgramSettingsDto): ApiResponse<LoyaltyProgramSettingsDto>

    @GET("api/v1/loyalty/analytics")
    suspend fun getAnalytics(): ApiResponse<LoyaltyAnalyticsDto>
}
