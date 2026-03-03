package com.retailiq.datasage.ui.dashboard

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import com.retailiq.datasage.data.api.DailySummary
import com.retailiq.datasage.data.api.DashboardPayload
import com.retailiq.datasage.data.api.Revenue7dPoint
import com.retailiq.datasage.data.api.TodayKpis
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class DashboardChartIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboardScreen_rendersCharts_whenDataIsLoaded() {
        // Mock ViewModel
        val mockViewModel = mock<DashboardViewModel>()

        // Stub state with mock chart data
        val mockData = DashboardPayload(
            todayKpis = TodayKpis(revenue = 1000.0),
            revenue7d = listOf(
                Revenue7dPoint("Mon", 100.0),
                Revenue7dPoint("Tue", 200.0)
            )
        )
        val stateFlow = MutableStateFlow<DashboardUiState>(DashboardUiState.Loaded(mockData))
        
        // Return dummy StateFlows
        whenever(mockViewModel.uiState).thenReturn(stateFlow)
        whenever(mockViewModel.pendingCount).thenReturn(MutableStateFlow(0))
        whenever(mockViewModel.failedCount).thenReturn(MutableStateFlow(0))

        composeTestRule.setContent {
            DashboardScreen(viewModel = mockViewModel)
        }

        // Assert our charts exist via test tags applied to them
        composeTestRule.onNodeWithTag("RevenueLineChart").assertIsDisplayed()
        composeTestRule.onNodeWithTag("CategoryPieChart").assertIsDisplayed()
    }
}
