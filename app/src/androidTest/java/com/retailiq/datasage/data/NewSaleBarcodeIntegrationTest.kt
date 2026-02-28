package com.retailiq.datasage.data

import com.retailiq.datasage.data.api.ReceiptsApiService
import com.retailiq.datasage.data.model.BarcodeDto
import com.retailiq.datasage.data.model.BarcodeProductDto
import com.retailiq.datasage.data.model.PrintJobRequest
import com.retailiq.datasage.data.model.PrintJobResponse
import com.retailiq.datasage.data.model.PrintJobStatusDto
import com.retailiq.datasage.data.model.ReceiptTemplateDto
import com.retailiq.datasage.data.model.ReceiptTemplateRequest
import com.retailiq.datasage.data.model.RegisterBarcodeRequest
import com.retailiq.datasage.data.repository.ReceiptsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/**
 * Data-layer integration test simulating a barcode scan and confirming that
 * the product is fetched and structured correctly.
 * Note: A full Compose UI test for scanning is skipped since it requires
 * physical camera hardware interactions.
 */
class NewSaleBarcodeIntegrationTest {

    @Test
    fun simulateScan_lookupBarcode_returnsExpectedProduct() = runBlocking {
        // Arrange
        val repo = ReceiptsRepository(FakeBarcodeApi())
        val scannedBarcode = "8901234567890"

        // Act
        val result = repo.lookupBarcode(scannedBarcode)

        // Assert
        assertTrue(result.isSuccess)
        val product = result.getOrNull()!!
        
        assertEquals(77, product.productId)
        assertEquals("Integration Test Product", product.productName)
        assertEquals(25.0, product.currentStock, 0.01)
        assertEquals(150.0, product.price, 0.01)
    }

    private class FakeBarcodeApi : ReceiptsApiService {
        override suspend fun getTemplate(): Response<ReceiptTemplateDto> = TODO()
        override suspend fun updateTemplate(body: ReceiptTemplateRequest): Response<ReceiptTemplateDto> = TODO()
        override suspend fun createPrintJob(body: PrintJobRequest): Response<PrintJobResponse> = TODO()
        override suspend fun pollPrintJob(jobId: String): Response<PrintJobStatusDto> = TODO()
        
        override suspend fun lookupBarcode(value: String): Response<BarcodeProductDto> {
            if (value == "8901234567890") {
                return Response.success(BarcodeProductDto(77, "Integration Test Product", 25.0, 150.0))
            }
            return Response.error(404, okhttp3.ResponseBody.Companion.toResponseBody(null))
        }

        override suspend fun registerBarcode(body: RegisterBarcodeRequest): Response<BarcodeDto> = TODO()
    }
}
