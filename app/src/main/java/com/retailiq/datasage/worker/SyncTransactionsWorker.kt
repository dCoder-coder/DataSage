package com.retailiq.datasage.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.retailiq.datasage.data.api.BatchTransactionsRequest
import com.retailiq.datasage.data.api.TransactionApiService
import com.retailiq.datasage.data.local.PendingTransactionDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class SyncTransactionsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dao: PendingTransactionDao,
    private val transactionApi: TransactionApiService
) : CoroutineWorker(appContext, params) {

    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, Any>>() {}.type

    override suspend fun doWork(): Result {
        Timber.d("SyncTransactionsWorker started")
        val pending = dao.getByStatus("pending")
        if (pending.isEmpty()) {
            Timber.d("No pending transactions to sync")
            return Result.success()
        }

        Timber.d("Found %d pending transactions", pending.size)
        val chunks = pending.chunked(BATCH_SIZE)

        var allSucceeded = true

        for (chunk in chunks) {
            val payloads = chunk.mapNotNull { tx ->
                try {
                    gson.fromJson<Map<String, Any>>(tx.payloadJson, mapType)
                } catch (e: Exception) {
                    Timber.e(e, "Invalid JSON payload for transaction %s", tx.id)
                    dao.markFailed(tx.id)
                    null
                }
            }

            if (payloads.isEmpty()) continue

            try {
                val response = transactionApi.createTransactionBatch(
                    BatchTransactionsRequest(payloads)
                )
                val code = response.code()
                Timber.d("Batch sync response code: %d", code)

                when (code) {
                    200, 201, 409 -> {
                        chunk.forEach { tx -> dao.updateStatus(tx.id, "synced") }
                        Timber.d("Batch of %d transactions synced", chunk.size)
                    }
                    else -> {
                        Timber.w("Batch sync failed with code %d", code)
                        handleChunkFailure(chunk)
                        allSucceeded = false
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Network error during batch sync")
                handleChunkFailure(chunk)
                allSucceeded = false
            }
        }

        // Clean up old synced transactions
        dao.deleteSynced()

        return if (allSucceeded) Result.success() else Result.retry()
    }

    private suspend fun handleChunkFailure(
        chunk: List<com.retailiq.datasage.data.local.PendingTransaction>
    ) {
        chunk.forEach { tx ->
            if (tx.retryCount >= MAX_RETRIES) {
                Timber.w("Transaction %s exceeded max retries, marking failed", tx.id)
                dao.markFailed(tx.id)
            } else {
                dao.markRetry(tx.id)
            }
        }
    }

    companion object {
        const val BATCH_SIZE = 500
        const val MAX_RETRIES = 5
        const val WORK_NAME = "sync_transactions"
    }
}
