package com.retailiq.datasage.ui.worker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SyncStatusBar(pending: Int, failed: Int, onRetry: (() -> Unit)? = null) {
    AnimatedVisibility(visible = pending > 0 || failed > 0) {
        val backgroundColor = if (failed > 0) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
        val message = when {
            failed > 0 && pending > 0 -> "$failed failed, $pending pending sync"
            failed > 0 -> "$failed sales failed to sync. Tap to retry"
            else -> "$pending sales pending sync"
        }

        val infiniteTransition = rememberInfiniteTransition(label = "sync")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "syncRotation"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .clickable(enabled = failed > 0 && onRetry != null) { onRetry?.invoke() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = if (failed > 0) Icons.Default.CloudOff else Icons.Default.Sync
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp).then(
                    if (pending > 0 && failed == 0) Modifier.rotate(rotation) else Modifier
                )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
