package com.retailiq.datasage.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.retailiq.datasage.data.api.BatchTransactionsRequest
import com.retailiq.datasage.data.api.TransactionApiService
import com.retailiq.datasage.data.local.PendingTransactionDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncTransactionsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dao: PendingTransactionDao,
    private val transactionApiService: TransactionApiService
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val pending = dao.getByStatus("pending")
        if (pending.isEmpty()) return Result.success()

        val chunks = pending.chunked(500)
        chunks.forEach { chunk ->
            val payload = chunk.map { Gson().fromJson(it.payloadJson, Map::class.java) as Map<String, Any> }
            try {
                val response = transactionApiService.createTransactionBatch(BatchTransactionsRequest(payload))
                when (response.code()) {
                    201, 409 -> chunk.forEach { dao.updateStatus(it.id, "synced") }
                    else -> chunk.forEach {
                        if (it.retryCount >= 2) dao.updateStatus(it.id, "failed") else dao.markFailed(it.id)
                    }
                }
            } catch (_: Exception) {
                chunk.forEach {
                    if (it.retryCount >= 2) dao.updateStatus(it.id, "failed") else dao.markFailed(it.id)
                }
            }
        }
        return Result.success()
    }
}
