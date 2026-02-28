package com.retailiq.datasage.ui.purchaseorder

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.retailiq.datasage.data.model.supplier.PurchaseOrderDto
import com.retailiq.datasage.data.model.supplier.PurchaseOrderItemDto
import com.retailiq.datasage.data.repository.SupplierRepository
import com.retailiq.datasage.ui.viewmodel.PurchaseOrderViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class GoodsReceiptInstrumented {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun verifyItemsRender_andConfirmButtonEnables() {
        // Setup mock repository & ViewModel
        val repo = mock(SupplierRepository::class.java)
        
        val dummyItems = listOf(
            PurchaseOrderItemDto(1, 101, "Test Widget", 50, 0, 10.0, 500.0)
        )
        val dummyPo = PurchaseOrderDto(
            id = 99,
            supplierId = 1,
            supplierName = "Acme Corp",
            status = "SENT", // Not FULFILLED, so confirm button should render active
            expectedDelivery = null,
            totalAmount = 500.0,
            notes = null,
            createdAt = "2024-01-01",
            updatedAt = null,
            items = dummyItems
        )
        
        // This makes sure the launch effect loads it instantly for the test
        whenever(repo.getPurchaseOrder(99)).thenReturn(Result.success(dummyPo))
        
        // Mock ViewModel using the mocked repo
        val viewModel = PurchaseOrderViewModel(repo)

        composeTestRule.setContent {
            GoodsReceiptScreen(
                poId = 99,
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // We use composeTestRule to advance until idle because of coroutines loading Data
        composeTestRule.waitForIdle()

        // 1. Verify general rendering
        composeTestRule.onNodeWithText("Order Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("Supplier: Acme Corp").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Widget").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ordered: 50").assertIsDisplayed()

        // 2. Verify input manipulation (Finding the TextField for Quantity)
        // In the UI, the label is "Recv Qty" with a default value matching "ordered" (50)
        composeTestRule.onNodeWithText("50").assertExists()
        
        // Type a new value to simulate reception differences
        composeTestRule.onNodeWithText("50").performTextClearance()
        composeTestRule.onNodeWithText("50").performTextInput("45")

        // 3. Confirm receipt button validation
        composeTestRule.onNodeWithText("Confirm Receipt").assertIsDisplayed().assertIsEnabled().performClick()
        
        // 4. Validate confirmation dialog opens
        composeTestRule.onNodeWithText("Confirm Receipt").assertIsDisplayed() // The dialog title
        composeTestRule.onNodeWithText("This will update your stock levels significantly. Proceed?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed().performClick()
        
        // 5. Verify the dialog went away after clicking cancel
        composeTestRule.onNodeWithText("This will update your stock levels significantly. Proceed?").assertDoesNotExist()
    }
}
