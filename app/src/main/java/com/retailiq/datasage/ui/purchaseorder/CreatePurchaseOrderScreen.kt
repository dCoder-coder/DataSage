package com.retailiq.datasage.ui.purchaseorder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.data.api.Product
import com.retailiq.datasage.data.model.supplier.CreatePoItemRequest
import com.retailiq.datasage.data.model.supplier.SupplierDto
import com.retailiq.datasage.ui.inventory.InventoryViewModel
import com.retailiq.datasage.ui.inventory.InventoryUiState
import com.retailiq.datasage.ui.viewmodel.PoActionUiState
import com.retailiq.datasage.ui.viewmodel.PurchaseOrderViewModel
import com.retailiq.datasage.ui.viewmodel.SupplierListUiState
import com.retailiq.datasage.ui.viewmodel.SupplierViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePurchaseOrderScreen(
    prefillSupplierId: String? = null,
    prefillProductId: Int? = null,
    supplierVm: SupplierViewModel = hiltViewModel(),
    inventoryVm: InventoryViewModel = hiltViewModel(),
    poVm: PurchaseOrderViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    
    // Step 1 State
    var selectedSupplier by remember { mutableStateOf<SupplierDto?>(null) }
    
    // Step 2 State
    val lineItems = remember { mutableStateListOf<PoLineItemState>() }
    
    // Step 3 State
    var expectedDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val supplierState by supplierVm.listState.collectAsState()
    val inventoryState by inventoryVm.uiState.collectAsState()
    val actionState by poVm.actionState.collectAsState()

    // Pre-fill Logic
    LaunchedEffect(supplierState) {
        if (prefillSupplierId != null && supplierState is SupplierListUiState.Loaded) {
            val sup = (supplierState as SupplierListUiState.Loaded).suppliers.find { it.id == prefillSupplierId }
            if (sup != null) {
                selectedSupplier = sup
                if (currentStep == 1) currentStep = 2 // Auto advance
            }
        }
    }

    LaunchedEffect(inventoryState) {
        if (prefillProductId != null && lineItems.isEmpty() && inventoryState is InventoryUiState.Loaded) {
            val prod = (inventoryState as InventoryUiState.Loaded).products.find { it.productId == prefillProductId }
            if (prod != null) {
                lineItems.add(PoLineItemState(product = prod, qty = "10", price = prod.sellingPrice.toString()))
            }
        }
    }

    LaunchedEffect(actionState) {
        if (actionState is PoActionUiState.Success) {
            poVm.resetActionState()
            onNavigateBack()
        }
    }

    if (lineItems.isEmpty() && prefillProductId == null) {
        lineItems.add(PoLineItemState()) // Start with 1 empty row
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create PO - Step $currentStep/3") },
                navigationIcon = {
                    IconButton(onClick = { if (currentStep > 1) currentStep-- else onNavigateBack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Total: $${String.format("%.2f", lineItems.sumOf { (it.qty.toIntOrNull() ?: 0) * (it.price.toDoubleOrNull() ?: 0.0) })}",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                    )
                    
                    if (currentStep < 3) {
                        Button(
                            onClick = { currentStep++ },
                            enabled = (currentStep == 1 && selectedSupplier != null) || (currentStep == 2 && lineItems.all { it.isValid() })
                        ) {
                            Text("Next")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp))
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    poVm.createPo(selectedSupplier!!.id, expectedDate.takeIf { it.isNotBlank() }, notes, isDraft = true, items = lineItems.map { it.toRequest() })
                                },
                                enabled = actionState !is PoActionUiState.InProgress
                            ) { Text("Save Draft") }
                            
                            Button(
                                onClick = {
                                    poVm.createPo(selectedSupplier!!.id, expectedDate.takeIf { it.isNotBlank() }, notes, isDraft = false, items = lineItems.map { it.toRequest() })
                                },
                                enabled = actionState !is PoActionUiState.InProgress
                            ) {
                                if (actionState is PoActionUiState.InProgress) {
                                    CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Send PO")
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            when (currentStep) {
                1 -> Step1SupplierSelector(supplierState, selectedSupplier) { selectedSupplier = it }
                2 -> Step2LineItems(lineItems, inventoryState) { lineItems.add(PoLineItemState()) }
                3 -> Step3Summary(expectedDate, notes, { expectedDate = it }, { notes = it }, actionState)
            }
        }
    }
}

// ─── Step 1 ─────────────────────────────────────────────────────────────

@Composable
fun Step1SupplierSelector(state: SupplierListUiState, selected: SupplierDto?, onSelect: (SupplierDto) -> Unit) {
    Column {
        Text("Select Supplier", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        when (state) {
            is SupplierListUiState.Loading -> CircularProgressIndicator()
            is SupplierListUiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
            is SupplierListUiState.Loaded -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.suppliers.size) { index ->
                        val sup = state.suppliers[index]
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(sup) },
                            colors = CardDefaults.cardColors(containerColor = if (selected?.id == sup.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(sup.name, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected?.id == sup.id) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

// ─── Step 2 ─────────────────────────────────────────────────────────────

class PoLineItemState(
    var product: Product? = null,
    var qty: String = "",
    var price: String = ""
) {
    fun isValid() = product != null && (qty.toIntOrNull() ?: 0) > 0 && (price.toDoubleOrNull() ?: 0.0) >= 0.0
    fun toRequest() = CreatePoItemRequest(product!!.productId, qty.toInt(), price.toDouble())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step2LineItems(items: MutableList<PoLineItemState>, inventoryState: InventoryUiState, onAddRow: () -> Unit) {
    val products = if (inventoryState is InventoryUiState.Loaded) inventoryState.products else emptyList()

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Line Items", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = onAddRow) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Row")
            }
        }
        Spacer(Modifier.height(8.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            itemsIndexed(items) { index, item ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        // Very simple product dropdown/selection fallback (in reality this would be an ExposedDropdownMenuBox with search)
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                value = item.product?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Product") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                products.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.name) },
                                        onClick = {
                                            // Ideally if supplier was selected, we look up `supplier_products.quoted_price`
                                            // But for now we just use standard cost/price.
                                            val newObj = PoLineItemState(p, item.qty.takeIf { it.isNotBlank() } ?: "10", p.sellingPrice.toString())
                                            items[index] = newObj
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = item.qty,
                                onValueChange = { items[index] = PoLineItemState(item.product, it, item.price) },
                                label = { Text("Qty") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                isError = item.qty.isNotBlank() && item.qty.toIntOrNull() == null
                            )
                            OutlinedTextField(
                                value = item.price,
                                onValueChange = { items[index] = PoLineItemState(item.product, item.qty, it) },
                                label = { Text("Unit \$") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                isError = item.price.isNotBlank() && item.price.toDoubleOrNull() == null
                            )
                            IconButton(onClick = { if (items.size > 1) items.removeAt(index) }) {
                                Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Step 3 ─────────────────────────────────────────────────────────────

@Composable
fun Step3Summary(
    expectedDate: String,
    notes: String,
    onDateChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    actionState: PoActionUiState
) {
    Column {
        Text("Final Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = expectedDate,
            onValueChange = onDateChange,
            label = { Text("Expected Delivery (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notes / Instructions") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            maxLines = 4
        )
        
        Spacer(Modifier.height(24.dp))
        if (actionState is PoActionUiState.Error) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(actionState.message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp))
            }
        }
    }
}
