package com.retailiq.datasage.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_transactions")
data class PendingTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: String,
    val payloadJson: String,
    val status: String,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
