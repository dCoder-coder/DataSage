package com.retailiq.datasage.ui.purchaseorder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.data.model.supplier.GoodsReceiptItemRequest
import com.retailiq.datasage.data.model.supplier.PurchaseOrderDto
import com.retailiq.datasage.ui.viewmodel.PoActionUiState
import com.retailiq.datasage.ui.viewmodel.PoDetailUiState
import com.retailiq.datasage.ui.viewmodel.PurchaseOrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodsReceiptScreen(
    poId: Int,
    viewModel: PurchaseOrderViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val detailState by viewModel.detailState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(poId) {
        viewModel.loadPurchaseOrder(poId)
    }

    LaunchedEffect(actionState) {
        if (actionState is PoActionUiState.Success) {
            viewModel.resetActionState()
            onNavigateBack() // Could also show snackbar
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receive Goods - PO #$poId") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val state = detailState) {
                is PoDetailUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is PoDetailUiState.Error -> {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadPurchaseOrder(poId) }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Retry")
                        }
                    }
                }
                is PoDetailUiState.Loaded -> {
                    ReceiptContent(state.order, actionState) { showConfirmDialog = true }
                }
            }
        }
    }

    if (showConfirmDialog && detailState is PoDetailUiState.Loaded) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Receipt") },
            text = { Text("This will update your stock levels significantly. Proceed?") },
            confirmButton = {
                Button(onClick = {
                    showConfirmDialog = false
                    val po = (detailState as PoDetailUiState.Loaded).order
                    // Default behavior: Receive exactly what was ordered for simplicity in this demo,
                    // In a real app we would map local state changes here
                    val receivedItems = po.items.map { 
                        GoodsReceiptItemRequest(it.id, it.orderedQty, it.unitPrice) 
                    }
                    viewModel.receiveGoods(po.id, receivedItems)
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Data structures for holding editable quantities
class ReceiptLineItem(
    val poItemId: Int,
    val productName: String,
    val ordered: Int,
    var receivedQty: String,
    var unitPrice: String
)

@Composable
fun ReceiptContent(po: PurchaseOrderDto, actionState: PoActionUiState, onConfirmClick: () -> Unit) {
    // Generate editable state for each ordered item
    val editableItems = remember(po.items) {
        po.items.map {
            ReceiptLineItem(
                poItemId = it.id,
                productName = it.productName ?: "Unknown Product",
                ordered = it.orderedQty,
                receivedQty = it.orderedQty.toString(), // Default pre-fill
                unitPrice = it.unitPrice.toString()
            )
        }.toMutableStateList()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Order Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Supplier: ${po.supplierName ?: "Unknown"}", style = MaterialTheme.typography.bodyMedium)
        Text("Status: ${po.status}", style = MaterialTheme.typography.bodyMedium)
        
        Spacer(Modifier.height(24.dp))
        Text("Line Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            itemsIndexed(editableItems) { _, item ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(item.productName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("Ordered: ${item.ordered}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Quick inputs using mutable string states (In Kotlin real projects prefer delegating up to VM)
                            OutlinedTextField(
                                value = item.receivedQty,
                                onValueChange = { item.receivedQty = it },
                                label = { Text("Recv Qty") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = item.unitPrice,
                                onValueChange = { item.unitPrice = it },
                                label = { Text("Unit \$") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (actionState is PoActionUiState.Error) {
            Text(actionState.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }

        Button(
            onClick = onConfirmClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = actionState !is PoActionUiState.InProgress && po.status != "FULFILLED"
        ) {
            if (actionState is PoActionUiState.InProgress) {
                CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(if (po.status == "FULFILLED") "Already Fulfilled" else "Confirm Receipt")
            }
        }
    }
}
