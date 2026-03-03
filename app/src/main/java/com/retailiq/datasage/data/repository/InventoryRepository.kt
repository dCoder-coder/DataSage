package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.AuditRequest
import com.retailiq.datasage.data.api.AuditResponse
import com.retailiq.datasage.data.api.CreateProductRequest
import com.retailiq.datasage.data.api.InventoryApiService
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.PriceHistoryEntry
import com.retailiq.datasage.data.api.Product
import com.retailiq.datasage.data.api.StockUpdateRequest
import com.retailiq.datasage.data.api.StockUpdateResponse
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

    fun deductStockLocally(soldItems: Map<Int, Double>) {
        if (cachedProducts.isEmpty() || soldItems.isEmpty()) return
        cachedProducts = cachedProducts.map { product ->
            soldItems[product.productId]?.let { qtySold ->
                product.copy(currentStock = maxOf(0.0, product.currentStock - qtySold))
            } ?: product
        }
    }

    suspend fun createProduct(
        name: String,
        costPrice: Double,
        sellingPrice: Double,
        hsnCode: String? = null,
        gstRate: Double? = null
    ): NetworkResult<Product> = safeCall {
        val request = CreateProductRequest(
            name = name,
            costPrice = costPrice,
            sellingPrice = sellingPrice
        )
        val response = inventoryApi.createProduct(request)
        if (response.success && response.data != null) {
            if (cachedProducts.isNotEmpty()) {
                cachedProducts = cachedProducts + response.data
            }
            NetworkResult.Success(response.data)
        } else {
            NetworkResult.Error(422, response.error.toUserMessage())
        }
    }

    suspend fun getPriceHistory(productId: Int): NetworkResult<List<PriceHistoryEntry>> = safeCall {
        val response = inventoryApi.getPriceHistory(productId)
        if (response.success && response.data != null) {
            NetworkResult.Success(response.data)
        } else {
            NetworkResult.Error(422, response.error.toUserMessage())
        }
    }

    suspend fun updateStock(productId: Int, request: com.retailiq.datasage.data.api.StockUpdateRequest): NetworkResult<com.retailiq.datasage.data.api.StockUpdateResponse> = safeCall {
        val response = inventoryApi.updateStock(productId, request)
        if (response.success && response.data != null) {
            val updatedStock = response.data.newStock
            if (cachedProducts.isNotEmpty()) {
                cachedProducts = cachedProducts.map {
                    if (it.productId == productId) it.copy(currentStock = updatedStock) else it
                }
            }
            NetworkResult.Success(response.data)
        } else {
            NetworkResult.Error(422, response.error.toUserMessage())
        }
    }

    suspend fun submitAudit(request: com.retailiq.datasage.data.api.AuditRequest): NetworkResult<com.retailiq.datasage.data.api.AuditResponse> = safeCall {
        val response = inventoryApi.submitAudit(request)
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
        Timber.e(ex, "Inventory API error")
        NetworkResult.Error(500, ex.message ?: "Unexpected error")
    }
}
