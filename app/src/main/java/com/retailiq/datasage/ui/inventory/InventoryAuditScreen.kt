package com.retailiq.datasage.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.data.api.AuditItem
import com.retailiq.datasage.data.api.AuditRequest
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryAuditScreen(
    onNavigateBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val auditState by viewModel.auditState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Map storing Product ID -> Actual Qty entered by user
    val auditEntries = remember { mutableStateMapOf<Int, String>() }
    var notes by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (uiState is InventoryUiState.Loading) viewModel.loadProducts()
    }

    LaunchedEffect(auditState) {
        when (auditState) {
            is NetworkResult.Success -> {
                snackbarHostState.showSnackbar("Audit submitted successfully")
                viewModel.resetAuditState()
                onNavigateBack()
            }
            is NetworkResult.Error -> {
                snackbarHostState.showSnackbar((auditState as NetworkResult.Error).message ?: "Error submitting audit")
                viewModel.resetAuditState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Stock Audit") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState is InventoryUiState.Loaded && auditEntries.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showConfirmDialog = true },
                    icon = { Icon(Icons.Default.Check, "Submit Audit") },
                    text = { Text("Submit (${auditEntries.size} items)") }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is InventoryUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is InventoryUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is InventoryUiState.Loaded -> {
                    AuditListContent(
                        products = state.products,
                        auditEntries = auditEntries,
                        notes = notes,
                        onNotesChange = { notes = it }
                    )
                }
            }
        }

        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Confirm Audit Submission") },
                text = { Text("You have audited ${auditEntries.size} product(s). This will adjust their stock levels in the backend to match your inputs. Proceed?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmDialog = false
                            val requests = auditEntries.mapNotNull { (productId, qtyStr) ->
                                val qty = qtyStr.toDoubleOrNull()
                                if (qty != null) AuditItem(productId, qty) else null
                            }
                            if (requests.isNotEmpty()) {
                                viewModel.submitAudit(AuditRequest(items = requests, notes = notes))
                            }
                        },
                        enabled = auditState !is NetworkResult.Loading
                    ) {
                        if (auditState is NetworkResult.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Submit Audit")
                        }
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
}

@Composable
private fun AuditListContent(
    products: List<Product>,
    auditEntries: MutableMap<Int, String>,
    notes: String,
    onNotesChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Audit Notes (Optional)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                minLines = 2
            )
            Divider(Modifier.padding(vertical = 8.dp))
        }

        items(products, key = { it.productId }) { product ->
            AuditProductRow(
                product = product,
                enteredQty = auditEntries[product.productId] ?: "",
                onQtyChange = { val str = it.trim(); if (str.isEmpty()) auditEntries.remove(product.productId) else auditEntries[product.productId] = str }
            )
        }
        
        item {
            Spacer(Modifier.height(80.dp)) // FAB clearance
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuditProductRow(
    product: Product,
    enteredQty: String,
    onQtyChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enteredQty.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Expected Stock: ${product.currentStock.toInt()}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Spacer(Modifier.width(16.dp))
            OutlinedTextField(
                value = enteredQty,
                onValueChange = onQtyChange,
                modifier = Modifier.width(100.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                placeholder = { Text("Qty") },
                textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
