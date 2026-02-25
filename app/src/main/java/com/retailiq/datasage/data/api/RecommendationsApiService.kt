package com.retailiq.datasage.data.api

import retrofit2.http.GET

interface RecommendationsApiService {
    @GET("api/v1/recommendations/")
    suspend fun getRecommendations(): StoreApiResponse<List<Map<String, Any>>>
}
