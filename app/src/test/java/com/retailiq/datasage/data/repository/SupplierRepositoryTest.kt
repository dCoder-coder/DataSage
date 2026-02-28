package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.ApiError
import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.SupplierApiService
import com.retailiq.datasage.data.model.supplier.CreatePoRequest
import com.retailiq.datasage.data.model.supplier.CreateSupplierRequest
import com.retailiq.datasage.data.model.supplier.GoodsReceiptRequest
import com.retailiq.datasage.data.model.supplier.PurchaseOrderDto
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
    fun createSupplier_returnsSuccess() = runBlocking {
        val repo = SupplierRepository(FakeSupplierApi())
        val req = CreateSupplierRequest("New Corp", null, null, null, 15)
        val result = repo.createSupplier(req)
        assertTrue(result.isSuccess)
        assertEquals("New Corp", result.getOrNull()?.name)
    }

    @Test
    fun receiveGoods_returnsSuccess() = runBlocking {
        val repo = SupplierRepository(FakeSupplierApi())
        val result = repo.receiveGoods(1, GoodsReceiptRequest(emptyList()))
        assertTrue(result.isSuccess)
        assertEquals("FULFILLED", result.getOrNull()?.status)
    }

    @Test
    fun exception_mapsToFailure() = runBlocking {
        val repo = SupplierRepository(object : SupplierApiService {
            override suspend fun getSuppliers() = throw RuntimeException("Crash")
            override suspend fun getSupplierProfile(id: Int) = throw RuntimeException()
            override suspend fun createSupplier(request: CreateSupplierRequest) = throw RuntimeException()
            override suspend fun getPurchaseOrders(supplierId: Int?, status: String?) = throw RuntimeException()
            override suspend fun getPurchaseOrder(id: Int) = throw RuntimeException()
            override suspend fun createPurchaseOrder(request: CreatePoRequest) = throw RuntimeException()
            override suspend fun sendPurchaseOrder(id: Int) = throw RuntimeException()
            override suspend fun receiveGoods(id: Int, request: GoodsReceiptRequest) = throw RuntimeException()
        })
        
        val result = repo.getSuppliers()
        assertTrue(result.isFailure)
        assertEquals("Crash", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun timeout_mapsTo408ErrorText() = runBlocking {
        val repo = SupplierRepository(object : SupplierApiService {
            override suspend fun getSuppliers() = throw SocketTimeoutException()
            override suspend fun getSupplierProfile(id: Int) = throw SocketTimeoutException()
            override suspend fun createSupplier(request: CreateSupplierRequest) = throw SocketTimeoutException()
            override suspend fun getPurchaseOrders(supplierId: Int?, status: String?) = throw SocketTimeoutException()
            override suspend fun getPurchaseOrder(id: Int) = throw SocketTimeoutException()
            override suspend fun createPurchaseOrder(request: CreatePoRequest) = throw SocketTimeoutException()
            override suspend fun sendPurchaseOrder(id: Int) = throw SocketTimeoutException()
            override suspend fun receiveGoods(id: Int, request: GoodsReceiptRequest) = throw SocketTimeoutException()
        })
        
        val result = repo.getSuppliers()
        assertTrue(result.isFailure)
        assertEquals("Request timed out. Please try again.", result.exceptionOrNull()?.message)
    }

    private class FakeSupplierApi(private val success: Boolean = true) : SupplierApiService {
        override suspend fun getSuppliers() = if (success) {
            ApiResponse(true, listOf(SupplierDto(1, "Test Supplier", null, null, null, 30, 2.5, 0.95, null)), null, null)
        } else {
            ApiResponse(false, null, ApiError("BAD_REQUEST", "Invalid request"), null)
        }

        override suspend fun getSupplierProfile(id: Int) = if (success) {
            ApiResponse(true, SupplierProfileDto(id, "Test Supplier", null, null, null, 30, 2.5, 0.95, 0.05), null, null)
        } else {
            ApiResponse(false, null, ApiError("NOT_FOUND", "Not found"), null)
        }

        override suspend fun createSupplier(request: CreateSupplierRequest) = if (success) {
            ApiResponse(true, SupplierDto(2, request.name, request.contactName, request.phone, request.email, request.paymentTermsDays, null, null, null), null, null)
        } else {
            ApiResponse(false, null, ApiError("VALIDATION_ERROR", "Invalid input"), null)
        }

        override suspend fun getPurchaseOrders(supplierId: Int?, status: String?) = if (success) {
            ApiResponse(true, listOf(PurchaseOrderDto(1, 1, "Supplier", "DRAFT", null, 100.0, null, null, null)), null, null)
        } else {
            ApiResponse(false, null, ApiError("BAD_REQUEST", "Invalid"), null)
        }

        override suspend fun getPurchaseOrder(id: Int) = if (success) {
            ApiResponse(true, PurchaseOrderDto(id, 1, "Supplier", "DRAFT", null, 100.0, null, null, null), null, null)
        } else {
            ApiResponse(false, null, ApiError("NOT_FOUND", "Not found"), null)
        }

        override suspend fun createPurchaseOrder(request: CreatePoRequest) = if (success) {
            ApiResponse(true, PurchaseOrderDto(2, request.supplierId, "Sup", "DRAFT", null, 50.0, request.notes, null, null), null, null)
        } else {
            ApiResponse(false, null, ApiError("ERROR", "Error"), null)
        }

        override suspend fun sendPurchaseOrder(id: Int) = if (success) {
            ApiResponse(true, PurchaseOrderDto(id, 1, "Sup", "SENT", null, 100.0, null, null, null), null, null)
        } else {
            ApiResponse(false, null, ApiError("ERROR", "Error"), null)
        }

        override suspend fun receiveGoods(id: Int, request: GoodsReceiptRequest) = if (success) {
            ApiResponse(true, PurchaseOrderDto(id, 1, "Sup", "FULFILLED", null, 100.0, null, null, null), null, null)
        } else {
            ApiResponse(false, null, ApiError("ERROR", "Error"), null)
        }
    }
}
