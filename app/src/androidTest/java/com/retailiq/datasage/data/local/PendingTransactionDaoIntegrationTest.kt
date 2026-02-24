package com.retailiq.datasage.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PendingTransactionDaoIntegrationTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: PendingTransactionDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.pendingTransactionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertQueryAndUpdateStatus_workAsExpected() {
        val p1 = PendingTransaction(transactionId = "tx-1", payloadJson = "{\"order\":1}", status = "pending")
        val p2 = PendingTransaction(transactionId = "tx-2", payloadJson = "{\"order\":2}", status = "pending")

        kotlinx.coroutines.runBlocking {
            dao.insert(p1)
            dao.insert(p2)

            val pending = dao.getByStatus("pending")
            assertEquals(2, pending.size)

            dao.updateStatus(pending.first().id, "synced")
            assertEquals(1, dao.getByStatus("pending").size)
            assertEquals(1, dao.getByStatus("synced").size)
        }
    }
}
