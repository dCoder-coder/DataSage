package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.ApiError
import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.LoyaltyApiService
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.LoyaltyAccountDto
import com.retailiq.datasage.data.model.RedeemPointsRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import com.retailiq.datasage.data.model.LoyaltyTransactionDto

class LoyaltyRepositoryTest {

    private val api = mock(LoyaltyApiService::class.java)
    private val repo = LoyaltyRepository(api)

    @Test
    fun getAccount_success() = runBlocking {
        val dto = LoyaltyAccountDto(100, 500, 100, 10.0, null)
        whenever(api.getAccount(1)).thenReturn(ApiResponse(true, dto, null, null))

        val result = repo.getAccount(1)
        assertTrue(result is NetworkResult.Success)
        assertEquals(100, (result as NetworkResult.Success).data.pointsBalance)
    }

    @Test
    fun getAccount_failure() = runBlocking {
        whenever(api.getAccount(1)).thenReturn(ApiResponse(false, null, ApiError("NOT_FOUND", "Account not found"), null))

        val result = repo.getAccount(1)
        assertTrue(result is NetworkResult.Error)
        assertEquals("Account not found", (result as NetworkResult.Error).message)
    }

    @Test
    fun redeemPoints_success() = runBlocking {
        val txDto = LoyaltyTransactionDto("tx-123", "REDEEM", 50, "now", null)
        whenever(api.redeemPoints(any(), any())).thenReturn(ApiResponse(true, txDto, null, null))

        val result = repo.redeemPoints(1, "tx-123", 50)
        assertTrue(result is NetworkResult.Success)
    }

    @Test
    fun redeemPoints_throwsException() = runBlocking {
        whenever(api.redeemPoints(any(), any())).thenThrow(RuntimeException("Network error"))

        val result = repo.redeemPoints(1, "tx-123", 50)
        assertTrue(result is NetworkResult.Error)
        assertEquals("Network error: Network error", (result as NetworkResult.Error).message)
    }
}
