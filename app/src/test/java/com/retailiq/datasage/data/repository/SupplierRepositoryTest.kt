package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.ApiError
import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.SupplierApiService
import com.retailiq.datasage.data.model.supplier.CreatePoRequest
import com.retailiq.datasage.data.model.supplier.CreateSupplierRequest
import com.retailiq.datasage.data.model.supplier.GoodsReceiptRequest
import com.retailiq.datasage.data.model.supplier.PurchaseOrderDto
import com.retailiq.datasage.data.model.supplier.SupplierAnalyticsDto
import com.retailiq.datasage.data.model.supplier.SupplierContactDto
import com.retailiq.datasage.data.model.supplier.SupplierDto
import com.retailiq.datasage.data.model.supplier.SupplierProfileDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException

class SupplierRepositoryTest {

    @Test
    fun getSuppliers_returnsSuccess() = runBlocking {
        val repo = SupplierRepository(FakeSupplierApi())
        val result = repo.getSuppliers()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Test Supplier", result.getOrNull()?.get(0)?.name)
    }

    @Test
    fun getSuppliers_handlesApiError() = runBlocking {
        val repo = SupplierRepository(FakeSupplierApi(success = false))
        val result = repo.getSuppliers()
        assertTrue(result.isFailure)
        assertEquals("Invalid request", result.exceptionOrNull()?.message)
    }

    @Test
    fun createSupplier_returnsSuccessId() = runBlocking {
        val repo = SupplierRepository(FakeSupplierApi())
        val req = CreateSupplierRequest("New Corp", null, null, null, 15)
        val result = repo.createSupplier(req)
        assertTrue(result.isSuccess)
        assertEquals("2", result.getOrNull())
    }

    @Test
    fun receiveGoods_returnsSuccessId() = runBlocking {
        val repo = SupplierRepository(FakeSupplierApi())
        val result = repo.receiveGoods("1", GoodsReceiptRequest(emptyList()))
        assertTrue(result.isSuccess)
        assertEquals("1", result.getOrNull())
    }

    @Test
    fun sendPurchaseOrder_returnsSuccessId() = runBlocking {
        val repo = SupplierRepository(FakeSupplierApi())
        val result = repo.sendPurchaseOrder("1")
        assertTrue(result.isSuccess)
        assertEquals("1", result.getOrNull())
    }

    @Test
    fun exception_mapsToFailure() = runBlocking {
        val repo = SupplierRepository(object : SupplierApiService {
            override suspend fun getSuppliers() = throw RuntimeException("Crash")
            override suspend fun getSupplierProfile(id: String) = throw RuntimeException()
            override suspend fun createSupplier(request: CreateSupplierRequest) = throw RuntimeException()
            override suspend fun getPurchaseOrders(supplierId: String?, status: String?) = throw RuntimeException()
            override suspend fun getPurchaseOrder(id: String) = throw RuntimeException()
            override suspend fun createPurchaseOrder(request: CreatePoRequest) = throw RuntimeException()
            override suspend fun sendPurchaseOrder(id: String) = throw RuntimeException()
            override suspend fun receiveGoods(id: String, request: GoodsReceiptRequest) = throw RuntimeException()
        })
        
        val result = repo.getSuppliers()
        assertTrue(result.isFailure)
        assertEquals("Crash", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun timeout_mapsTo408ErrorText() = runBlocking {
        val repo = SupplierRepository(object : SupplierApiService {
            override suspend fun getSuppliers() = throw SocketTimeoutException()
            override suspend fun getSupplierProfile(id: String) = throw SocketTimeoutException()
            override suspend fun createSupplier(request: CreateSupplierRequest) = throw SocketTimeoutException()
            override suspend fun getPurchaseOrders(supplierId: String?, status: String?) = throw SocketTimeoutException()
            override suspend fun getPurchaseOrder(id: String) = throw SocketTimeoutException()
            override suspend fun createPurchaseOrder(request: CreatePoRequest) = throw SocketTimeoutException()
            override suspend fun sendPurchaseOrder(id: String) = throw SocketTimeoutException()
            override suspend fun receiveGoods(id: String, request: GoodsReceiptRequest) = throw SocketTimeoutException()
        })
        
        val result = repo.getSuppliers()
        assertTrue(result.isFailure)
        assertEquals("Request timed out. Please try again.", result.exceptionOrNull()?.message)
    }

    private class FakeSupplierApi(private val success: Boolean = true) : SupplierApiService {
        override suspend fun getSuppliers(): ApiResponse<List<SupplierDto>> = if (success) {
            ApiResponse(true, listOf(SupplierDto("1", "Test Supplier", null, null, null, 30, null, null, null)), null, null)
        } else {
            ApiResponse(false, null, ApiError("BAD_REQUEST", "Invalid request"), null)
        }

        override suspend fun getSupplierProfile(id: String): ApiResponse<SupplierProfileDto> = if (success) {
            ApiResponse(
                true,
                SupplierProfileDto(
                    id = id,
                    name = "Test Supplier",
                    contact = SupplierContactDto(name = "John", phone = "1234567890", email = "test@test.com"),
                    paymentTermsDays = 30,
                    analytics = SupplierAnalyticsDto(avgLeadTimeDays = 2.5, fillRate90d = 95.0)
                ),
                null, null
            )
        } else {
            ApiResponse(false, null, ApiError("NOT_FOUND", "Not found"), null)
        }

        override suspend fun createSupplier(request: CreateSupplierRequest): ApiResponse<Map<String, String>> = if (success) {
            ApiResponse(true, mapOf("id" to "2"), null, null)
        } else {
            ApiResponse(false, null, ApiError("VALIDATION_ERROR", "Invalid input"), null)
        }

        override suspend fun getPurchaseOrders(supplierId: String?, status: String?): ApiResponse<List<PurchaseOrderDto>> = if (success) {
            ApiResponse(true, listOf(PurchaseOrderDto("1", "1", "Supplier", "DRAFT")), null, null)
        } else {
            ApiResponse(false, null, ApiError("BAD_REQUEST", "Invalid"), null)
        }

        override suspend fun getPurchaseOrder(id: String): ApiResponse<PurchaseOrderDto> = if (success) {
            ApiResponse(true, PurchaseOrderDto(id, "1", "Supplier", "DRAFT"), null, null)
        } else {
            ApiResponse(false, null, ApiError("NOT_FOUND", "Not found"), null)
        }

        override suspend fun createPurchaseOrder(request: CreatePoRequest): ApiResponse<Map<String, String>> = if (success) {
            ApiResponse(true, mapOf("id" to "2"), null, null)
        } else {
            ApiResponse(false, null, ApiError("ERROR", "Error"), null)
        }

        override suspend fun sendPurchaseOrder(id: String): ApiResponse<Map<String, String>> = if (success) {
            ApiResponse(true, mapOf("id" to id), null, null)
        } else {
            ApiResponse(false, null, ApiError("ERROR", "Error"), null)
        }

        override suspend fun receiveGoods(id: String, request: GoodsReceiptRequest): ApiResponse<Map<String, String>> = if (success) {
            ApiResponse(true, mapOf("id" to id), null, null)
        } else {
            ApiResponse(false, null, ApiError("ERROR", "Error"), null)
        }
    }
}
