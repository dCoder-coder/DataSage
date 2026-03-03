package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.ReceiptsApiService
import com.retailiq.datasage.data.model.BarcodeDto
import com.retailiq.datasage.data.model.BarcodeProductDto
import com.retailiq.datasage.data.model.PrintJobRequest
import com.retailiq.datasage.data.model.PrintJobResponse
import com.retailiq.datasage.data.model.PrintJobStatusDto
import com.retailiq.datasage.data.model.ReceiptTemplateDto
import com.retailiq.datasage.data.model.ReceiptTemplateRequest
import com.retailiq.datasage.data.model.RegisterBarcodeRequest
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class ReceiptsRepositoryTest {

    @Test
    fun lookupBarcode_found_returnsSuccess() = runBlocking {
        val repo = ReceiptsRepository(FakeReceiptsApi(networkError = false, notFound = false))

        val result = repo.lookupBarcode("123456")

        assertTrue(result.isSuccess)
        val product = result.getOrNull()
        assertEquals(1, product?.productId)
        assertEquals("Test Product", product?.productName)
        assertEquals(10.0, product?.currentStock)
        assertEquals(100.0, product?.price)
    }

    @Test
    fun lookupBarcode_notFound_returnsFailureWithMessage() = runBlocking {
        val repo = ReceiptsRepository(FakeReceiptsApi(networkError = false, notFound = true))

        val result = repo.lookupBarcode("999999")

        assertTrue(result.isFailure)
        assertEquals("Product not found for barcode: 999999", result.exceptionOrNull()?.message)
    }

    @Test
    fun lookupBarcode_networkError_returnsFailure() = runBlocking {
        val repo = ReceiptsRepository(FakeReceiptsApi(networkError = true, notFound = false))

        val result = repo.lookupBarcode("123456")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Network error") == true)
    }

    private class FakeReceiptsApi(val networkError: Boolean, val notFound: Boolean) : ReceiptsApiService {
        override suspend fun getTemplate(): Response<ApiResponse<ReceiptTemplateDto>> = TODO()
        override suspend fun updateTemplate(body: ReceiptTemplateRequest): Response<ApiResponse<ReceiptTemplateDto>> = TODO()
        override suspend fun createPrintJob(body: PrintJobRequest): Response<ApiResponse<PrintJobResponse>> = TODO()
        override suspend fun pollPrintJob(jobId: String): Response<ApiResponse<PrintJobStatusDto>> = TODO()
        
        override suspend fun lookupBarcode(value: String): Response<ApiResponse<BarcodeProductDto>> {
            if (networkError) throw IOException("Failed to connect")
            if (notFound) return Response.error(404, "".toResponseBody(null))

            val dto = BarcodeProductDto(1, "Test Product", 10.0, 100.0)
            return Response.success(ApiResponse(success = true, data = dto, error = null, meta = null))
        }

        override suspend fun registerBarcode(body: RegisterBarcodeRequest): Response<ApiResponse<BarcodeDto>> = TODO()
    }
}
