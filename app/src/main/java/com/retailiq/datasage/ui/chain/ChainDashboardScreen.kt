package com.retailiq.datasage.ui.chain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.data.model.ChainDashboardDto
import com.retailiq.datasage.data.model.StoreRevenueDto
import com.retailiq.datasage.data.model.TransferSuggestionDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChainDashboardScreen(
    viewModel: ChainDashboardViewModel = hiltViewModel(),
    onNavigateToCompare: () -> Unit = {},
    onNavigateToTransfers: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chain Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 8.dp))
                    } else {
                        IconButton(onClick = { viewModel.loadDashboard(fromRefresh = true) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                    TextButton(onClick = onNavigateToCompare) { Text("Compare Stores") }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is ChainDashboardUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is ChainDashboardUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadDashboard() }) { Text("Retry") }
                    }
                }
                is ChainDashboardUiState.Loaded -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { ChainKpiRow(state.dashboard) }
                        item { StoreRevenueBarChart(state.dashboard.store_revenues) }
                        item {
                            Text("Transfer Suggestions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        if (state.dashboard.transfer_suggestions.isEmpty()) {
                            item {
                                Text(
                                    "No transfer opportunities detected this week.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        } else {
                            items(state.dashboard.transfer_suggestions, key = { it.id }) { t ->
                                TransferSuggestionRow(t, onMarkDone = { viewModel.markTransferDone(t.id) })
                            }
                        }
                        item {
                            OutlinedButton(
                                onClick = onNavigateToTransfers,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("View All Transfers") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChainKpiRow(dashboard: ChainDashboardDto) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KpiCard("Chain Revenue", "₹${"%,.0f".format(dashboard.total_revenue_today)}", MaterialTheme.colorScheme.surface, Modifier.weight(1f))
        KpiCard("Alerts", dashboard.total_open_alerts.toString(), MaterialTheme.colorScheme.surface, Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val best = dashboard.best_store
        val worst = dashboard.worst_store
        KpiCard("Best Store", best?.store_name ?: "-", Color(0xFFE8F5E9), Modifier.weight(1f),
            sub = if (best != null) "₹${"%,.0f".format(best.revenue)}" else "")
        KpiCard("Worst Store", worst?.store_name ?: "-", Color(0xFFFFF3E0), Modifier.weight(1f),
            sub = if (worst != null) "₹${"%,.0f".format(worst.revenue)}" else "")
    }
}

@Composable
private fun KpiCard(label: String, value: String, bg: Color, modifier: Modifier = Modifier, sub: String = "") {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Box(Modifier.background(bg).padding(12.dp)) {
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun StoreRevenueBarChart(revenues: List<StoreRevenueDto>) {
    if (revenues.isEmpty()) return
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Revenue by Store", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            val max = revenues.maxOf { it.revenue }.coerceAtLeast(1.0)
            revenues.forEach { store ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(store.store_name, modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    Spacer(Modifier.width(4.dp))
                    LinearProgressIndicator(
                        progress = { (store.revenue / max).toFloat() },
                        modifier = Modifier.weight(1f).height(18.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("₹${"%,.0f".format(store.revenue)}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(60.dp))
                }
            }
        }
    }
}

@Composable
private fun TransferSuggestionRow(transfer: TransferSuggestionDto, onMarkDone: () -> Unit) {
    Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Send ${transfer.suggested_qty}x ${transfer.product_name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("${transfer.from_store_name} → ${transfer.to_store_name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(transfer.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = onMarkDone) { Text("Mark Done", fontSize = 12.sp) }
        }
    }
}
