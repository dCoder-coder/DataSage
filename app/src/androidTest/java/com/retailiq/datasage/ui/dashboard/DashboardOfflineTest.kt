package com.retailiq.datasage.ui.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.gson.Gson
import com.retailiq.datasage.core.ConnectivityObserver
import com.retailiq.datasage.data.local.AnalyticsSnapshot
import com.retailiq.datasage.data.local.AnalyticsSnapshotDao
import com.retailiq.datasage.data.local.PendingTransactionDao
import com.retailiq.datasage.data.repository.DashboardRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardOfflineTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        val mockRepo = mockk<DashboardRepository>()
        val mockObserver = mockk<ConnectivityObserver>()
        val mockDao = mockk<AnalyticsSnapshotDao>()
        val mockPendingDao = mockk<PendingTransactionDao>()
        val gson = Gson()

        // Setup connectivity as offline
        every { mockObserver.isOnline } returns MutableStateFlow(false)

        // Setup Pending Dao Flow
        every { mockPendingDao.countByStatusFlow(any()) } returns MutableStateFlow(0)

        // Mock room returning a snapshot
        val json = """
            {
               "built_at": "2026-02-28T12:00:00Z",
               "kpis": {
                   "today_revenue": 500.0,
                   "today_profit": 100.0,
                   "today_transactions": 5
               }
            }
        """.trimIndent()
        val snapshot = AnalyticsSnapshot("my_store", json, "2026-02-28T12:00:00Z", 123L)
        coEvery { mockDao.getSnapshot("my_store") } returns snapshot

        viewModel = DashboardViewModel(
            repository = mockRepo,
            connectivityObserver = mockObserver,
            snapshotDao = mockDao,
            gson = gson,
            pendingDao = mockPendingDao
        )
    }

    @Test
    fun offlineState_showsBanner_and_rendersKpis() {
        composeTestRule.setContent {
            DashboardScreen(viewModel = viewModel)
        }
        
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Offline", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("₹500.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transactions").assertIsDisplayed()
        composeTestRule.onNodeWithText("5").assertIsDisplayed()
    }
}
