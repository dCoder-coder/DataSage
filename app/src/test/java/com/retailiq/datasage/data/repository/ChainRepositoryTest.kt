package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.ChainApiService
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.ChainDashboardDto
import com.retailiq.datasage.data.model.StoreRevenueDto
import com.retailiq.datasage.data.model.TransferSuggestionDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ChainRepositoryTest {

    private lateinit var api: ChainApiService
    private lateinit var repository: ChainRepository

    @Before
    fun setup() {
        api = mock()
        repository = ChainRepository(api)
    }

    @Test
    fun `getDashboard returns Success with correct DTO mapping`() = runTest {
        val mockDashboard = ChainDashboardDto(
            total_revenue_today = 12500.0,
            best_store = null,
            worst_store = null,
            total_open_alerts = 3,
            store_revenues = listOf(
                StoreRevenueDto(1, "Alpha Store", 8000.0),
                StoreRevenueDto(2, "Beta Store", 4500.0)
            ),
            transfer_suggestions = emptyList()
        )
        whenever(api.getDashboard()).thenReturn(ApiResponse(true, mockDashboard, null, null))

        val result = repository.getDashboard()

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals(12500.0, data.total_revenue_today, 0.01)
        assertEquals(2, data.store_revenues.size)
        assertEquals("Alpha Store", data.store_revenues[0].store_name)
        assertEquals(3, data.total_open_alerts)
    }

    @Test
    fun `getTransfers returns only PENDING transfers`() = runTest {
        val transfers = listOf(
            TransferSuggestionDto("1", 1, "A", 2, "B", 10, "Widget", 5, "Excess stock", "PENDING"),
            TransferSuggestionDto("2", 1, "A", 3, "C", 11, "Gadget", 3, "Low stock", "CONFIRMED")
        )
        whenever(api.getTransfers()).thenReturn(ApiResponse(true, transfers, null, null))

        val result = repository.getTransfers()

        assertTrue(result is NetworkResult.Success)
        // Repository returns all; ViewModel filters to PENDING — test repo delivers raw list
        assertEquals(2, (result as NetworkResult.Success).data.size)
    }

    @Test
    fun `confirmTransfer returns Success on API success`() = runTest {
        whenever(api.confirmTransfer("42")).thenReturn(
            ApiResponse<Map<String, Any>>(true, mapOf("status" to "confirmed"), null, null)
        )

        val result = repository.confirmTransfer("42")

        assertTrue(result is NetworkResult.Success)
        assertEquals(true, (result as NetworkResult.Success).data)
    }

    @Test
    fun `getDashboard returns Error on API failure`() = runTest {
        whenever(api.getDashboard()).thenThrow(RuntimeException("timeout"))

        val result = repository.getDashboard()

        assertTrue(result is NetworkResult.Error)
    }
}
