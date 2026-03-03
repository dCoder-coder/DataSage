package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.SupplierApiService
import com.retailiq.datasage.data.api.toUserMessage
import com.retailiq.datasage.data.model.supplier.CreatePoRequest
import com.retailiq.datasage.data.model.supplier.CreateSupplierRequest
import com.retailiq.datasage.data.model.supplier.GoodsReceiptRequest
import com.retailiq.datasage.data.model.supplier.PurchaseOrderDto
import com.retailiq.datasage.data.model.supplier.SupplierDto
import com.retailiq.datasage.data.model.supplier.SupplierProfileDto
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

class SupplierRepository @Inject constructor(
    private val api: SupplierApiService
) {
    suspend fun getSuppliers(): Result<List<SupplierDto>> = safeCall {
        val response = api.getSuppliers()
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.error.toUserMessage()))
        }
    }

    suspend fun getSupplierProfile(id: String): Result<SupplierProfileDto> = safeCall {
        val response = api.getSupplierProfile(id)
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.error.toUserMessage()))
        }
    }

    /** Returns the new supplier's UUID ID string on success. */
    suspend fun createSupplier(request: CreateSupplierRequest): Result<String> = safeCall {
        val response = api.createSupplier(request)
        if (response.success && response.data != null) {
            val id = response.data["id"] ?: throw Exception("Missing id in response")
            Result.success(id)
        } else {
            Result.failure(Exception(response.error.toUserMessage()))
        }
    }

    suspend fun getPurchaseOrders(supplierId: String? = null, status: String? = null): Result<List<PurchaseOrderDto>> = safeCall {
        val response = api.getPurchaseOrders(supplierId, status)
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.error.toUserMessage()))
        }
    }

    suspend fun getPurchaseOrder(id: String): Result<PurchaseOrderDto> = safeCall {
        val response = api.getPurchaseOrder(id)
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.error.toUserMessage()))
        }
    }

    /** Returns the new PO's UUID ID string on success. */
    suspend fun createPurchaseOrder(request: CreatePoRequest): Result<String> = safeCall {
        val response = api.createPurchaseOrder(request)
        if (response.success && response.data != null) {
            val id = response.data["id"] ?: throw Exception("Missing id in response")
            Result.success(id)
        } else {
            Result.failure(Exception(response.error.toUserMessage()))
        }
    }

    /** Returns the PO's UUID ID string on success. */
    suspend fun sendPurchaseOrder(id: String): Result<String> = safeCall {
        val response = api.sendPurchaseOrder(id)
        if (response.success && response.data != null) {
            Result.success(response.data["id"] ?: id)
        } else {
            Result.failure(Exception(response.error.toUserMessage()))
        }
    }

    /** Returns the PO's UUID ID string on success. */
    suspend fun receiveGoods(id: String, request: GoodsReceiptRequest): Result<String> = safeCall {
        val response = api.receiveGoods(id, request)
        if (response.success && response.data != null) {
            Result.success(response.data["id"] ?: id)
        } else {
            Result.failure(Exception(response.error.toUserMessage()))
        }
    }

    private suspend fun <T> safeCall(block: suspend () -> Result<T>): Result<T> = try {
        block()
    } catch (_: SocketTimeoutException) {
        Result.failure(Exception("Request timed out. Please try again."))
    } catch (ex: Exception) {
        Timber.e(ex, "API Error in SupplierRepository")
        Result.failure(Exception(ex.message ?: "Unexpected error"))
    }
}
