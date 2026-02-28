package com.retailiq.datasage.ui.viewmodel

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PrintJobPollingTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun maxPollAttemptsReached_transitionsToFailed() = runTest {
        val fakeApi = FakePollReceiptsApi()
        val repo = ReceiptsRepository(fakeApi)
        val viewModel = ReceiptsViewModel(repo)

        viewModel.startPrintJob("tx_123", "00:11:22:33:44:55")

        // Progress time enough for exactly 10 polls (10 * 2000ms = 20000ms)
        advanceTimeBy(21000)

        // It should have polled exactly 10 times and then given up and marked as failed
        assertEquals(10, fakeApi.pollCount)
        assertEquals(PrintJobUiState.Failed, viewModel.printJobState.value)
    }

    private class FakePollReceiptsApi : ReceiptsApiService {
        var pollCount = 0

        override suspend fun createPrintJob(body: PrintJobRequest): Response<PrintJobResponse> {
            return Response.success(PrintJobResponse("job_999"))
        }

        override suspend fun pollPrintJob(jobId: String): Response<PrintJobStatusDto> {
            pollCount++
            return Response.success(PrintJobStatusDto("job_999", "PENDING", Instant.now().toString()))
        }

        override suspend fun getTemplate(): Response<ReceiptTemplateDto> = TODO()
        override suspend fun updateTemplate(body: ReceiptTemplateRequest): Response<ReceiptTemplateDto> = TODO()
        override suspend fun lookupBarcode(value: String): Response<BarcodeProductDto> = TODO()
        override suspend fun registerBarcode(body: RegisterBarcodeRequest): Response<BarcodeDto> = TODO()
    }
}
