package com.retailiq.datasage.ui.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.gson.Gson
import com.retailiq.datasage.core.ConnectivityObserver
import com.retailiq.datasage.data.local.AnalyticsSnapshot
import com.retailiq.datasage.data.local.AnalyticsSnapshotDao
import com.retailiq.datasage.data.local.PendingTransactionDao
import com.retailiq.datasage.data.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class DashboardOfflineTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        val mockRepo = mock<DashboardRepository>()
        val mockObserver = mock<ConnectivityObserver>()
        val mockDao = mock<AnalyticsSnapshotDao>()
        val mockPendingDao = mock<PendingTransactionDao>()
        val gson = Gson()

        whenever(mockObserver.isOnline).thenReturn(MutableStateFlow(false))
        whenever(mockPendingDao.countByStatusFlow(any())).thenReturn(MutableStateFlow(0))

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
        runBlocking { whenever(mockDao.getSnapshot("my_store")).thenReturn(snapshot) }

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
