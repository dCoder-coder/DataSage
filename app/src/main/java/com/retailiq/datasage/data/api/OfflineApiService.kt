package com.retailiq.datasage.data.api

import com.retailiq.datasage.data.model.SnapshotResponse
import retrofit2.http.GET

interface OfflineApiService {
    @GET("api/v1/offline/snapshot")
    suspend fun getSnapshot(): ApiResponse<SnapshotResponse>
}
