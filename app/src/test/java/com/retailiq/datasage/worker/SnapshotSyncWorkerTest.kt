package com.retailiq.datasage.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.gson.Gson
import com.retailiq.datasage.core.TokenStore
import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.OfflineApiService
import com.retailiq.datasage.data.local.AnalyticsSnapshotDao
import com.retailiq.datasage.data.model.SnapshotDto
import com.retailiq.datasage.data.model.SnapshotResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
class SnapshotSyncWorkerTest {
    
    private lateinit var context: Context
    private lateinit var mockApi: OfflineApiService
    private lateinit var mockDao: AnalyticsSnapshotDao
    private lateinit var mockTokenStore: TokenStore
    private lateinit var gson: Gson

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockApi = mock(OfflineApiService::class.java)
        mockDao = mock(AnalyticsSnapshotDao::class.java)
        mockTokenStore = mock(TokenStore::class.java)
        gson = Gson()
    }

    private fun getWorker(): SnapshotSyncWorker {
        return TestListenableWorkerBuilder<SnapshotSyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return SnapshotSyncWorker(appContext, workerParameters, mockApi, mockDao, mockTokenStore, gson)
                }
            })
            .build()
    }

    @Test
    fun doWorkReturnsSuccessOnSuccessfulSync() = runBlocking {
        val mockResponse = SnapshotResponse(builtAt = "now", sizeBytes = 100, snapshot = SnapshotDto(kpis = null, builtAt = "now"))
        val mockApiResponse = ApiResponse(success = true, data = mockResponse, error = null, meta = null)
        
        `when`(mockApi.getSnapshot()).thenReturn(mockApiResponse)

        val worker = getWorker()
        val result = worker.doWork()
        
        assertEquals(Result.success(), result)
    }

    @Test
    fun doWorkReturnsRetryOnFailure() = runBlocking {
        `when`(mockApi.getSnapshot()).thenThrow(RuntimeException("Network error"))

        val worker = getWorker()
        val result = worker.doWork()
        
        assertEquals(Result.retry(), result)
    }
}
