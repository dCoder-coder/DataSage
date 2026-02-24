package com.retailiq.datasage.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PendingTransaction)

    @Query("SELECT * FROM pending_transactions WHERE status = :status ORDER BY createdAt ASC")
    suspend fun getByStatus(status: String): List<PendingTransaction>

    @Query("UPDATE pending_transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE pending_transactions SET status = 'pending', retryCount = retryCount + 1 WHERE id = :id")
    suspend fun markRetry(id: String)

    @Query("UPDATE pending_transactions SET status = 'failed' WHERE id = :id")
    suspend fun markFailed(id: String)

    @Query("SELECT COUNT(*) FROM pending_transactions WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM pending_transactions WHERE status = :status")
    fun countByStatusFlow(status: String): Flow<Int>

    @Query("SELECT * FROM pending_transactions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PendingTransaction>>

    @Query("DELETE FROM pending_transactions WHERE status = 'synced'")
    suspend fun deleteSynced()
}
