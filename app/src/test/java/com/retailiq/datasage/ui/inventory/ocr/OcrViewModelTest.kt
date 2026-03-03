package com.retailiq.datasage.ui.inventory.ocr

import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.OcrJobResponse
import com.retailiq.datasage.data.repository.InventoryRepository
import com.retailiq.datasage.data.repository.VisionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class OcrViewModelTest {

    private lateinit var visionRepo: VisionRepository
    private lateinit var inventoryRepo: InventoryRepository
    private lateinit var viewModel: OcrViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        visionRepo = mock()
        inventoryRepo = mock()
        viewModel = OcrViewModel(visionRepo, inventoryRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uploadInvoice transitions to Polling on success`() = runTest {
        val mockFile = File.createTempFile("test", ".jpg")
        mockFile.deleteOnExit()
        
        whenever(visionRepo.uploadInvoice(any())).thenReturn(NetworkResult.Success("job_123"))
        
        // mock polling returning REVIEW immediately so it completes
        whenever(visionRepo.getJobStatus("job_123")).thenReturn(
            NetworkResult.Success(OcrJobResponse("job_123", "REVIEW", null, emptyList()))
        )

        viewModel.uploadInvoice(mockFile)
        testDispatcher.scheduler.advanceUntilIdle()

        val endState = viewModel.state.value
        assertTrue(endState is OcrState.Review)
    }

    @Test
    fun `startPolling hits max attempts and fails`() = runTest {
        // mock polling always returning PROCESSING
        whenever(visionRepo.getJobStatus("job_123")).thenReturn(
            NetworkResult.Success(OcrJobResponse("job_123", "PROCESSING", null, emptyList()))
        )

        viewModel.startPolling("job_123")
        
        // Advance time to pass the 30 * 3000ms loop
        testDispatcher.scheduler.advanceTimeBy(100_000L)

        val endState = viewModel.state.value
        assertTrue("Expected Error state due to timeout, got $endState", endState is OcrState.Error)
        assertEquals("Processing is taking longer than expected. Try again later.", (endState as OcrState.Error).message)
    }
}
