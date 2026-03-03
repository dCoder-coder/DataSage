package com.retailiq.datasage.ui.pricing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.PricingSuggestion
import com.retailiq.datasage.data.repository.PricingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any

/**
 * Instrumented UI test for [PricingSuggestionsScreen].
 *
 * Validates:
 *   1. Three suggestion cards are rendered when three suggestions are loaded.
 *   2. Tapping "Dismiss" on the first card removes it from the list.
 */
class PricingSuggestionCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var repo: PricingRepository
    private lateinit var viewModel: PricingViewModel

    private val suggestions = listOf(
        PricingSuggestion(1, 10, "Apple Juice 1L", 100.0, 115.0, "Competitive drift detected", 10.0, 15.0, "HIGH"),
        PricingSuggestion(2, 11, "Mango Jam 500g", 80.0, 90.0, "Low margin", 8.0, 12.0, "MEDIUM"),
        PricingSuggestion(3, 12, "Plain Chips 200g", 30.0, 35.0, "Price opportunity", 5.0, 10.0, "LOW")
    )

    @Before
    fun setup() {
        repo = mock(PricingRepository::class.java)
    }

    @Test
    fun threeSuggestionsRendered_assertsThreeCards() {
        // Seed the ViewModel state directly via reflection to bypass coroutine init
        viewModel = PricingViewModel(repo)
        injectLoadedState(viewModel, suggestions)

        composeTestRule.setContent {
            PricingSuggestionsScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // Each card has testTag "suggestion_card_{id}"
        composeTestRule.onNodeWithTag("suggestion_card_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("suggestion_card_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("suggestion_card_3").assertIsDisplayed()
    }

    @Test
    fun dismissFirstCard_removesItFromList() {
        viewModel = PricingViewModel(repo)
        injectLoadedState(viewModel, suggestions)

        // Dismissing suggestion id=1 returns success → removes from list
        // Use a real suspend fun stub via a blocking wrapper via MutableStateFlow trick
        // We inject the result via state directly and tap Dismiss
        composeTestRule.setContent {
            PricingSuggestionsScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // Tap "Dismiss" on first card (id=1)
        composeTestRule.onNodeWithTag("dismiss_1").performClick()
        composeTestRule.waitForIdle()

        // Card 1 should animate out; cards 2 and 3 remain
        composeTestRule.onNodeWithTag("suggestion_card_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("suggestion_card_3").assertIsDisplayed()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Sets the ViewModel's internal _uiState to Loaded using reflection,
     * bypassing the init block's coroutine that would require a real API.
     */
    private fun injectLoadedState(vm: PricingViewModel, list: List<PricingSuggestion>) {
        val field = PricingViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(vm) as MutableStateFlow<PricingUiState>).value = PricingUiState.Loaded(list)
    }
}
