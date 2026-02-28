package com.retailiq.datasage.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapshotDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: AnalyticsSnapshotDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.analyticsSnapshotDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun upsertAndGetSnapshot() = runBlocking {
        val snapshot = AnalyticsSnapshot(
            storeId = "test_store",
            snapshotJson = "{\"kpis\":{}}",
            builtAt = "2026-01-01T00:00:00Z",
            downloadedAt = 123456789L
        )
        dao.upsertSnapshot(snapshot)

        val retrieved = dao.getSnapshot("test_store")
        assertNotNull(retrieved)
        assertEquals("test_store", retrieved?.storeId)
        assertEquals("{\"kpis\":{}}", retrieved?.snapshotJson)
        assertEquals("2026-01-01T00:00:00Z", retrieved?.builtAt)
        assertEquals(123456789L, retrieved?.downloadedAt)
    }
}
