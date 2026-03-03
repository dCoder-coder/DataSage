package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.ApiError
import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.InventoryApiService
import com.retailiq.datasage.data.api.PriceHistoryEntry
import com.retailiq.datasage.data.api.PricingApiService
import com.retailiq.datasage.data.api.PricingSuggestion
import com.retailiq.datasage.data.api.NetworkResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PricingRepositoryTest {

    // ─── getSuggestions ──────────────────────────────────────────────────────

    @Test
    fun getSuggestions_returnsList_onSuccess() = runBlocking {
        val repo = PricingRepository(FakePricingApi(), FakeInventoryApi())

        val result = repo.getSuggestions()

        assertTrue(result is NetworkResult.Success)
        val suggestions = (result as NetworkResult.Success).data
        assertEquals(2, suggestions.size)
        assertEquals(101, suggestions[0].id)
        assertEquals(102, suggestions[1].id)
    }

    @Test
    fun getSuggestions_returnsError_onApiFailure() = runBlocking {
        val repo = PricingRepository(FakePricingApi(suggestionsSuccess = false), FakeInventoryApi())

        val result = repo.getSuggestions()

        assertTrue(result is NetworkResult.Error)
        assertEquals(422, (result as NetworkResult.Error).code)
    }

    // ─── applySuggestion ─────────────────────────────────────────────────────

    @Test
    fun applySuggestion_returnsSuccess() = runBlocking {
        val repo = PricingRepository(FakePricingApi(), FakeInventoryApi())

        val result = repo.applySuggestion(101)

        assertTrue(result is NetworkResult.Success)
    }

    @Test
    fun applySuggestion_returnsError_onApiFailure() = runBlocking {
        val repo = PricingRepository(FakePricingApi(applySuccess = false), FakeInventoryApi())

        val result = repo.applySuggestion(101)

        assertTrue(result is NetworkResult.Error)
        assertEquals(422, (result as NetworkResult.Error).code)
    }

    // ─── dismissSuggestion ───────────────────────────────────────────────────

    @Test
    fun dismissSuggestion_returnsSuccess() = runBlocking {
        val repo = PricingRepository(FakePricingApi(), FakeInventoryApi())

        val result = repo.dismissSuggestion(101)

        assertTrue(result is NetworkResult.Success)
    }

    @Test
    fun dismissSuggestion_returnsError_onApiFailure() = runBlocking {
        val repo = PricingRepository(FakePricingApi(dismissSuccess = false), FakeInventoryApi())

        val result = repo.dismissSuggestion(102)

        assertTrue(result is NetworkResult.Error)
        assertEquals(422, (result as NetworkResult.Error).code)
    }

    // ─── getPriceHistory ─────────────────────────────────────────────────────

    @Test
    fun getPriceHistory_returnsList_onSuccess() = runBlocking {
        val repo = PricingRepository(FakePricingApi(), FakeInventoryApi())

        val result = repo.getPriceHistory(1)

        assertTrue(result is NetworkResult.Success)
        assertEquals(2, (result as NetworkResult.Success).data.size)
    }

    @Test
    fun getPriceHistory_returnsError_onFailure() = runBlocking {
        val repo = PricingRepository(FakePricingApi(), FakeInventoryApi(historySuccess = false))

        val result = repo.getPriceHistory(1)

        assertTrue(result is NetworkResult.Error)
    }

    // ─── Fakes ───────────────────────────────────────────────────────────────

    private val fakeSuggestions = listOf(
        PricingSuggestion(101, 1, "Rice 5kg", 120.0, 140.0, "Competitive drift detected", 12.0, 17.0, "HIGH"),
        PricingSuggestion(102, 2, "Sugar 1kg", 50.0, 55.0, "Low margin detected", 8.0, 12.0, "MEDIUM")
    )

    private inner class FakePricingApi(
        private val suggestionsSuccess: Boolean = true,
        private val applySuccess: Boolean = true,
        private val dismissSuccess: Boolean = true
    ) : PricingApiService {
        override suspend fun getSuggestions(): ApiResponse<List<PricingSuggestion>> =
            if (suggestionsSuccess)
                ApiResponse(true, fakeSuggestions, null, null)
            else
                ApiResponse(false, null, ApiError("SERVER_ERROR", "Failed to fetch"), null)

        override suspend fun applySuggestion(id: Int): ApiResponse<Map<String, Any>> =
            if (applySuccess)
                ApiResponse(true, mapOf("status" to "applied"), null, null)
            else
                ApiResponse(false, null, ApiError("SERVER_ERROR", "Apply failed"), null)

        override suspend fun dismissSuggestion(id: Int): ApiResponse<Map<String, Any>> =
            if (dismissSuccess)
                ApiResponse(true, mapOf("status" to "dismissed"), null, null)
            else
                ApiResponse(false, null, ApiError("SERVER_ERROR", "Dismiss failed"), null)
    }

    private val fakeHistory = listOf(
        PriceHistoryEntry(100.0, 120.0, "2025-01-01T00:00:00", 1),
        PriceHistoryEntry(120.0, 140.0, "2025-06-01T00:00:00", 1)
    )

    private inner class FakeInventoryApi(
        private val historySuccess: Boolean = true
    ) : InventoryApiService {
        override suspend fun listProducts(
            page: Int,
            pageSize: Int,
            search: String?,
            categoryId: Int?,
            isActive: Boolean?,
            sortBy: String?,
            sortOrder: String?,
            lowStock: Boolean?,
            slowMoving: Boolean?
        ) = ApiResponse(true, emptyList<com.retailiq.datasage.data.api.Product>(), null, null)

        override suspend fun getProduct(id: Int) =
            ApiResponse(true, com.retailiq.datasage.data.api.ProductDetail(
                id, "Test", null, null, null, null, 50.0, 80.0, 10.0, 5.0
            ), null, null)

        override suspend fun createProduct(request: com.retailiq.datasage.data.api.CreateProductRequest) =
            ApiResponse(true, com.retailiq.datasage.data.api.Product(1, "T"), null, null)

        override suspend fun updateProduct(id: Int, request: Map<String, Any?>) =
            ApiResponse<com.retailiq.datasage.data.api.Product>(false, null, com.retailiq.datasage.data.api.ApiError("Not implemented"), null)

        override suspend fun deleteProduct(id: Int) =
            ApiResponse<Map<String, Any>>(false, null, com.retailiq.datasage.data.api.ApiError("Not implemented"), null)

        override suspend fun updateStock(id: Int, request: com.retailiq.datasage.data.api.StockUpdateRequest) =
            ApiResponse<com.retailiq.datasage.data.api.StockUpdateResponse>(false, null, com.retailiq.datasage.data.api.ApiError("Not implemented"), null)

        override suspend fun submitAudit(request: com.retailiq.datasage.data.api.AuditRequest) =
            ApiResponse<com.retailiq.datasage.data.api.AuditResponse>(false, null, com.retailiq.datasage.data.api.ApiError("Not implemented"), null)

        override suspend fun getPriceHistory(id: Int): ApiResponse<List<PriceHistoryEntry>> =
            if (historySuccess)
                ApiResponse(true, fakeHistory, null, null)
            else
                ApiResponse(false, null, ApiError("SERVER_ERROR", "History failed"), null)
    }
}
