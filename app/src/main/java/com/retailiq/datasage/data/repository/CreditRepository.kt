package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.CreditApiService
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.CreditAccountDto
import com.retailiq.datasage.data.model.CreditTransactionDto
import com.retailiq.datasage.data.model.RepayCreditRequest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditRepository @Inject constructor(
    private val api: CreditApiService
) {
    suspend fun getAccount(customerId: Int): NetworkResult<CreditAccountDto> {
        return try {
            val response = api.getAccount(customerId)
            if (response.success && response.data != null) {
                NetworkResult.Success(response.data)
            } else {
                NetworkResult.Error(0, response.error?.message ?: "Failed to fetch credit account")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching credit account for $customerId")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }

    suspend fun getTransactions(customerId: Int): NetworkResult<List<CreditTransactionDto>> {
        return try {
            val response = api.getTransactions(customerId)
            if (response.success && response.data != null) {
                NetworkResult.Success(response.data)
            } else {
                NetworkResult.Error(0, response.error?.message ?: "Failed to fetch credit transactions")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching credit transactions for $customerId")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }

    suspend fun repay(customerId: Int, amount: Double, notes: String? = null, paymentMode: String = "cash"): NetworkResult<CreditTransactionDto> {
        return try {
            val response = api.repay(customerId, RepayCreditRequest(amount, notes, paymentMode))
            if (response.success && response.data != null) {
                NetworkResult.Success(response.data)
            } else {
                NetworkResult.Error(0, response.error?.message ?: "Failed to process repayment")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing repayment for $customerId")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }
}
