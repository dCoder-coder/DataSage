package com.retailiq.datasage.data.api

import com.retailiq.datasage.data.model.ChainDashboardDto
import com.retailiq.datasage.data.model.StoreCompareResponseDto
import com.retailiq.datasage.data.model.TransferSuggestionDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChainApiService {

    @GET("api/v1/chain/dashboard")
    suspend fun getDashboard(): ApiResponse<ChainDashboardDto>

    @GET("api/v1/chain/compare")
    suspend fun getComparison(@Query("period") period: String): ApiResponse<StoreCompareResponseDto>

    @GET("api/v1/chain/transfers")
    suspend fun getTransfers(): ApiResponse<List<TransferSuggestionDto>>

    @POST("api/v1/chain/transfers/{id}/confirm")
    suspend fun confirmTransfer(@Path("id") id: String): ApiResponse<Map<String, Any>>
}
