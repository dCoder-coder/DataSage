package com.retailiq.datasage.ui.worker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SyncStatusBar(pending: Int, failed: Int) {
    if (pending <= 0 && failed <= 0) return
    val message = if (failed > 0) "Sync failed for $failed sales" else "$pending sales pending sync"
    Text(
        text = message,
        modifier = Modifier.fillMaxWidth().background(if (failed > 0) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary).padding(8.dp),
        color = Color.White
    )
}
