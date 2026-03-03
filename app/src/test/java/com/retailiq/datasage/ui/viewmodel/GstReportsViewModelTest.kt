package com.retailiq.datasage.ui.viewmodel

import android.content.Context
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.GstSlabDto
import com.retailiq.datasage.data.model.GstSummaryDto
import com.retailiq.datasage.data.repository.GstRepository
import com.retailiq.datasage.ui.reports.GstReportUiState
import com.retailiq.datasage.ui.reports.GstReportsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GstReportsViewModelTest {

    private lateinit var mockRepository: GstRepository
    private lateinit var viewModel: GstReportsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeSummary() = GstSummaryDto(
        period = "2026-02",
        totalTaxable = 100.0,
        totalCgst = 9.0,
        totalSgst = 9.0,
        totalIgst = 0.0,
        invoiceCount = 1,
        status = "PENDING",
        compiledAt = null
    )

    private fun makeSlab() = GstSlabDto(
        rate = 5.0,
        taxableValue = 50.0,
        taxAmount = 2.5
    )

    @Test
    fun `loadData sets Success when both calls succeed`() = runTest {
        val summary = makeSummary()
        val slabs = listOf(makeSlab())

        whenever(mockRepository.getSummary(any())).thenReturn(NetworkResult.Success(summary))
        whenever(mockRepository.getLiabilitySlabs(any())).thenReturn(NetworkResult.Success(slabs))

        viewModel = GstReportsViewModel(mockRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is GstReportUiState.Success)
        val state = viewModel.uiState.value as GstReportUiState.Success
        assertEquals(summary, state.summary)
        assertEquals(slabs, state.slabs)
    }

    @Test
    fun `loadData sets Error when summary fails`() = runTest {
        whenever(mockRepository.getSummary(any())).thenReturn(NetworkResult.Error(0, "Backend down"))
        whenever(mockRepository.getLiabilitySlabs(any())).thenReturn(NetworkResult.Success(listOf(makeSlab())))

        viewModel = GstReportsViewModel(mockRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is GstReportUiState.Error)
    }

    @Test
    fun `exportGstr1 sets error when export fails`() = runTest {
        whenever(mockRepository.getSummary(any())).thenReturn(NetworkResult.Success(makeSummary()))
        whenever(mockRepository.getLiabilitySlabs(any())).thenReturn(NetworkResult.Success(emptyList()))
        whenever(mockRepository.getGstr1(any())).thenReturn(NetworkResult.Error(0, "No content"))

        viewModel = GstReportsViewModel(mockRepository)
        advanceUntilIdle()

        val ctx: Context = mock()
        viewModel.exportGstr1(ctx)
        advanceUntilIdle()

        assertTrue(viewModel.exportMessage.value?.contains("failed") == true)
    }
}
