package com.retailiq.datasage.ui.staff

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.data.model.DailyTargetRequest
import com.retailiq.datasage.data.model.StaffPerformanceSummaryDto
import com.retailiq.datasage.ui.components.DateRevenuePair
import com.retailiq.datasage.ui.components.RevenueLineChart
import com.retailiq.datasage.ui.navigation.RoleGuard
import com.retailiq.datasage.ui.components.EmptyStateView
import com.retailiq.datasage.ui.components.ShimmerLoadingList
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffPerformanceScreen(
    userRole: String,
    onNavigateBack: () -> Unit,
    viewModel: StaffViewModel = hiltViewModel()
) {
    RoleGuard(role = userRole, requiredRole = "OWNER") {
        var selectedDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
        var showTargetSheet by remember { mutableStateOf(false) }
        val performanceState by viewModel.performanceState.collectAsState()
        val context = LocalContext.current

        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                selectedDate = newDate
                viewModel.fetchPerformance(newDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        LaunchedEffect(selectedDate) {
            viewModel.fetchPerformance(selectedDate)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Staff Performance") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(selectedDate)
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showTargetSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Set Targets")
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (val state = performanceState) {
                    is StaffPerformanceState.Loading -> {
                        ShimmerLoadingList()
                    }
                    is StaffPerformanceState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    is StaffPerformanceState.Success -> {
                        if (state.performanceList.isEmpty()) {
                            EmptyStateView(
                                message = "No performance data available for $selectedDate"
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.performanceList) { item ->
                                    StaffPerformanceCard(item)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showTargetSheet) {
            SetTargetDialog(
                currentDate = selectedDate,
                staffMembers = if (performanceState is StaffPerformanceState.Success) 
                                (performanceState as StaffPerformanceState.Success).performanceList 
                               else emptyList(),
                onDismiss = { showTargetSheet = false },
                onSave = { request ->
                    viewModel.setTarget(request) { success ->
                        if (success) showTargetSheet = false
                    }
                }
            )
        }
    }
}

@Composable
fun StaffPerformanceCard(summary: StaffPerformanceSummaryDto) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar Initials
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = summary.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
                    Text(text = initials, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(summary.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                            Text("${summary.todayTransactionCount} tx", modifier = Modifier.padding(horizontal = 4.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        
                        val discountColor = when {
                            summary.avgDiscountPct > 20 -> Color.Red
                            summary.avgDiscountPct > 10 -> Color(0xFFFFA500) // Amber
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = "Avg Disc: ${String.format("%.1f", summary.avgDiscountPct)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = discountColor
                        )
                    }
                }
                
                Text(
                    text = "₹${String.format("%,.0f", summary.todayRevenue)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Revenue Progress Bar
            summary.targetRevenue?.let { target ->
                Spacer(modifier = Modifier.height(12.dp))
                val progress = (summary.todayRevenue / target).coerceIn(0.0, 1.0).toFloat()
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Target: ₹${String.format("%,.0f", target)}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("${String.format("%.1f", progress * 100)}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF009688), // Teal fill
                    trackColor = Color.LightGray
                )
            }

            // Expandable Chart Area (Mock 7-day data for demo purposes if not provided by DTO)
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Divider(modifier = Modifier.padding(bottom = 16.dp))
                    Text("7-Day Trend", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                    
                    // Note: Mock data since the endpoint might not return 7 days historical list natively
                    // In a real scenario, this would be fetched from a specific historical endpoint
                    val mockData = listOf(
                        DateRevenuePair("Day 1", summary.todayRevenue * 0.8),
                        DateRevenuePair("Day 2", summary.todayRevenue * 0.9),
                        DateRevenuePair("Day 3", summary.todayRevenue * 0.7),
                        DateRevenuePair("Day 4", summary.todayRevenue * 1.1),
                        DateRevenuePair("Day 5", summary.todayRevenue * 1.2),
                        DateRevenuePair("Day 6", summary.todayRevenue * 0.95),
                        DateRevenuePair("Today", summary.todayRevenue)
                    )
                    
                    Box(modifier = Modifier.height(180.dp).fillMaxWidth()) {
                        RevenueLineChart(data = mockData, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetTargetDialog(
    currentDate: String,
    staffMembers: List<StaffPerformanceSummaryDto>,
    onDismiss: () -> Unit,
    onSave: (DailyTargetRequest) -> Unit
) {
    var selectedUserId by remember { mutableStateOf(staffMembers.firstOrNull()?.userId ?: "") }
    var selectedUserName by remember { mutableStateOf(staffMembers.firstOrNull()?.name ?: "Select Staff") }
    var revenueTarget by remember { mutableStateOf("") }
    var countTarget by remember { mutableStateOf("") }
    var staffDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Set Daily Target", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Date: $currentDate", style = MaterialTheme.typography.bodyMedium)

                ExposedDropdownMenuBox(
                    expanded = staffDropdownExpanded,
                    onExpandedChange = { staffDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedUserName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Staff Member") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = staffDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = staffDropdownExpanded,
                        onDismissRequest = { staffDropdownExpanded = false }
                    ) {
                        staffMembers.forEach { staff ->
                            DropdownMenuItem(
                                text = { Text(staff.name) },
                                onClick = {
                                    selectedUserId = staff.userId
                                    selectedUserName = staff.name
                                    staffDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = revenueTarget,
                    onValueChange = { revenueTarget = it },
                    label = { Text("Revenue Target (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = countTarget,
                    onValueChange = { countTarget = it },
                    label = { Text("Transaction Count Target") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val req = DailyTargetRequest(
                                targetDate = currentDate,
                                revenueTarget = revenueTarget.toDoubleOrNull() ?: 0.0,
                                countTarget = countTarget.toIntOrNull() ?: 0,
                                userId = selectedUserId
                            )
                            onSave(req)
                        },
                        enabled = selectedUserId.isNotEmpty() && revenueTarget.isNotEmpty() && countTarget.isNotEmpty()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
