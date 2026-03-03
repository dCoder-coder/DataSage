package com.retailiq.datasage.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.data.model.GstSlabDto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GstReportsScreen(
    onNavigateBack: () -> Unit,
    viewModel: GstReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val exportMsg by viewModel.exportMessage.collectAsState()
    val currentPeriod by viewModel.currentPeriod.collectAsState()
    
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(exportMsg) {
        exportMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearExportMessage()
        }
    }

    var showPeriodMenu by remember { mutableStateOf(false) }
    val availablePeriods = generateRecentPeriods()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GST Reports") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setPeriod(currentPeriod) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState is GstReportUiState.Success) {
                FloatingActionButton(onClick = { viewModel.exportGstr1(context) }) {
                    Icon(Icons.Default.Download, contentDescription = "Export GSTR-1")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period Selector
            Box {
                OutlinedButton(onClick = { showPeriodMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Period: $currentPeriod")
                }
                DropdownMenu(
                    expanded = showPeriodMenu,
                    onDismissRequest = { showPeriodMenu = false }
                ) {
                    availablePeriods.forEach { period ->
                        DropdownMenuItem(
                            text = { Text(period) },
                            onClick = {
                                viewModel.setPeriod(period)
                                showPeriodMenu = false
                            }
                        )
                    }
                }
            }

            when (val state = uiState) {
                is GstReportUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is GstReportUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is GstReportUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text("Summary", style = MaterialTheme.typography.titleLarge)
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SummaryCard("Taxable Amt", "₹${String.format("%.2f", state.summary.totalTaxable)}", Modifier.weight(1f))
                                SummaryCard("Total GST", "₹${String.format("%.2f", state.summary.totalCgst + state.summary.totalSgst + state.summary.totalIgst)}", Modifier.weight(1f))
                            }
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SummaryCard("CGST", "₹${String.format("%.2f", state.summary.totalCgst)}", Modifier.weight(1f))
                                SummaryCard("SGST", "₹${String.format("%.2f", state.summary.totalSgst)}", Modifier.weight(1f))
                                SummaryCard("IGST", "₹${String.format("%.2f", state.summary.totalIgst)}", Modifier.weight(1f))
                            }
                        }

                        item {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("Tax Slabs Breakdown", style = MaterialTheme.typography.titleLarge)
                        }

                        if (state.slabs.isEmpty()) {
                            item {
                                Text("No transactions in this period.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(state.slabs) { slab ->
                                SlabRow(slab)
                            }
                        }
                        
                        item {
                            Spacer(Modifier.height(80.dp)) // space for FAB
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SlabRow(slab: GstSlabDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("${slab.rate}% Slab", fontWeight = FontWeight.Bold)
                Text(
                    text = "Taxable: ₹${String.format("%.2f", slab.taxableValue)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "GST: ₹${String.format("%.2f", slab.taxAmount)}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

fun generateRecentPeriods(count: Int = 6): List<String> {
    val periods = mutableListOf<String>()
    val formatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val cal = Calendar.getInstance()
    for (i in 0 until count) {
        periods.add(formatter.format(cal.time))
        cal.add(Calendar.MONTH, -1)
    }
    return periods
}
