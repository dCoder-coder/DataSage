package com.retailiq.datasage.data.api

import retrofit2.http.GET

interface StoreApiService {
    @GET("api/v1/store/profile")
    suspend fun getStoreProfile(): ApiResponse<Map<String, Any>>
}
