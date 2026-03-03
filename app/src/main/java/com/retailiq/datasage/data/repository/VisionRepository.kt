package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.ConfirmedItem
import com.retailiq.datasage.data.api.OcrConfirmRequest
import com.retailiq.datasage.data.api.OcrJobResponse
import com.retailiq.datasage.data.api.VisionApiService
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.toUserMessage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class VisionRepository @Inject constructor(
    private val api: VisionApiService
) {
    suspend fun uploadInvoice(file: File): NetworkResult<String> = safeCall {
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        
        val response = api.uploadInvoice(body)
        if (response.success && response.data != null) {
            NetworkResult.Success(response.data.jobId)
        } else {
            NetworkResult.Error(400, response.error.toUserMessage())
        }
    }

    suspend fun getJobStatus(jobId: String): NetworkResult<OcrJobResponse> = safeCall {
        val response = api.getJobStatus(jobId)
        if (response.success && response.data != null) {
            NetworkResult.Success(response.data)
        } else {
            NetworkResult.Error(400, response.error.toUserMessage())
        }
    }

    suspend fun confirmJob(jobId: String, items: List<ConfirmedItem>): NetworkResult<Unit> = safeCall {
        val request = OcrConfirmRequest(items)
        val response = api.confirmJob(jobId, request)
        if (response.success) {
            NetworkResult.Success(Unit)
        } else {
            NetworkResult.Error(400, response.error.toUserMessage())
        }
    }

    suspend fun dismissJob(jobId: String): NetworkResult<Unit> = safeCall {
        val response = api.dismissJob(jobId)
        if (response.success) {
            NetworkResult.Success(Unit)
        } else {
            NetworkResult.Error(400, response.error.toUserMessage())
        }
    }

    private suspend fun <T> safeCall(block: suspend () -> NetworkResult<T>): NetworkResult<T> = try {
        block()
    } catch (e: Exception) {
        NetworkResult.Error(0, e.message ?: "Unknown error")
    }
}

