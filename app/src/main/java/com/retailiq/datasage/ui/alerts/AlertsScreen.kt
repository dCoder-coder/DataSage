package com.retailiq.datasage.ui.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel = hiltViewModel(),
    onNavigateToCreatePo: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val whatsappEnabled by viewModel.whatsappEnabled.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Alerts") },
                actions = {
                    IconButton(onClick = { viewModel.loadAlerts() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is AlertsUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AlertsUiState.Loaded -> {
                if (state.alerts.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("No alerts")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.alerts) { alert ->
                            AlertCard(
                                alert = alert,
                                isWhatsappEnabled = whatsappEnabled,
                                onCreatePo = { 
                                    val prodId = alert.metadata?.get("product_id") as? Int ?: alert.metadata?.get("product_id")?.toString()?.toDoubleOrNull()?.toInt()
                                    if (prodId != null) onNavigateToCreatePo(prodId)
                                    else onNavigateToCreatePo(-1)
                                },
                                onNotifyWhatsapp = {
                                    val alertId = it.toIntOrNull() ?: 1
                                    viewModel.sendWhatsAppAlert(alertId)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Message queued")
                                    }
                                }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
            is AlertsUiState.Error -> {
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.message)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadAlerts() }) { Text("Retry") }
                }
            }
        }
    }
}

@Composable
private fun AlertCard(
    alert: AlertItem,
    isWhatsappEnabled: Boolean = false,
    onCreatePo: () -> Unit = {},
    onNotifyWhatsapp: (String) -> Unit = {}
) {
    val severityColor = when (alert.severity.lowercase()) {
        "critical" -> Color(0xFFD32F2F)
        "high" -> Color(0xFFFF9800)
        "medium" -> Color(0xFFFFC107)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    modifier = Modifier.size(10.dp),
                    shape = CircleShape,
                    color = severityColor
                ) {}
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        alert.type.replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(alert.message, style = MaterialTheme.typography.bodySmall)
                    Text(
                        alert.severity.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = severityColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Render action chip if po_action flag is true
            val isPoAction = alert.metadata?.get("po_action") == true || alert.metadata?.get("po_action") == "true"
            if (isPoAction || isWhatsappEnabled) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isPoAction) {
                        AssistChip(
                            onClick = onCreatePo,
                            label = { Text("Create PO") },
                            leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.primary, leadingIconContentColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                    if (isWhatsappEnabled) {
                        AssistChip(
                            onClick = { onNotifyWhatsapp(alert.id) },
                            label = { Text("Notify via WhatsApp") },
                            colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF25D366), leadingIconContentColor = Color(0xFF25D366))
                        )
                    }
                }
            }
        }
    }
}
