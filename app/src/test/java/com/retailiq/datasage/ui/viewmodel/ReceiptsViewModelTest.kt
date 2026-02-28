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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptsViewModelTest {

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
    fun barcodeLookup_idle_initially() = runTest {
        val repo = ReceiptsRepository(FakeReceiptsApi(false))
        val viewModel = ReceiptsViewModel(repo)

        assertEquals(BarcodeLookupUiState.Idle, viewModel.barcodeLookupState.value)
    }

    @Test
    fun barcodeLookup_loading_thenSuccess() = runTest {
        val repo = ReceiptsRepository(FakeReceiptsApi(false))
        val viewModel = ReceiptsViewModel(repo)

        val states = mutableListOf<BarcodeLookupUiState>()
        val job = launch {
            viewModel.barcodeLookupState.toList(states)
        }

        viewModel.lookupBarcode("123456")
        advanceUntilIdle()

        assertTrue(states.contains(BarcodeLookupUiState.Loading))
        val lastState = states.last()
        assertTrue(lastState is BarcodeLookupUiState.Success)
        assertEquals("Test Product", (lastState as BarcodeLookupUiState.Success).product.productName)

        job.cancel()
    }

    @Test
    fun barcodeLookup_loading_thenError() = runTest {
        val repo = ReceiptsRepository(FakeReceiptsApi(true))
        val viewModel = ReceiptsViewModel(repo)

        val states = mutableListOf<BarcodeLookupUiState>()
        val job = launch {
            viewModel.barcodeLookupState.toList(states)
        }

        viewModel.lookupBarcode("999999")
        advanceUntilIdle()

        assertTrue(states.contains(BarcodeLookupUiState.Loading))
        val lastState = states.last()
        assertTrue(lastState is BarcodeLookupUiState.Error)
        assertEquals("Product not found for barcode: 999999", (lastState as BarcodeLookupUiState.Error).message)

        job.cancel()
    }

    private class FakeReceiptsApi(val notFound: Boolean) : ReceiptsApiService {
        override suspend fun getTemplate(): Response<ReceiptTemplateDto> = TODO()
        override suspend fun updateTemplate(body: ReceiptTemplateRequest): Response<ReceiptTemplateDto> = TODO()
        override suspend fun createPrintJob(body: PrintJobRequest): Response<PrintJobResponse> = TODO()
        override suspend fun pollPrintJob(jobId: String): Response<PrintJobStatusDto> = TODO()

        override suspend fun lookupBarcode(value: String): Response<BarcodeProductDto> {
            if (notFound) return Response.error(404, "".toResponseBody(null))
            return Response.success(BarcodeProductDto(1, "Test Product", 10.0, 100.0))
        }

        override suspend fun registerBarcode(body: RegisterBarcodeRequest): Response<BarcodeDto> = TODO()
    }
}
