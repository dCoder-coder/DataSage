package com.retailiq.datasage.data.api

import com.retailiq.datasage.data.model.BarcodeDto
import com.retailiq.datasage.data.model.BarcodeProductDto
import com.retailiq.datasage.data.model.PrintJobRequest
import com.retailiq.datasage.data.model.PrintJobResponse
import com.retailiq.datasage.data.model.PrintJobStatusDto
import com.retailiq.datasage.data.model.ReceiptTemplateDto
import com.retailiq.datasage.data.model.ReceiptTemplateRequest
import com.retailiq.datasage.data.model.RegisterBarcodeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ReceiptsApiService {

    @GET("receipts/template")
    suspend fun getTemplate(): Response<ReceiptTemplateDto>

    @PUT("receipts/template")
    suspend fun updateTemplate(@Body body: ReceiptTemplateRequest): Response<ReceiptTemplateDto>

    @POST("receipts/print")
    suspend fun createPrintJob(@Body body: PrintJobRequest): Response<PrintJobResponse>

    @GET("receipts/print/{jobId}")
    suspend fun pollPrintJob(@Path("jobId") jobId: String): Response<PrintJobStatusDto>

    @GET("barcodes/lookup")
    suspend fun lookupBarcode(@Query("value") value: String): Response<BarcodeProductDto>

    @POST("barcodes")
    suspend fun registerBarcode(@Body body: RegisterBarcodeRequest): Response<BarcodeDto>
}
