package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.ReceiptsApiService
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
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("Failed to load template: HTTP ${response.code()}"))
        }
    }

    suspend fun updateTemplate(req: ReceiptTemplateRequest): Result<ReceiptTemplateDto> = safeCall {
        val response = api.updateTemplate(req)
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("Failed to save template: HTTP ${response.code()}"))
        }
    }

    suspend fun createPrintJob(req: PrintJobRequest): Result<PrintJobResponse> = safeCall {
        val response = api.createPrintJob(req)
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("Failed to create print job: HTTP ${response.code()}"))
        }
    }

    suspend fun pollPrintJob(jobId: String): Result<PrintJobStatusDto> = safeCall {
        val response = api.pollPrintJob(jobId)
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("Failed to poll print job: HTTP ${response.code()}"))
        }
    }

    suspend fun lookupBarcode(value: String): Result<BarcodeProductDto> = safeCall {
        val response = api.lookupBarcode(value)
        when {
            response.isSuccessful && response.body() != null -> Result.success(response.body()!!)
            response.code() == 404 -> Result.failure(Exception("Product not found for barcode: $value"))
            else -> Result.failure(Exception("Barcode lookup failed: HTTP ${response.code()}"))
        }
    }

    suspend fun registerBarcode(req: RegisterBarcodeRequest): Result<BarcodeDto> = safeCall {
        val response = api.registerBarcode(req)
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("Failed to register barcode: HTTP ${response.code()}"))
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
