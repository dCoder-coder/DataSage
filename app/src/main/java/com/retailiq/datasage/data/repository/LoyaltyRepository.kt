package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.LoyaltyApiService
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.LoyaltyAccountDto
import com.retailiq.datasage.data.model.LoyaltyAnalyticsDto
import com.retailiq.datasage.data.model.LoyaltyProgramSettingsDto
import com.retailiq.datasage.data.model.LoyaltyTransactionDto
import com.retailiq.datasage.data.model.RedeemPointsRequest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoyaltyRepository @Inject constructor(
    private val api: LoyaltyApiService
) {
    suspend fun getAccount(customerId: Int): NetworkResult<LoyaltyAccountDto> {
        return try {
            val response = api.getAccount(customerId)
            if (response.success && response.data != null) {
                NetworkResult.Success(response.data)
            } else {
                NetworkResult.Error(0, response.error?.message ?: "Failed to fetch loyalty account")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching loyalty account for $customerId")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }

    suspend fun getTransactions(customerId: Int): NetworkResult<List<LoyaltyTransactionDto>> {
        return try {
            val response = api.getTransactions(customerId)
            if (response.success && response.data != null) {
                NetworkResult.Success(response.data)
            } else {
                NetworkResult.Error(0, response.error?.message ?: "Failed to fetch loyalty transactions")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching loyalty transactions for $customerId")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }

    suspend fun redeemPoints(customerId: Int, transactionId: String, points: Int): NetworkResult<LoyaltyTransactionDto> {
        return try {
            val response = api.redeemPoints(customerId, RedeemPointsRequest(transactionId, points))
            if (response.success && response.data != null) {
                NetworkResult.Success(response.data)
            } else {
                NetworkResult.Error(0, response.error?.message ?: "Failed to redeem points")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error redeeming points for $customerId")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }

    suspend fun getSettings(): NetworkResult<LoyaltyProgramSettingsDto> {
        return try {
            val response = api.getSettings()
            if (response.success && response.data != null) {
                NetworkResult.Success(response.data)
            } else {
                NetworkResult.Error(0, response.error?.message ?: "Failed to get program settings")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching loyalty program settings")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }

    suspend fun updateSettings(settings: LoyaltyProgramSettingsDto): NetworkResult<LoyaltyProgramSettingsDto> {
        return try {
            val response = api.updateSettings(settings)
            if (response.success && response.data != null) {
                NetworkResult.Success(response.data)
            } else {
                NetworkResult.Error(0, response.error?.message ?: "Failed to update settings")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating loyalty program settings")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }

    suspend fun getAnalytics(): NetworkResult<LoyaltyAnalyticsDto> {
        return try {
            val response = api.getAnalytics()
            if (response.success && response.data != null) {
                NetworkResult.Success(response.data)
            } else {
                NetworkResult.Error(0, response.error?.message ?: "Failed to get loyalty analytics")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching loyalty analytics")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }
}
