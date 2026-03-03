package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.ConfirmedItem
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.OcrItemDto
import com.retailiq.datasage.data.api.OcrJobResponse
import com.retailiq.datasage.data.api.OcrUploadResponse
import com.retailiq.datasage.data.api.VisionApiService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class VisionRepositoryTest {

    private lateinit var apiService: VisionApiService
    private lateinit var repository: VisionRepository

    @Before
    fun setup() {
        apiService = mock()
        repository = VisionRepository(apiService)
    }

    @Test
    fun `uploadInvoice returns Success with jobId`() = runTest {
        val mockFile = File.createTempFile("test", ".jpg")
        mockFile.deleteOnExit()
        
        whenever(apiService.uploadInvoice(any())).thenReturn(
            ApiResponse(success = true, data = OcrUploadResponse("job_123"), error = null, meta = null)
        )

        val result = repository.uploadInvoice(mockFile)

        assertTrue(result is NetworkResult.Success)
        assertEquals("job_123", (result as NetworkResult.Success).data)
    }

    @Test
    fun `getJobStatus returns Success with OcrJobResponse`() = runTest {
        val responseData = OcrJobResponse("job_123", "REVIEW", null, emptyList())
        whenever(apiService.getJobStatus("job_123")).thenReturn(
            ApiResponse(success = true, data = responseData, error = null, meta = null)
        )

        val result = repository.getJobStatus("job_123")

        assertTrue(result is NetworkResult.Success)
        assertEquals("REVIEW", (result as NetworkResult.Success).data.status)
    }

    @Test
    fun `confirmJob returns Success`() = runTest {
        whenever(apiService.confirmJob(any(), any())).thenReturn(
            ApiResponse(success = true, data = Any(), error = null, meta = null)
        )

        val items = listOf(ConfirmedItem("item123", 1, 10.0, 100.0))
        val result = repository.confirmJob("job_123", items)

        assertTrue(result is NetworkResult.Success)
    }
}
