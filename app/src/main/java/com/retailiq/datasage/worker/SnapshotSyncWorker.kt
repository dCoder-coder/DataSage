package com.retailiq.datasage.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.retailiq.datasage.core.TokenStore
import com.retailiq.datasage.data.api.OfflineApiService
import com.retailiq.datasage.data.local.AnalyticsSnapshot
import com.retailiq.datasage.data.local.AnalyticsSnapshotDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class SnapshotSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val offlineApi: OfflineApiService,
    private val dao: AnalyticsSnapshotDao,
    private val tokenStore: TokenStore,
    private val gson: Gson
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val response = offlineApi.getSnapshot()
            if (response.success && response.data?.snapshot != null) {
                val snapshotDto = response.data.snapshot
                val builtAt = response.data.builtAt ?: "Unknown"
                val json = gson.toJson(snapshotDto)
                
                // We use "my_store" as the default store pk since data is localized to the
                // currently authenticated user on the mobile client.
                val storeId = "my_store"
                
                val entity = AnalyticsSnapshot(
                    storeId = storeId,
                    snapshotJson = json,
                    builtAt = builtAt,
                    downloadedAt = System.currentTimeMillis()
                )
                dao.upsertSnapshot(entity)
                Timber.d("SnapshotSyncWorker: Successfully synced offline snapshot")
                Result.success()
            } else {
                Timber.e("SnapshotSyncWorker: Failed to fetch snapshot - ${response.error?.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "SnapshotSyncWorker: Exception during sync")
            Result.retry()
        }
    }
}
