package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.AuditAdjustment
import com.retailiq.datasage.data.api.AuditItem
import com.retailiq.datasage.data.api.AuditRequest
import com.retailiq.datasage.data.api.AuditResponse
import com.retailiq.datasage.data.api.InventoryApiService
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.PriceHistoryEntry
import com.retailiq.datasage.data.api.Product
import com.retailiq.datasage.data.api.ProductDetail
import com.retailiq.datasage.data.api.StockUpdateRequest
import com.retailiq.datasage.data.api.StockUpdateResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException

class InventoryRepositoryTest {

    @Test
    fun getProducts_success() = runBlocking {
        val repo = InventoryRepository(FakeInventoryApi(success = true))
        val result = repo.getProducts()
        assertTrue(result is NetworkResult.Success)
        assertEquals(1, (result as NetworkResult.Success).data.size)
        assertEquals("Test Product", result.data[0].name)
    }

    @Test
    fun getProducts_networkError() = runBlocking {
        val repo = InventoryRepository(FakeInventoryApi(success = false))
        val result = repo.getProducts()
        assertTrue("Expected NetworkResult.Error, got $result", result is NetworkResult.Error)
        assertEquals(422, (result as NetworkResult.Error).code)
    }

    @Test
    fun updateStock_success() = runBlocking {
        val repo = InventoryRepository(FakeInventoryApi(success = true))
        val result = repo.updateStock(1, StockUpdateRequest(10.0, 100.0))
        assertTrue(result is NetworkResult.Success)
        assertEquals(1, (result as NetworkResult.Success).data.productId)
        assertEquals(15.0, result.data.newStock, 0.01)
    }

    @Test
    fun updateStock_timeout_returns408() = runBlocking {
        val repo = InventoryRepository(FakeInventoryApi(throwTimeout = true))
        val result = repo.updateStock(1, StockUpdateRequest(10.0, 100.0))
        assertTrue(result is NetworkResult.Error)
        assertEquals(408, (result as NetworkResult.Error).code)
    }

    @Test
    fun submitAudit_success() = runBlocking {
        val repo = InventoryRepository(FakeInventoryApi(success = true))
        val result = repo.submitAudit(AuditRequest(items = listOf(AuditItem(1, 20.0))))
        assertTrue(result is NetworkResult.Success)
        assertEquals(101, (result as NetworkResult.Success).data.auditId)
    }

    private inner class FakeInventoryApi(
        private val success: Boolean = true,
        private val throwTimeout: Boolean = false
    ) : InventoryApiService {
        
        override suspend fun listProducts(
            page: Int, pageSize: Int, search: String?, categoryId: Int?, 
            isActive: Boolean?, sortBy: String?, sortOrder: String?, 
            lowStock: Boolean?, slowMoving: Boolean?
        ): ApiResponse<List<Product>> {
            if (throwTimeout) throw SocketTimeoutException()
            return if (success) {
                ApiResponse(true, listOf(Product(1, "Test Product", costPrice = 10.0, sellingPrice = 20.0)), null, null)
            } else {
                ApiResponse(false, null, com.retailiq.datasage.data.api.ApiError("Failed to fetch"), null)
            }
        }

        override suspend fun getProduct(id: Int): ApiResponse<ProductDetail> = throw NotImplementedError()
        override suspend fun createProduct(request: com.retailiq.datasage.data.api.CreateProductRequest): ApiResponse<Product> = throw NotImplementedError()
        override suspend fun updateProduct(id: Int, request: Map<String, Any?>): ApiResponse<Product> = throw NotImplementedError()
        override suspend fun deleteProduct(id: Int): ApiResponse<Map<String, Any>> = throw NotImplementedError()
        override suspend fun getPriceHistory(id: Int): ApiResponse<List<PriceHistoryEntry>> = throw NotImplementedError()

        override suspend fun updateStock(
            id: Int,
            request: StockUpdateRequest
        ): ApiResponse<StockUpdateResponse> {
            if (throwTimeout) throw SocketTimeoutException()
            return if (success) {
                ApiResponse(true, StockUpdateResponse(id, 15.0, 99), null, null)
            } else {
                ApiResponse(false, null, com.retailiq.datasage.data.api.ApiError("Update stock failed"), null)
            }
        }

        override suspend fun submitAudit(request: AuditRequest): ApiResponse<AuditResponse> {
            if (throwTimeout) throw SocketTimeoutException()
            return if (success) {
                ApiResponse(true, AuditResponse(101, listOf(AuditAdjustment(1, 10.0, 20.0, 10.0))), null, null)
            } else {
                ApiResponse(false, null, com.retailiq.datasage.data.api.ApiError("Audit failed"), null)
            }
        }
    }
}
