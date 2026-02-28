package com.retailiq.datasage.ui.staff

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.retailiq.datasage.data.model.StaffSessionDto
import com.retailiq.datasage.data.repository.StaffRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class StaffBannerInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var repository: StaffRepository
    private lateinit var viewModel: StaffViewModel

    @Before
    fun setup() {
        repository = mockk()
        viewModel = StaffViewModel(repository)
    }

    @Test
    fun noActiveSession_showsStartButton() {
        composeTestRule.setContent {
            StaffSessionBanner(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("No Active Session").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Session").assertIsDisplayed()
    }

    @Test
    fun startSession_transitionsToActive_andShowsBanner() {
        val session = StaffSessionDto("s1", "ACTIVE", Instant.now().toString(), null, 0, 0.0)
        coEvery { repository.startSession() } returns Result.success(session)

        composeTestRule.setContent {
            StaffSessionBanner(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Start Session").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Session Active").assertExists()
        composeTestRule.onNodeWithText("End Session").assertIsDisplayed()
    }

    @Test
    fun endSession_showsDialogWithSummary() {
        // Pre-populate ViewModel with an active session
        val mockActiveSession = StaffSessionDto("s1", "ACTIVE", Instant.now().minusSeconds(600).toString(), null, 0, 0.0)
        
        // Use reflection to set the private flow value for testing the transition state
        val field = StaffViewModel::class.java.getDeclaredField("_sessionState")
        field.isAccessible = true
        (field.get(viewModel) as MutableStateFlow<StaffSessionState>).value = StaffSessionState.Active(mockActiveSession, "10 min")

        val endedSession = StaffSessionDto("s1", "ENDED", mockActiveSession.startTime, Instant.now().toString(), 5, 500.0)
        coEvery { repository.endSession() } returns Result.success(endedSession)

        composeTestRule.setContent {
            StaffSessionBanner(viewModel = viewModel)
        }

        // Active session ui is shown
        composeTestRule.onNodeWithText("End Session").performClick()
        composeTestRule.waitForIdle()

        // Verify the summary dialog text is displayed based on endedSession values
        composeTestRule.onNodeWithText("Session Ended").assertIsDisplayed()
        composeTestRule.onNodeWithText("You recorded 5 transactions totalling ₹500.00 in this session.")
            .assertIsDisplayed()
    }
}
