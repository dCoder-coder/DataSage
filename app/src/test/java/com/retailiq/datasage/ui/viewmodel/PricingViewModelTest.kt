package com.retailiq.datasage.ui.viewmodel

import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.PricingSuggestion
import com.retailiq.datasage.data.repository.PricingRepository
import com.retailiq.datasage.ui.pricing.PricingActionState
import com.retailiq.datasage.ui.pricing.PricingUiState
import com.retailiq.datasage.ui.pricing.PricingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.whenever
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class PricingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: PricingRepository

    private val suggestion1 = PricingSuggestion(1, 10, "Apple Juice 1L", 100.0, 115.0, "Competitive drift", 10.0, 15.0, "HIGH")
    private val suggestion2 = PricingSuggestion(2, 11, "Mango Jam 500g", 80.0, 90.0, "Low margin", 8.0, 12.0, "MEDIUM")
    private val suggestion3 = PricingSuggestion(3, 12, "Chips 200g", 30.0, 35.0, "Price opportunity", 5.0, 10.0, "LOW")

    private val threesuggestions = listOf(suggestion1, suggestion2, suggestion3)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mock(PricingRepository::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── loadSuggestions ─────────────────────────────────────────────────────

    @Test
    fun loadSuggestions_populatesStateWithList() = runTest {
        whenever(repo.getSuggestions()).thenReturn(NetworkResult.Success(threesuggestions))

        val vm = PricingViewModel(repo)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is PricingUiState.Loaded)
        assertEquals(3, (state as PricingUiState.Loaded).suggestions.size)
    }

    @Test
    fun loadSuggestions_setsErrorState_onFailure() = runTest {
        whenever(repo.getSuggestions()).thenReturn(NetworkResult.Error(500, "Server error"))

        val vm = PricingViewModel(repo)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is PricingUiState.Error)
        assertEquals("Server error", (state as PricingUiState.Error).message)
    }

    // ─── applySuggestion ─────────────────────────────────────────────────────

    @Test
    fun applySuggestion_removesSuggestionFromList() = runTest {
        whenever(repo.getSuggestions()).thenReturn(NetworkResult.Success(threesuggestions))
        whenever(repo.applySuggestion(1)).thenReturn(NetworkResult.Success(Unit))

        val vm = PricingViewModel(repo)
        advanceUntilIdle() // load suggestions first

        vm.applySuggestion(1)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is PricingUiState.Loaded)
        val list = (state as PricingUiState.Loaded).suggestions
        assertEquals(2, list.size)
        assertTrue(list.none { it.id == 1 })
    }

    @Test
    fun applySuggestion_setsSuccessAction_onApply() = runTest {
        whenever(repo.getSuggestions()).thenReturn(NetworkResult.Success(threesuggestions))
        whenever(repo.applySuggestion(2)).thenReturn(NetworkResult.Success(Unit))

        val vm = PricingViewModel(repo)
        advanceUntilIdle()

        vm.applySuggestion(2)
        advanceUntilIdle()

        val actionState = vm.actionState.value
        assertTrue(actionState is PricingActionState.Success)
    }

    @Test
    fun applySuggestion_setsErrorAction_onFailure() = runTest {
        whenever(repo.getSuggestions()).thenReturn(NetworkResult.Success(threesuggestions))
        whenever(repo.applySuggestion(1)).thenReturn(NetworkResult.Error(422, "Apply failed"))

        val vm = PricingViewModel(repo)
        advanceUntilIdle()

        vm.applySuggestion(1)
        advanceUntilIdle()

        val actionState = vm.actionState.value
        assertTrue(actionState is PricingActionState.Error)
        assertEquals("Apply failed", (actionState as PricingActionState.Error).message)
    }

    // ─── dismissSuggestion ───────────────────────────────────────────────────

    @Test
    fun dismissSuggestion_removesSuggestionFromList() = runTest {
        whenever(repo.getSuggestions()).thenReturn(NetworkResult.Success(threesuggestions))
        whenever(repo.dismissSuggestion(3)).thenReturn(NetworkResult.Success(Unit))

        val vm = PricingViewModel(repo)
        advanceUntilIdle()

        vm.dismissSuggestion(3)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is PricingUiState.Loaded)
        val list = (state as PricingUiState.Loaded).suggestions
        assertEquals(2, list.size)
        assertTrue(list.none { it.id == 3 })
    }

    // ─── resetAction ─────────────────────────────────────────────────────────

    @Test
    fun resetAction_clearsActionState() = runTest {
        whenever(repo.getSuggestions()).thenReturn(NetworkResult.Success(threesuggestions))
        whenever(repo.applySuggestion(1)).thenReturn(NetworkResult.Success(Unit))

        val vm = PricingViewModel(repo)
        advanceUntilIdle()
        vm.applySuggestion(1)
        advanceUntilIdle()

        vm.resetAction()

        assertTrue(vm.actionState.value is PricingActionState.Idle)
    }
}
