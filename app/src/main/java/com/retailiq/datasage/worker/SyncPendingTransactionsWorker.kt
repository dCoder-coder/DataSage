package com.retailiq.datasage.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.retailiq.datasage.data.local.PendingTransactionDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncPendingTransactionsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dao: PendingTransactionDao
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val pending = dao.getByStatus("pending")
        pending.forEach { dao.updateStatus(it.id, "synced") }
        return Result.success()
    }
}
