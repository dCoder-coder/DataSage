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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.data.model.ReceiptTemplateRequest
import com.retailiq.datasage.ui.viewmodel.ReceiptsViewModel
import com.retailiq.datasage.ui.viewmodel.SaveUiState
import com.retailiq.datasage.ui.viewmodel.TemplateUiState
import com.retailiq.datasage.ui.worker.SyncStatusViewModel

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
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToSuppliers: () -> Unit = {},
    onNavigateToForecast: () -> Unit = {},
    onNavigateToNlpQuery: () -> Unit = {},
    onNavigateToGstReports: () -> Unit = {},
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

    val isOwner = userRole.equals("OWNER", ignoreCase = true)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── QUICK ACTIONS ──────────────────────────────────────────
            SectionHeader("Quick Actions")

            SettingsNavItem(
                icon = Icons.Default.People,
                label = "Customers",
                subtitle = "Manage customer profiles",
                onClick = onNavigateToCustomers
            )

            SettingsNavItem(
                icon = Icons.Default.LocalShipping,
                label = "Suppliers & Purchase Orders",
                subtitle = "Supplier management, POs",
                onClick = onNavigateToSuppliers
            )

            SettingsNavItem(
                icon = Icons.Default.Notifications,
                label = "Alerts",
                subtitle = "Inventory alerts & notifications",
                onClick = onNavigateToAlerts
            )

            // ── BUSINESS ───────────────────────────────────────────────
            if (isOwner) {
                SectionHeader("Business")

                SettingsNavItem(
                    icon = Icons.Default.Groups,
                    label = "Staff Performance",
                    subtitle = "Track staff sales & targets",
                    onClick = onNavigateToStaffPerformance
                )
                SettingsNavItem(
                    icon = Icons.Default.CardGiftcard,
                    label = "Loyalty Programme",
                    subtitle = "Points, rewards & redemption",
                    onClick = onNavigateToLoyaltySettings
                )
                SettingsNavItem(
                    icon = Icons.Default.PriceChange,
                    label = "Pricing Suggestions",
                    subtitle = "AI-powered pricing recommendations",
                    onClick = onNavigateToPricing
                )
                SettingsNavItem(
                    icon = Icons.Default.TrendingUp,
                    label = "Demand Forecast",
                    subtitle = "Stock demand predictions",
                    onClick = onNavigateToForecast
                )
                SettingsNavItem(
                    icon = Icons.Default.CalendarMonth,
                    label = "Events Calendar",
                    subtitle = "Promotions & seasonal events",
                    onClick = onNavigateToEvents
                )
                SettingsNavItem(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    label = "AI Query",
                    subtitle = "Ask questions about your data",
                    onClick = onNavigateToNlpQuery
                )
            }

            // ── REPORTS & COMPLIANCE ───────────────────────────────────
            if (isOwner) {
                SectionHeader("Reports & Compliance")

                SettingsNavItem(
                    icon = Icons.Default.Assessment,
                    label = "GST Configuration",
                    subtitle = "GSTIN, tax rates & HSN codes",
                    onClick = onNavigateToGstSettings
                )
                SettingsNavItem(
                    icon = Icons.Default.Receipt,
                    label = "GST Reports & GSTR-1",
                    subtitle = "Tax filing reports",
                    onClick = onNavigateToGstReports
                )
            }

            // ── INTEGRATIONS ───────────────────────────────────────────
            if (isOwner) {
                SectionHeader("Integrations")

                SettingsNavItem(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    label = "WhatsApp Alerts",
                    subtitle = "Configure WhatsApp notifications",
                    onClick = onNavigateToWhatsAppSettings
                )
            }

            // ── SYSTEM ─────────────────────────────────────────────────
            SectionHeader("System")

            // Sync status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
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

            // Receipt & Printer Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
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

            // App info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("App Info", fontWeight = FontWeight.SemiBold)
                    }
                    Text("DataSage v1.0.0", style = MaterialTheme.typography.bodySmall)
                    Text("RetailIQ Platform", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Logout
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                Text("  Logout")
            }

            Spacer(Modifier.height(16.dp))
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

@Composable
private fun SectionHeader(title: String) {
    Spacer(Modifier.height(4.dp))
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SettingsNavItem(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
