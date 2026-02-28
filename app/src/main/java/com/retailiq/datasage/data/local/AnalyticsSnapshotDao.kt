package com.retailiq.datasage.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AnalyticsSnapshotDao {
    @Query("SELECT * FROM analytics_snapshot WHERE storeId = :storeId LIMIT 1")
    suspend fun getSnapshot(storeId: String): AnalyticsSnapshot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(snapshot: AnalyticsSnapshot)
}
