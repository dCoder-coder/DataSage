package com.retailiq.datasage.ui.viewmodel

import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.HsnDto
import com.retailiq.datasage.data.repository.GstRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HsnSearchViewModelTest {

    private lateinit var mockRepository: GstRepository
    private lateinit var viewModel: HsnSearchViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mock()
        viewModel = HsnSearchViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `empty query keeps Idle state`() = runTest {
        viewModel.updateSearchQuery("")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is HsnSearchUiState.Idle)
    }

    @Test
    fun `valid query triggers search after debounce`() = runTest {
        val query = "1234"
        val expected = listOf(
            HsnDto(hsn_code = "123412", description = "Test", default_rate = 18.0)
        )
        whenever(mockRepository.searchHsn(query)).thenReturn(NetworkResult.Success(expected))

        viewModel.updateSearchQuery(query)

        // Advance past debounce (300ms) and let coroutines run
        advanceTimeBy(350)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success but got $state", state is HsnSearchUiState.Success)
        assertEquals(expected, (state as HsnSearchUiState.Success).results)
    }

    @Test
    fun `backend error sets Error state`() = runTest {
        val query = "error"
        whenever(mockRepository.searchHsn(query))
            .thenReturn(NetworkResult.Error(0, "API Failed"))

        viewModel.updateSearchQuery(query)
        advanceTimeBy(350)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Error but got $state", state is HsnSearchUiState.Error)
        assertEquals("API Failed", (state as HsnSearchUiState.Error).message)
    }
}
