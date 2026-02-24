package com.retailiq.datasage.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "pending_transactions")
data class PendingTransaction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val payloadJson: String,
    val status: String = "pending",
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
