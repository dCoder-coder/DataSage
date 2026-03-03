package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.GstApiService
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.GstConfigDto
import com.retailiq.datasage.data.model.GstSummaryDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GstRepositoryTest {

    private lateinit var api: GstApiService
    private lateinit var repo: GstRepository

    @Before
    fun setup() {
        api = mock()
        repo = GstRepository(api)
    }

    @Test
    fun `getConfig returns Success when API succeeds`() = runTest {
        val config = GstConfigDto(
            isGstEnabled = true,
            registrationType = "REGULAR",
            stateCode = "27",
            gstin = "27AAAAA0000A1Z5"
        )
        whenever(api.getConfig()).thenReturn(
            ApiResponse(success = true, data = config, error = null, meta = null)
        )

        val result = repo.getConfig()

        assertTrue(result is NetworkResult.Success)
        assertEquals(config, (result as NetworkResult.Success).data)
    }

    @Test
    fun `getConfig returns Error when API throws`() = runTest {
        whenever(api.getConfig()).thenThrow(RuntimeException("Network Error"))

        val result = repo.getConfig()

        assertTrue(result is NetworkResult.Error)
        assertTrue((result as NetworkResult.Error).message.contains("Network Error"))
    }

    @Test
    fun `updateConfig returns Success when API succeeds`() = runTest {
        val config = GstConfigDto(
            isGstEnabled = true,
            registrationType = "REGULAR",
            stateCode = "27",
            gstin = "27AAAAA0000A1Z5"
        )
        whenever(api.updateConfig(config)).thenReturn(
            ApiResponse(success = true, data = config, error = null, meta = null)
        )

        val result = repo.updateConfig(config)

        assertTrue(result is NetworkResult.Success)
        assertEquals(config, (result as NetworkResult.Success).data)
    }

    @Test
    fun `getSummary returns Success with valid period`() = runTest {
        val period = "2026-02"
        val summary = GstSummaryDto(
            period = period,
            totalTaxable = 5000.0,
            totalCgst = 450.0,
            totalSgst = 450.0,
            totalIgst = 0.0,
            invoiceCount = 60,
            status = "PENDING",
            compiledAt = null
        )
        whenever(api.getSummary(period)).thenReturn(
            ApiResponse(success = true, data = summary, error = null, meta = null)
        )

        val result = repo.getSummary(period)

        assertTrue(result is NetworkResult.Success)
        assertEquals(summary, (result as NetworkResult.Success).data)
    }
}
