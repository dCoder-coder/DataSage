package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.ApiError
import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.CreditApiService
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.CreditAccountDto
import com.retailiq.datasage.data.model.CreditTransactionDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class CreditRepositoryTest {

    private val api = mock(CreditApiService::class.java)
    private val repo = CreditRepository(api)

    @Test
    fun getAccount_success() = runBlocking {
        val dto = CreditAccountDto(1000.0, 5000.0, 4000.0, false)
        whenever(api.getAccount(1)).thenReturn(ApiResponse(true, dto, null, null))

        val result = repo.getAccount(1)
        assertTrue(result is NetworkResult.Success)
        assertEquals(1000.0, (result as NetworkResult.Success).data.currentBalance, 0.0)
    }

    @Test
    fun repay_success() = runBlocking {
        val txDto = CreditTransactionDto("1", "REPAYMENT", 500.0, "2023-10-10T10:00:00Z", "Paid")
        whenever(api.repay(any(), any())).thenReturn(ApiResponse(true, txDto, null, null))

        val result = repo.repay(1, 500.0, "Paid")
        assertTrue(result is NetworkResult.Success)
        assertEquals("REPAYMENT", (result as NetworkResult.Success).data.type)
    }

    @Test
    fun exception_mapsToFailure() = runBlocking {
        whenever(api.getTransactions(1)).thenThrow(RuntimeException("Crash"))

        val result = repo.getTransactions(1)
        assertTrue(result is NetworkResult.Error)
        assertEquals("Network error: Crash", (result as NetworkResult.Error).message)
    }
}
