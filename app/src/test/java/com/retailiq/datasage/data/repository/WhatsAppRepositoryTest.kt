package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.WhatsAppApiService
import com.retailiq.datasage.data.model.WhatsAppConfigDto
import com.retailiq.datasage.data.model.WhatsAppLogResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WhatsAppRepositoryTest {

    private lateinit var apiService: WhatsAppApiService
    private lateinit var repository: WhatsAppRepository

    @Before
    fun setup() {
        apiService = mock()
        repository = WhatsAppRepository(apiService)
    }

    @Test
    fun `getConfig returns Success with config`() = runTest {
        val mockConfig = WhatsAppConfigDto("phone", "token", "waba", "webhook", true, true)
        whenever(apiService.getConfig()).thenReturn(
            ApiResponse(success = true, data = mockConfig, error = null, meta = null)
        )

        val result = repository.getConfig()

        assertTrue(result is NetworkResult.Success)
        assertEquals("phone", (result as NetworkResult.Success).data.phone_number_id)
    }

    @Test
    fun `sendAlert returns Success`() = runTest {
        whenever(apiService.sendAlert(any())).thenReturn(
            ApiResponse(success = true, data = emptyMap<String, Any>(), error = null, meta = null)
        )

        val result = repository.sendAlert(1, "1234567890")

        assertTrue(result is NetworkResult.Success)
        assertTrue((result as NetworkResult.Success).data)
    }

    @Test
    fun `sendPo returns Success`() = runTest {
        whenever(apiService.sendPo(any())).thenReturn(
            ApiResponse(success = true, data = emptyMap<String, Any>(), error = null, meta = null)
        )

        val result = repository.sendPo("uuid123", "0987654321")

        assertTrue(result is NetworkResult.Success)
        assertTrue((result as NetworkResult.Success).data)
    }

    @Test
    fun `getLogs returns Success`() = runTest {
        val mockLogs = WhatsAppLogResponse(logs = emptyList(), total = 0, pages = 0)
        whenever(apiService.getLogs(any(), any())).thenReturn(
            ApiResponse(success = true, data = mockLogs, error = null, meta = null)
        )

        val result = repository.getLogs()

        assertTrue(result is NetworkResult.Success)
        assertEquals(0, (result as NetworkResult.Success).data.total)
    }
}
