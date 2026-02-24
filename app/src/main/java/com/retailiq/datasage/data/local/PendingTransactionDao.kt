package com.retailiq.datasage.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingTransactionDao {
    @Insert
    suspend fun insert(item: PendingTransaction)

    @Query("SELECT * FROM pending_transactions WHERE status = :status ORDER BY createdAt ASC")
    suspend fun getByStatus(status: String): List<PendingTransaction>

    @Query("UPDATE pending_transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE pending_transactions SET status = :status, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun markFailed(id: Long, status: String = "failed")

    @Query("SELECT COUNT(*) FROM pending_transactions WHERE status = :status")
    suspend fun countByStatus(status: String): Int
}
