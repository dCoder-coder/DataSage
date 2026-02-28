package com.retailiq.datasage.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.data.model.StaffSessionDto
import kotlinx.coroutines.delay

@Composable
fun StaffSessionBanner(
    modifier: Modifier = Modifier,
    viewModel: StaffViewModel = hiltViewModel()
) {
    val sessionState by viewModel.sessionState.collectAsState()
    var showSummaryDialog by remember { mutableStateOf<StaffSessionDto?>(null) }

    LaunchedEffect(sessionState) {
        if (sessionState is StaffSessionState.Ended) {
            showSummaryDialog = (sessionState as StaffSessionState.Ended).session
            viewModel.resetSessionState()
        }
    }

    LaunchedEffect(sessionState) {
        if (sessionState is StaffSessionState.Active) {
            while (true) {
                delay(60_000L) // 60 seconds
                viewModel.updateSessionDuration()
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (sessionState is StaffSessionState.Active) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (sessionState) {
                is StaffSessionState.Active -> {
                    val activeState = sessionState as StaffSessionState.Active
                    Column {
                        Text(
                            text = "Session Active",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = activeState.durationFormatted,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Button(
                        onClick = { viewModel.endSession() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("End Session")
                    }
                }
                is StaffSessionState.Loading -> {
                    Text("Updating session...")
                }
                else -> {
                    Text(
                        text = "No Active Session",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { viewModel.startSession() }) {
                        Text("Start Session")
                    }
                }
            }
        }
    }

    showSummaryDialog?.let { session ->
        AlertDialog(
            onDismissRequest = { showSummaryDialog = null },
            title = { Text("Session Ended") },
            text = {
                Text(
                    "You recorded ${session.transactionsRecorded} transactions " +
                    "totalling ₹${String.format("%,.2f", session.totalAmount)} in this session."
                )
            },
            confirmButton = {
                TextButton(onClick = { showSummaryDialog = null }) {
                    Text("OK")
                }
            }
        )
    }
}
