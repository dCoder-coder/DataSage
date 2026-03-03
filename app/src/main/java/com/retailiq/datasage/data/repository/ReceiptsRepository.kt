package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.ReceiptsApiService
import com.retailiq.datasage.data.api.toUserMessage
import com.retailiq.datasage.data.model.BarcodeDto
import com.retailiq.datasage.data.model.BarcodeProductDto
import com.retailiq.datasage.data.model.PrintJobRequest
import com.retailiq.datasage.data.model.PrintJobResponse
import com.retailiq.datasage.data.model.PrintJobStatusDto
import com.retailiq.datasage.data.model.ReceiptTemplateDto
import com.retailiq.datasage.data.model.ReceiptTemplateRequest
import com.retailiq.datasage.data.model.RegisterBarcodeRequest
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class ReceiptsRepository @Inject constructor(
    private val api: ReceiptsApiService
) {

    suspend fun getTemplate(): Result<ReceiptTemplateDto> = safeCall {
        val response = api.getTemplate()
        val body = response.body()
        if (response.isSuccessful && body != null && body.success && body.data != null) {
            Result.success(body.data)
        } else {
            val msg = body?.error.toUserMessage()
            Result.failure(Exception("Failed to load template: $msg"))
        }
    }

    suspend fun updateTemplate(req: ReceiptTemplateRequest): Result<ReceiptTemplateDto> = safeCall {
        val response = api.updateTemplate(req)
        val body = response.body()
        if (response.isSuccessful && body != null && body.success && body.data != null) {
            Result.success(body.data)
        } else {
            val msg = body?.error.toUserMessage()
            Result.failure(Exception("Failed to save template: $msg"))
        }
    }

    suspend fun createPrintJob(req: PrintJobRequest): Result<PrintJobResponse> = safeCall {
        val response = api.createPrintJob(req)
        val body = response.body()
        if (response.isSuccessful && body != null && body.success && body.data != null) {
            Result.success(body.data)
        } else {
            val msg = body?.error.toUserMessage()
            Result.failure(Exception("Failed to create print job: $msg"))
        }
    }

    suspend fun pollPrintJob(jobId: String): Result<PrintJobStatusDto> = safeCall {
        val response = api.pollPrintJob(jobId)
        val body = response.body()
        if (response.isSuccessful && body != null && body.success && body.data != null) {
            Result.success(body.data)
        } else {
            val msg = body?.error.toUserMessage()
            Result.failure(Exception("Failed to poll print job: $msg"))
        }
    }

    suspend fun lookupBarcode(value: String): Result<BarcodeProductDto> = safeCall {
        val response = api.lookupBarcode(value)
        val body = response.body()
        when {
            response.isSuccessful && body != null && body.success && body.data != null -> Result.success(body.data)
            response.code() == 404 -> Result.failure(Exception("Product not found for barcode: $value"))
            else -> {
                val msg = body?.error.toUserMessage()
                Result.failure(Exception("Barcode lookup failed: $msg"))
            }
        }
    }

    suspend fun registerBarcode(req: RegisterBarcodeRequest): Result<BarcodeDto> = safeCall {
        val response = api.registerBarcode(req)
        val body = response.body()
        if (response.isSuccessful && body != null && body.success && body.data != null) {
            Result.success(body.data)
        } else {
            val msg = body?.error.toUserMessage()
            Result.failure(Exception("Failed to register barcode: $msg"))
        }
    }

    private suspend fun <T> safeCall(block: suspend () -> Result<T>): Result<T> {
        return try {
            block()
        } catch (e: IOException) {
            Timber.e(e, "Network error in ReceiptsRepository")
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error in ReceiptsRepository")
            Result.failure(e)
        }
    }
}
