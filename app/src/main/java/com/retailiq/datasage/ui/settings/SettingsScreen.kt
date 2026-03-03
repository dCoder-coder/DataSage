package com.retailiq.datasage.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.ui.worker.SyncStatusViewModel

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material.icons.filled.Print
import androidx.compose.runtime.LaunchedEffect
import com.retailiq.datasage.data.model.ReceiptTemplateRequest
import com.retailiq.datasage.ui.viewmodel.ReceiptsViewModel
import com.retailiq.datasage.ui.viewmodel.SaveUiState
import com.retailiq.datasage.ui.viewmodel.TemplateUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userRole: String = "STAFF",
    syncViewModel: SyncStatusViewModel = hiltViewModel(),
    receiptsViewModel: ReceiptsViewModel = hiltViewModel(),
    onNavigateToStaffPerformance: () -> Unit = {},
    onNavigateToLoyaltySettings: () -> Unit = {},
    onNavigateToGstSettings: () -> Unit = {},
    onNavigateToWhatsAppSettings: () -> Unit = {},
    onNavigateToPricing: () -> Unit = {},
    onNavigateToEvents: () -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val pending by syncViewModel.pending.collectAsState()
    val failed by syncViewModel.failed.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    val templateState by receiptsViewModel.templateState.collectAsState()
    val templateSaveState by receiptsViewModel.templateSaveState.collectAsState()

    var headerText by remember { mutableStateOf("") }
    var footerText by remember { mutableStateOf("") }
    var showGstin by remember { mutableStateOf(false) }
    var paperWidth by remember { mutableStateOf(80) }

    LaunchedEffect(Unit) {
        receiptsViewModel.loadTemplate()
    }

    LaunchedEffect(templateState) {
        if (templateState is TemplateUiState.Success) {
            val tmpl = (templateState as TemplateUiState.Success).template
            headerText = tmpl.header.orEmpty()
            footerText = tmpl.footer.orEmpty()
            showGstin = tmpl.showGstin
            paperWidth = tmpl.paperWidth
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sync status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync Status", modifier = Modifier.size(20.dp))
                        Text("Sync Status", fontWeight = FontWeight.SemiBold)
                    }
                    Text("Pending: $pending")
                    Text("Failed: $failed")
                    if (failed > 0) {
                        OutlinedButton(onClick = { syncViewModel.retryFailed() }) {
                            Text("Retry Failed")
                        }
                    }
                }
            }

            // App info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, contentDescription = "App Info", modifier = Modifier.size(20.dp))
                        Text("App Info", fontWeight = FontWeight.SemiBold)
                    }
                    Text("DataSage v1.0.0", style = MaterialTheme.typography.bodySmall)
                    Text("RetailIQ Platform", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Receipt & Printer Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Print, contentDescription = "Printer Settings", modifier = Modifier.size(20.dp))
                        Text("Receipt & Printer", fontWeight = FontWeight.SemiBold)
                    }

                    if (templateState is TemplateUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp))
                    } else {
                        OutlinedTextField(
                            value = headerText,
                            onValueChange = { headerText = it },
                            label = { Text("Receipt Header") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = footerText,
                            onValueChange = { footerText = it },
                            label = { Text("Receipt Footer") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Show GSTIN")
                            Switch(checked = showGstin, onCheckedChange = { showGstin = it })
                        }

                        Text("Paper Width")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = paperWidth == 58, onClick = { paperWidth = 58 }, label = { Text("58mm") })
                            FilterChip(selected = paperWidth == 80, onClick = { paperWidth = 80 }, label = { Text("80mm") })
                        }

                        Button(
                            onClick = {
                                receiptsViewModel.saveTemplate(
                                    ReceiptTemplateRequest(headerText, footerText, showGstin, paperWidth)
                                )
                            },
                            modifier = Modifier.align(Alignment.End),
                            enabled = templateSaveState != SaveUiState.Saving
                        ) {
                            if (templateSaveState == SaveUiState.Saving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text(if (templateSaveState == SaveUiState.Saved) "Saved ✓" else "Save")
                            }
                        }
                        
                        if (templateSaveState is SaveUiState.Error) {
                            Text(
                                (templateSaveState as SaveUiState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            if (userRole.equals("OWNER", ignoreCase = true)) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToLoyaltySettings() },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, contentDescription = "Loyalty Programme", modifier = Modifier.size(20.dp))
                            Text("Loyalty Programme", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToStaffPerformance() },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, contentDescription = "Staff Management", modifier = Modifier.size(20.dp))
                            Text("Staff Management", fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Navigate", modifier = Modifier.size(20.dp))
                    }
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToGstSettings() },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, contentDescription = "GST Configuration", modifier = Modifier.size(20.dp))
                            Text("GST Configuration", fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Navigate", modifier = Modifier.size(20.dp))
                    }
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToWhatsAppSettings() },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, contentDescription = "WhatsApp Setup", modifier = Modifier.size(20.dp))
                            Text("WhatsApp Setup", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToPricing() },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.PriceChange, contentDescription = "Pricing Suggestions", modifier = Modifier.size(20.dp))
                            Text("Pricing Suggestions", fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Navigate", modifier = Modifier.size(20.dp))
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToEvents() },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.DateRange, contentDescription = "Events Calendar", modifier = Modifier.size(20.dp))
                            Text("Events Calendar", fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Navigate", modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Alerts
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToAlerts() },
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Notifications, contentDescription = "Alerts", modifier = Modifier.size(20.dp))
                        Text("Alerts", fontWeight = FontWeight.SemiBold)
                    }
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Navigate", modifier = Modifier.size(20.dp))
                }
            }

            // Logout
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                Text("  Logout")
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = {
                val warning = if (pending > 0) "\n\n⚠️ You have $pending pending transactions that haven't synced yet." else ""
                Text("Are you sure you want to logout?$warning")
            },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; onLogout() }) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }
}
