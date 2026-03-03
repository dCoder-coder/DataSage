package com.retailiq.datasage.ui.viewmodel

import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.Customer
import com.retailiq.datasage.data.api.CustomerApiService
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.Product
import com.retailiq.datasage.data.model.CreditAccountDto
import com.retailiq.datasage.data.model.LoyaltyAccountDto
import com.retailiq.datasage.data.repository.CreditRepository
import com.retailiq.datasage.data.repository.InventoryRepository
import com.retailiq.datasage.data.repository.LoyaltyRepository
import com.retailiq.datasage.data.repository.TransactionRepository
import com.retailiq.datasage.ui.sales.SaleUiState
import com.retailiq.datasage.ui.sales.SalesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import com.retailiq.datasage.data.model.LoyaltyTransactionDto

@OptIn(ExperimentalCoroutinesApi::class)
class SalesViewModelLoyaltyCreditTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var inventoryRepo: InventoryRepository
    private lateinit var transactionRepo: TransactionRepository
    private lateinit var customerApi: CustomerApiService
    private lateinit var loyaltyRepo: LoyaltyRepository
    private lateinit var creditRepo: CreditRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        inventoryRepo = org.mockito.Mockito.mock(InventoryRepository::class.java)
        transactionRepo = org.mockito.Mockito.mock(TransactionRepository::class.java)
        customerApi = org.mockito.Mockito.mock(CustomerApiService::class.java)
        loyaltyRepo = org.mockito.Mockito.mock(LoyaltyRepository::class.java)
        creditRepo = org.mockito.Mockito.mock(CreditRepository::class.java)
        
        runTest {
            whenever(inventoryRepo.getProducts()).thenReturn(NetworkResult.Success(emptyList()))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectCustomer_loadsLoyaltyAndCreditAndClearsCartDiscounts() = runTest {
        val vm = SalesViewModel(inventoryRepo, transactionRepo, customerApi, loyaltyRepo, creditRepo)
        val customer = Customer(customerId = 1, name = "John", mobileNumber = "123", email = "a@a.com", address = "Address", totalSpend = 100.0, visitCount = 1)

        val loyaltyDto = LoyaltyAccountDto(100, 500, 100, 10.0, null)
        whenever(loyaltyRepo.getAccount(1)).thenReturn(NetworkResult.Success(loyaltyDto))
        val creditDto = CreditAccountDto(50.0, 1000.0, 950.0, false)
        whenever(creditRepo.getAccount(1)).thenReturn(NetworkResult.Success(creditDto))

        vm.selectCustomer(customer)
        advanceUntilIdle()

        assertEquals(customer, vm.selectedCustomer.value)
        assertEquals(loyaltyDto, vm.loyaltyAccount.value)
        assertEquals(creditDto, vm.creditAccount.value)
    }

    @Test
    fun submitSale_withCredit_andCustomer_succeeds() = runTest {
        val vm = SalesViewModel(inventoryRepo, transactionRepo, customerApi, loyaltyRepo, creditRepo)
        val product = Product(productId = 1, name = "Prod", skuCode = "SKU", categoryId = 1, costPrice = 10.0, sellingPrice = 10.0, currentStock = 10.0)
        
        whenever(transactionRepo.createSaleOffline(any())).thenReturn("tx-123")
        vm.addToCart(product)
        
        vm.submitSale("credit")
        advanceUntilIdle()

        assertTrue(vm.saleState.value is SaleUiState.Success)
        assertEquals("tx-123", (vm.saleState.value as SaleUiState.Success).transactionId)
    }

    @Test
    fun submitSale_withLoyaltyRedemption_postsRedemption() = runTest {
        val vm = SalesViewModel(inventoryRepo, transactionRepo, customerApi, loyaltyRepo, creditRepo)
        val customer = Customer(customerId = 1, name = "John", mobileNumber = "123", email = "a@a.com", address = "Address", totalSpend = 100.0, visitCount = 1)
        
        val product = Product(productId = 1, name = "Prod", skuCode = "SKU", categoryId = 1, costPrice = 100.0, sellingPrice = 100.0, currentStock = 10.0)
        vm.addToCart(product) // 100 total
        
        val loyaltyDto = LoyaltyAccountDto(500, 500, 500, 50.0, null)
        whenever(loyaltyRepo.getAccount(1)).thenReturn(NetworkResult.Success(loyaltyDto))
        whenever(transactionRepo.createSaleOffline(any())).thenReturn("tx-123")
        val txDto = LoyaltyTransactionDto("tx-123", "REDEEM", 250, "now", null)
        whenever(loyaltyRepo.redeemPoints(any(), any(), any())).thenReturn(NetworkResult.Success(txDto))

        vm.selectCustomer(customer)
        advanceUntilIdle()
        
        vm.setRedemptionPoints(250) // half points = 25 discount
        advanceUntilIdle()

        assertEquals(75.0, vm.cartTotal, 0.0)

        vm.submitSale("cash")
        advanceUntilIdle()

        assertTrue(vm.saleState.value is SaleUiState.Success)
        verify(loyaltyRepo).redeemPoints(1, "tx-123", 250)
        assertNull(vm.selectedCustomer.value) // Should be cleared
    }
}
