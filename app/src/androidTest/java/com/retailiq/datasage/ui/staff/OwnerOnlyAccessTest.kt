package com.retailiq.datasage.ui.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class OwnerOnlyAccessTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun nonOwnerAccess_showsAccessRestrictedMessage() {
        // Render the StaffPerformanceScreen with a STAFF role
        // This should trigger the RoleGuard to block access and show the fallback UI
        composeTestRule.setContent {
            StaffPerformanceScreen(
                userRole = "STAFF",
                onNavigateBack = {}
            )
        }

        // Verify the RoleGuard default text is displayed
        composeTestRule.onNodeWithText("Access restricted").assertIsDisplayed()
        
        // Ensure that the actual screen content (e.g. "Staff Performance" title) is NOT displayed
        composeTestRule.onNodeWithText("Staff Performance").assertDoesNotExist()
    }
    
    @Test
    fun ownerAccess_showsPerformanceScreen() {
        // Render the StaffPerformanceScreen with an OWNER role
        composeTestRule.setContent {
            StaffPerformanceScreen(
                userRole = "OWNER",
                onNavigateBack = {}
            )
        }

        // Verify the screen content is displayed
        composeTestRule.onNodeWithText("Staff Performance").assertIsDisplayed()
        
        // Ensure that the access restricted text is NOT displayed
        composeTestRule.onNodeWithText("Access restricted").assertDoesNotExist()
    }
}
