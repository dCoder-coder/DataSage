package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.InventoryApiService
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.Product
import com.retailiq.datasage.data.api.toUserMessage
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

class InventoryRepository @Inject constructor(
    private val inventoryApi: InventoryApiService
) {
    private var cachedProducts: List<Product> = emptyList()

    suspend fun getProducts(search: String? = null, forceRefresh: Boolean = false): NetworkResult<List<Product>> {
        if (!forceRefresh && cachedProducts.isNotEmpty() && search == null) {
            return NetworkResult.Success(cachedProducts)
        }
        return safeCall {
            val response = inventoryApi.listProducts(search = search)
            if (response.success && response.data != null) {
                if (search == null) cachedProducts = response.data
                NetworkResult.Success(response.data)
            } else {
                NetworkResult.Error(422, response.error.toUserMessage())
            }
        }
    }

    fun searchCached(query: String): List<Product> {
        if (query.isBlank()) return cachedProducts
        val lower = query.lowercase()
        return cachedProducts.filter {
            it.name.lowercase().contains(lower) ||
                    it.skuCode?.lowercase()?.contains(lower) == true
        }
    }

    fun getCachedProducts(): List<Product> = cachedProducts

    private suspend fun <T> safeCall(block: suspend () -> NetworkResult<T>): NetworkResult<T> = try {
        block()
    } catch (_: SocketTimeoutException) {
        NetworkResult.Error(408, "Request timed out. Please try again.")
    } catch (ex: Exception) {
        Timber.e(ex, "Inventory API error")
        NetworkResult.Error(500, ex.message ?: "Unexpected error")
    }
}
