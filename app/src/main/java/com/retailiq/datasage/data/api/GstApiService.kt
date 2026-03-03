package com.retailiq.datasage.data.api

import com.retailiq.datasage.data.model.GstConfigDto
import com.retailiq.datasage.data.model.GstSlabDto
import com.retailiq.datasage.data.model.GstSummaryDto
import com.retailiq.datasage.data.model.HsnDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

interface GstApiService {

    @GET("api/v1/gst/config")
    suspend fun getConfig(): ApiResponse<GstConfigDto>

    @PUT("api/v1/gst/config")
    suspend fun updateConfig(@Body config: GstConfigDto): ApiResponse<GstConfigDto>

    @GET("api/v1/gst/hsn-search")
    suspend fun searchHsn(@Query("q") query: String): ApiResponse<List<HsnDto>>

    @GET("api/v1/gst/summary")
    suspend fun getSummary(@Query("period") period: String): ApiResponse<GstSummaryDto>

    @GET("api/v1/gst/liability-slabs")
    suspend fun getLiabilitySlabs(@Query("period") period: String): ApiResponse<List<GstSlabDto>>
    
    @GET("api/v1/gst/gstr1")
    suspend fun exportGstr1(@Query("period") period: String): Map<String, Any>
}
