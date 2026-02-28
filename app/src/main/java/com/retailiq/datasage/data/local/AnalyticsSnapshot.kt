package com.retailiq.datasage.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analytics_snapshot")
data class AnalyticsSnapshot(
    @PrimaryKey
    val storeId: String,
    val snapshotJson: String,
    val builtAt: String,
    val downloadedAt: Long
)
