package com.retailiq.datasage.ui.viewmodel

import com.retailiq.datasage.data.model.supplier.CreateSupplierRequest
import com.retailiq.datasage.data.model.supplier.SupplierDto
import com.retailiq.datasage.data.model.supplier.SupplierProfileDto
import com.retailiq.datasage.data.model.supplier.CreatePoRequest
import com.retailiq.datasage.data.model.supplier.GoodsReceiptRequest
import com.retailiq.datasage.data.model.supplier.PurchaseOrderDto
import com.retailiq.datasage.data.repository.SupplierRepository
import com.retailiq.datasage.data.api.SupplierApiService
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
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SupplierViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: SupplierRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mock(SupplierRepository::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadSuppliers_emitsLoadedState() = runTest {
        val list = listOf(SupplierDto("1", "S1", null, null, null, 30, null, null, null))
        whenever(repo.getSuppliers()).thenReturn(Result.success(list))

        val vm = SupplierViewModel(repo)
        advanceUntilIdle()

        val state = vm.listState.value
        assertTrue(state is SupplierListUiState.Loaded)
        assertEquals(1, (state as SupplierListUiState.Loaded).suppliers.size)
    }

    @Test
    fun loadSuppliers_emitsErrorState() = runTest {
        whenever(repo.getSuppliers()).thenReturn(Result.failure(Exception("Server Error")))

        val vm = SupplierViewModel(repo)
        advanceUntilIdle()

        val state = vm.listState.value
        assertTrue(state is SupplierListUiState.Error)
        assertEquals("Server Error", (state as SupplierListUiState.Error).message)
    }

    @Test
    fun createSupplier_emitsSuccess_andReloadsList() = runTest {
        val s1 = SupplierDto("1", "S1", null, null, null, 30, null, null, null)
        whenever(repo.getSuppliers()).thenReturn(Result.success(emptyList())) // Initial load
        whenever(repo.createSupplier(any())).thenReturn(Result.success("1"))

        val vm = SupplierViewModel(repo)
        advanceUntilIdle() // Process initial load
        
        // Mock a different return for the subsequent reload
        whenever(repo.getSuppliers()).thenReturn(Result.success(listOf(s1)))

        vm.createSupplier("S1", null, null, null, 30)
        advanceUntilIdle()

        val listState = vm.listState.value
        assertTrue(listState is SupplierListUiState.Loaded)
        assertEquals(1, (listState as SupplierListUiState.Loaded).suppliers.size)

        val createState = vm.createState.value
        assertTrue(createState is SupplierCreateUiState.Success)
        // createSupplier now returns String ID, not the full SupplierDto
        assertEquals("1", (createState as SupplierCreateUiState.Success).supplierId)
    }

    @Test
    fun createSupplier_withBlankName_emitsError() = runTest {
        whenever(repo.getSuppliers()).thenReturn(Result.success(emptyList()))

        val vm = SupplierViewModel(repo)
        advanceUntilIdle()

        vm.createSupplier("", null, null, null, 30)
        advanceUntilIdle()

        val createState = vm.createState.value
        assertTrue(createState is SupplierCreateUiState.Error)
        assertEquals("Supplier name is required", (createState as SupplierCreateUiState.Error).message)
    }
}
