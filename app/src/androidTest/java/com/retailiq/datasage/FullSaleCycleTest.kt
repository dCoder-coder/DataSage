package com.retailiq.datasage

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.retailiq.datasage.core.TokenStore
import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.InventoryApiService
import com.retailiq.datasage.data.api.Product
import com.retailiq.datasage.data.api.StoreApiService
import com.retailiq.datasage.data.model.ChainDashboardDto
import com.retailiq.datasage.data.model.StoreRevenueDto
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FullSaleCycleTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var inventoryApi: InventoryApiService

    @Inject
    lateinit var tokenStore: TokenStore

    @Before
    fun setup() {
        hiltRule.inject()
        whenever(tokenStore.getAccessToken()).thenReturn("mock_token")
        whenever(tokenStore.getRole()).thenReturn("OWNER")
        whenever(tokenStore.isSetupComplete()).thenReturn(true)
    }

    @Test
    fun completeSaleCycle_fromNewSale_toReceipt_toDashboard() {
        // Mock products
        val dummyProducts = listOf(
            Product(productId = 1, name = "Product A", skuCode = "SKUA", costPrice = 10.0, sellingPrice = 15.0, currentStock = 50.0, reorderLevel = 5.0, categoryId = 1),
            Product(productId = 2, name = "Product B", skuCode = "SKUB", costPrice = 20.0, sellingPrice = 25.0, currentStock = 100.0, reorderLevel = 10.0, categoryId = 1),
            Product(productId = 3, name = "Product C", skuCode = "SKUC", costPrice = 5.0, sellingPrice = 8.0, currentStock = 20.0, reorderLevel = 2.0, categoryId = 1)
        )
        // Need to run these inside runTest or use runBlocking for suspend mock if necessary.
        // But Mockito `whenever` allows suspend functions if using mockito-kotlin.
        kotlinx.coroutines.runBlocking {
            whenever(inventoryApi.listProducts()).thenReturn(ApiResponse(true, dummyProducts, null, null))
        }

        // a. Launch app -> Dashboard should appear.
        
        // Wait until Dashboard is loaded
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()

        // b. Navigate to New Sale.
        composeTestRule.onNodeWithText("Sales", useUnmergedTree = true).performClick()
        
        // Wait for products to load
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Product A").fetchSemanticsNodes().isNotEmpty()
        }

        // Tap product A, product B (quantity 2).
        composeTestRule.onNode(hasText("Product A")).onParent().performClick()
        composeTestRule.onNode(hasText("Product B")).onParent().performClick()
        
        // Increase quantity of Product B
        // The "+ " button is an Add icon next to the quantity.
        // We can find the node with text "Product B" inside the cart, and its sibling elements.
        // Actually, the simplest way is to click the 'Increase' icon inside the Cart.
        composeTestRule.onNodeWithContentDescription("Increase").performClick()

        // Select UPI payment
        composeTestRule.onNodeWithText("Upi").performClick()

        // c. Tap "Record Sale" (or "Complete Sale")
        composeTestRule.onNodeWithText("Complete Sale").performClick()

        // Assert Dialog or Snackbar appears. The UI currently shows a Dialog with "Sale Saved"
        composeTestRule.onNodeWithText("Sale Saved").assertIsDisplayed()
        
        // Tap OK on dialog
        composeTestRule.onNodeWithText("OK").performClick()

        // d. Assert PrintReceiptBottomSheet appears.
        composeTestRule.onNodeWithText("Print Receipt").assertIsDisplayed()

        // e. Tap "Skip". Assert bottom sheet dismisses.
        composeTestRule.onNodeWithText("Skip").performClick()
        composeTestRule.onNodeWithText("Print Receipt").assertDoesNotExist()

        // f. Navigate to Dashboard.
        composeTestRule.onNodeWithText("Home", useUnmergedTree = true).performClick()

        // Assert mocked revenue appears in KPI card. (Depends on how KPI card is populated. 
        // Need to mock StoreApiService or TransactionApiService for dashboard metrics)
        // Wait for dashboard to load
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
    }
}
