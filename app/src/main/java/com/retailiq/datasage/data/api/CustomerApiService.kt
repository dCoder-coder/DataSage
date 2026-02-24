package com.retailiq.datasage.data.api

import retrofit2.http.GET

interface CustomerApiService {
    @GET("api/v1/customers")
    suspend fun listCustomers(): ApiResponse<List<Map<String, Any>>>
}
