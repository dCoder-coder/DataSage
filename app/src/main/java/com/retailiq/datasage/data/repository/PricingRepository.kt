package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.InventoryApiService
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.PriceHistoryEntry
import com.retailiq.datasage.data.api.PricingApiService
import com.retailiq.datasage.data.api.PricingSuggestion
import com.retailiq.datasage.data.api.toUserMessage
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

class PricingRepository @Inject constructor(
    private val pricingApi: PricingApiService,
    private val inventoryApi: InventoryApiService
) {

    /**
     * Fetches all PENDING pricing suggestions from the backend.
     */
    suspend fun getSuggestions(): NetworkResult<List<PricingSuggestion>> = safeCall {
        val response = pricingApi.getSuggestions()
        if (response.success && response.data != null) {
            NetworkResult.Success(response.data)
        } else {
            NetworkResult.Error(422, response.error.toUserMessage())
        }
    }

    /**
     * Applies a pricing suggestion, instructing the backend to update the product price.
     */
    suspend fun applySuggestion(id: Int): NetworkResult<Unit> = safeCall {
        val response = pricingApi.applySuggestion(id)
        if (response.success) {
            NetworkResult.Success(Unit)
        } else {
            NetworkResult.Error(422, response.error.toUserMessage())
        }
    }

    /**
     * Dismisses a pricing suggestion without applying the suggested price change.
     */
    suspend fun dismissSuggestion(id: Int): NetworkResult<Unit> = safeCall {
        val response = pricingApi.dismissSuggestion(id)
        if (response.success) {
            NetworkResult.Success(Unit)
        } else {
            NetworkResult.Error(422, response.error.toUserMessage())
        }
    }

    /**
     * Retrieves the price change history for a specific product.
     * Reuses the existing InventoryApiService endpoint.
     */
    suspend fun getPriceHistory(productId: Int): NetworkResult<List<PriceHistoryEntry>> = safeCall {
        val response = inventoryApi.getPriceHistory(productId)
        if (response.success && response.data != null) {
            NetworkResult.Success(response.data)
        } else {
            NetworkResult.Error(422, response.error.toUserMessage())
        }
    }

    private suspend fun <T> safeCall(block: suspend () -> NetworkResult<T>): NetworkResult<T> = try {
        block()
    } catch (_: SocketTimeoutException) {
        NetworkResult.Error(408, "Request timed out. Please try again.")
    } catch (ex: Exception) {
        Timber.e(ex, "Pricing API error")
        NetworkResult.Error(500, ex.message ?: "Unexpected error")
    }
}
