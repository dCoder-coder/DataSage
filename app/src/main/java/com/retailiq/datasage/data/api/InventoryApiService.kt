package com.retailiq.datasage.data.api

import retrofit2.http.GET

interface InventoryApiService {
    @GET("api/v1/products")
    suspend fun listProducts(): ApiResponse<List<Map<String, Any>>>
}
