package com.retailiq.datasage.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.retailiq.datasage.data.api.DailySummary
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.TransactionApiService
import com.retailiq.datasage.data.api.TransactionSummary
import com.retailiq.datasage.data.api.toUserMessage
import com.retailiq.datasage.data.local.PendingTransaction
import com.retailiq.datasage.data.local.PendingTransactionDao
import com.retailiq.datasage.worker.SyncTransactionsWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val transactionApi: TransactionApiService,
    private val pendingDao: PendingTransactionDao,
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    /** Save sale offline first, then sync immediately + enqueue background worker as backup */
    suspend fun createSaleOffline(payload: Map<String, Any>): String {
        val tx = PendingTransaction(payloadJson = gson.toJson(payload))
        pendingDao.insert(tx)
        Timber.d("Sale saved offline: %s", tx.id)
        enqueueSync()
        // Try immediate sync so we don't rely solely on WorkManager
        trySyncNow()
        return tx.id
    }

    /** Fetch today's daily summary from API */
    suspend fun getDailySummary(date: String? = null): NetworkResult<DailySummary> = safeCall {
        val response = transactionApi.getDailySummary(date)
        if (response.success && response.data != null) {
            NetworkResult.Success(response.data)
        } else {
            NetworkResult.Error(422, response.error.toUserMessage())
        }
    }

    /** Fetch transaction list with pagination */
    suspend fun listTransactions(
        page: Int = 1,
        pageSize: Int = 50,
        startDate: String? = null,
        endDate: String? = null,
        paymentMode: String? = null
    ): NetworkResult<List<TransactionSummary>> = safeCall {
        val response = transactionApi.listTransactions(page, pageSize, startDate, endDate, paymentMode)
        if (response.success && response.data != null) {
            NetworkResult.Success(response.data)
        } else {
            NetworkResult.Error(422, response.error.toUserMessage())
        }
    }

    /** Observable pending count */
    fun pendingCountFlow(): Flow<Int> = pendingDao.countByStatusFlow("pending")

    /** Observable failed count */
    fun failedCountFlow(): Flow<Int> = pendingDao.countByStatusFlow("failed")

    /** Observable all pending transactions */
    fun observeAll(): Flow<List<PendingTransaction>> = pendingDao.observeAll()

    /** Retry all failed transactions */
    suspend fun retryFailed() {
        val failed = pendingDao.getByStatus("failed")
        failed.forEach { pendingDao.updateStatus(it.id, "pending") }
        if (failed.isNotEmpty()) {
            Timber.d("Retrying %d failed transactions", failed.size)
            enqueueSync()
            trySyncNow()
        }
    }

    /** Direct immediate sync — bypasses WorkManager to sync right now */
    suspend fun trySyncNow() {
        try {
            // Rescue any failed transactions first
            pendingDao.resetAllFailedToPending()
            val pending = pendingDao.getByStatus("pending")
            if (pending.isEmpty()) return
            Timber.d("trySyncNow: %d pending transactions", pending.size)

            val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
            val payloads = pending.mapNotNull { tx ->
                try {
                    val map = gson.fromJson<Map<String, Any>>(tx.payloadJson, mapType)
                        .toMutableMap()
                    map.remove("loyalty_points_redeemed")
                    map.remove("loyalty_discount_amount")
                    // Fix lowercase payment_mode from old payloads (backend requires CASH, UPI, etc.)
                    val mode = map["payment_mode"]
                    if (mode is String) map["payment_mode"] = mode.uppercase()
                    map
                } catch (e: Exception) {
                    Timber.e(e, "trySyncNow: bad payload %s", tx.id)
                    null
                }
            }
            if (payloads.isEmpty()) return

            val response = transactionApi.createTransactionBatch(
                com.retailiq.datasage.data.api.BatchTransactionsRequest(payloads)
            )
            val code = response.code()
            Timber.d("trySyncNow response: %d", code)

            when (code) {
                200, 201, 409 -> {
                    pending.forEach { pendingDao.updateStatus(it.id, "synced") }
                    pendingDao.deleteSynced()
                    Timber.d("trySyncNow: synced %d transactions", pending.size)
                }
                else -> {
                    val errorBody = response.errorBody()?.string()
                    Timber.w("trySyncNow failed: code=%d body=%s", code, errorBody)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "trySyncNow failed (will retry via WorkManager)")
        }
    }

    private fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<SyncTransactionsWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "sync_now",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private suspend fun <T> safeCall(block: suspend () -> NetworkResult<T>): NetworkResult<T> = try {
        block()
    } catch (_: SocketTimeoutException) {
        NetworkResult.Error(408, "Request timed out")
    } catch (e: Exception) {
        Timber.e(e, "Transaction API error")
        NetworkResult.Error(500, e.message ?: "Unexpected error")
    }
}
