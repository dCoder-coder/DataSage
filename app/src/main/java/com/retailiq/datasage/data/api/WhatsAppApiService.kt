package com.retailiq.datasage.data.api

import com.retailiq.datasage.data.model.SendAlertRequest
import com.retailiq.datasage.data.model.SendPoRequest
import com.retailiq.datasage.data.model.WhatsAppConfigDto
import com.retailiq.datasage.data.model.WhatsAppLogResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface WhatsAppApiService {

    @GET("api/v1/whatsapp/config")
    suspend fun getConfig(): ApiResponse<WhatsAppConfigDto>

    @PUT("api/v1/whatsapp/config")
    suspend fun updateConfig(@Body config: WhatsAppConfigDto): ApiResponse<WhatsAppConfigDto>

    @POST("api/v1/whatsapp/send-alert")
    suspend fun sendAlert(@Body request: SendAlertRequest): ApiResponse<Map<String, Any>>

    @POST("api/v1/whatsapp/send-po")
    suspend fun sendPo(@Body request: SendPoRequest): ApiResponse<Map<String, Any>>

    @GET("api/v1/whatsapp/message-log")
    suspend fun getLogs(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50
    ): ApiResponse<WhatsAppLogResponse>
}
