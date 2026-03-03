package com.retailiq.datasage.ui.viewmodel

import com.retailiq.datasage.data.model.supplier.CreatePoItemRequest
import com.retailiq.datasage.data.model.supplier.PurchaseOrderDto
import com.retailiq.datasage.data.repository.SupplierRepository
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
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseOrderViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: SupplierRepository
    private lateinit var whatsappRepo: com.retailiq.datasage.data.repository.WhatsAppRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mock(SupplierRepository::class.java)
        whatsappRepo = mock(com.retailiq.datasage.data.repository.WhatsAppRepository::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sendPo_optimisticallyUpdatesActionState_andReloadsDetails() = runTest {
        val dto = PurchaseOrderDto("1", "1", "Sup", "SENT", null, 100.0, null, null, null)
        whenever(repo.sendPurchaseOrder("1")).thenReturn(Result.success("PO sent"))
        whenever(repo.getPurchaseOrder("1")).thenReturn(Result.success(dto))
        whenever(repo.getPurchaseOrders(anyOrNull(), anyOrNull())).thenReturn(Result.success(listOf(dto)))

        val vm = PurchaseOrderViewModel(repo, whatsappRepo)

        vm.sendPo("1")
        advanceUntilIdle()

        val actionState = vm.actionState.value
        assertTrue(actionState is PoActionUiState.Success)
        assertEquals("PO Sent successfully", (actionState as PoActionUiState.Success).message)

        val detailState = vm.detailState.value
        assertTrue(detailState is PoDetailUiState.Loaded)
        assertEquals("SENT", (detailState as PoDetailUiState.Loaded).order.status)
    }

    @Test
    fun createPo_handlesEmptyItemsValidation() = runTest {
        val vm = PurchaseOrderViewModel(repo, whatsappRepo)

        vm.createPo("1", null, null, isDraft = true, items = emptyList())
        advanceUntilIdle()

        val actionState = vm.actionState.value
        assertTrue(actionState is PoActionUiState.Error)
        assertEquals("At least one product is required", (actionState as PoActionUiState.Error).message)
    }

    @Test
    fun createPo_savesDraftSuccessfully() = runTest {
        val dto = PurchaseOrderDto("2", "1", "Sup", "DRAFT", null, 50.0, null, null, null)
        whenever(repo.createPurchaseOrder(any())).thenReturn(Result.success("2"))
        whenever(repo.getPurchaseOrders(anyOrNull(), anyOrNull())).thenReturn(Result.success(listOf(dto)))

        val vm = PurchaseOrderViewModel(repo, whatsappRepo)

        vm.createPo("1", null, null, isDraft = true, items = listOf(CreatePoItemRequest(1, 10, 5.0)))
        advanceUntilIdle()

        val actionState = vm.actionState.value
        assertTrue(actionState is PoActionUiState.Success)
        assertEquals("Draft saved", (actionState as PoActionUiState.Success).message)
    }
}
