package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.InventoryApiService
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.Product
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
                NetworkResult.Error(422, response.error?.message ?: "Failed to load products")
            }
        }
    }

    fun getCachedProducts(): List<Product> = cachedProducts

    fun searchCached(query: String): List<Product> {
        if (query.isBlank()) return cachedProducts
        val lower = query.lowercase()
        return cachedProducts.filter {
            it.name.lowercase().contains(lower) ||
                    it.sku?.lowercase()?.contains(lower) == true
        }
    }

    private suspend fun <T> safeCall(block: suspend () -> NetworkResult<T>): NetworkResult<T> = try {
        block()
    } catch (_: SocketTimeoutException) {
        NetworkResult.Error(408, "Request timed out")
    } catch (e: Exception) {
        Timber.e(e, "Inventory API error")
        NetworkResult.Error(500, e.message ?: "Unexpected error")
    }
}
