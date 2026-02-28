package com.retailiq.datasage.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PendingTransaction::class, AnalyticsSnapshot::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingTransactionDao(): PendingTransactionDao
    abstract fun analyticsSnapshotDao(): AnalyticsSnapshotDao
}
