package com.retailiq.datasage.ui.purchaseorder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.retailiq.datasage.data.model.supplier.PurchaseOrderDto
import com.retailiq.datasage.ui.viewmodel.PoListUiState
import com.retailiq.datasage.ui.viewmodel.PurchaseOrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseOrderListScreen(
    supplierId: String? = null,
    viewModel: PurchaseOrderViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onCreatePo: (String?) -> Unit,
    onNavigateToReceive: (String) -> Unit
) {
    val listState by viewModel.listState.collectAsState()
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    val statuses = listOf("ALL", "DRAFT", "SENT", "PARTIAL", "FULFILLED", "CANCELLED")

    val snackbarHostState = remember { SnackbarHostState() }
    val actionState by viewModel.actionState.collectAsState()

    var showPhoneDialogForPo by remember { mutableStateOf<String?>(null) }
    var fetchedPhone by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(actionState) {
        if (actionState is com.retailiq.datasage.ui.viewmodel.PoActionUiState.Success) {
            snackbarHostState.showSnackbar((actionState as com.retailiq.datasage.ui.viewmodel.PoActionUiState.Success).message)
            viewModel.resetActionState()
        } else if (actionState is com.retailiq.datasage.ui.viewmodel.PoActionUiState.Error) {
            snackbarHostState.showSnackbar("Error: ${(actionState as com.retailiq.datasage.ui.viewmodel.PoActionUiState.Error).message}")
            viewModel.resetActionState()
        }
    }

    LaunchedEffect(supplierId, selectedStatus) {
        viewModel.loadPurchaseOrders(supplierId) // Backend doesn't support status filter out of box, so we filter locally
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (supplierId != null) "Supplier Orders" else "Purchase Orders") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onCreatePo(supplierId) }) {
                Icon(Icons.Default.Add, "Create PO")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Filter Chips
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statuses.forEach { status ->
                    FilterChip(
                        selected = (status == "ALL" && selectedStatus == null) || status == selectedStatus,
                        onClick = { selectedStatus = if (status == "ALL") null else status },
                        label = { Text(status) }
                    )
                }
            }

            when (val state = listState) {
                is PoListUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is PoListUiState.Error -> {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadPurchaseOrders(supplierId) }) { Text("Retry") }
                    }
                }
                is PoListUiState.Loaded -> {
                    val filteredList = if (selectedStatus == null) state.orders else state.orders.filter { it.status == selectedStatus }
                    if (filteredList.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No purchase orders found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filteredList) { po ->
                                PurchaseOrderCard(
                                    po = po,
                                    onClick = { if (po.status == "SENT" || po.status == "PARTIAL") onNavigateToReceive(po.id) },
                                    onSendWhatsapp = {
                                        viewModel.getSupplierPhoneForPo(po.id) { phone ->
                                            fetchedPhone = phone ?: ""
                                            showPhoneDialogForPo = po.id
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showPhoneDialogForPo != null) {
        AlertDialog(
            onDismissRequest = { showPhoneDialogForPo = null },
            title = { Text("Send via WhatsApp") },
            text = { Text("Send PO summary to ${fetchedPhone.takeIf { !it.isNullOrBlank() } ?: "the supplier"}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val poId = showPhoneDialogForPo!!
                        viewModel.sendPoViaWhatsApp(poId, fetchedPhone ?: "")
                        showPhoneDialogForPo = null
                    },
                    enabled = fetchedPhone != null
                ) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhoneDialogForPo = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PurchaseOrderCard(po: PurchaseOrderDto, onClick: () -> Unit, onSendWhatsapp: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("PO #${po.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                StatusChip(po.status)
            }
            Spacer(Modifier.height(8.dp))
            if (po.supplierName != null) {
                Text("Supplier: ${po.supplierName}", style = MaterialTheme.typography.bodyMedium)
            }
            Text("Created: ${po.createdAt?.take(10) ?: "N/A"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (po.expectedDeliveryDate != null) {
                Text("Expected: ${po.expectedDeliveryDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (po.status == "SENT") {
                    AssistChip(
                        onClick = onSendWhatsapp,
                        label = { Text("Send PO to Supplier") },
                        colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF25D366))
                    )
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                Text(String.format("$%.2f", po.totalAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (color, textCol) = when (status) {
        "DRAFT" -> Color(0xFFE0E0E0) to Color(0xFF424242)
        "SENT" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        "PARTIAL" -> Color(0xFFFFF3E0) to Color(0xFFEF6C00)
        "FULFILLED" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "CANCELLED" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        else -> Color(0xFFF5F5F5) to Color(0xFF9E9E9E)
    }
    Surface(shape = RoundedCornerShape(16.dp), color = color) {
        Text(status, color = textCol, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}
