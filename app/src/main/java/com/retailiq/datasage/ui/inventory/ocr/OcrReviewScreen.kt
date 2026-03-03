package com.retailiq.datasage.ui.inventory.ocr

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.data.api.ConfirmedItem
import com.retailiq.datasage.data.api.OcrItemDto
import com.retailiq.datasage.ui.inventory.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrReviewScreen(
    jobId: String,
    onNavigateBack: () -> Unit,
    viewModel: OcrViewModel = hiltViewModel(),
    inventoryViewModel: InventoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val inventoryState by inventoryViewModel.uiState.collectAsState()

    // We start polling when the screen is deployed.
    LaunchedEffect(jobId) {
        viewModel.startPolling(jobId)
        inventoryViewModel.loadProducts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Invoice") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                is OcrState.Polling, is OcrState.Uploading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Analyzing invoice...")
                    }
                }
                is OcrState.Review -> {
                    val reviewState = state as OcrState.Review
                    OcrReviewContent(
                        jobId = jobId,
                        items = reviewState.items,
                        inventoryProducts = (inventoryState as? com.retailiq.datasage.ui.inventory.InventoryUiState.Loaded)?.products ?: emptyList(),
                        onConfirm = { items -> viewModel.confirm(jobId, items) },
                        onDismiss = { viewModel.dismiss(jobId) },
                        onDone = onNavigateBack
                    )
                }
                is OcrState.Confirming -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Updating stock...")
                    }
                }
                is OcrState.Done -> {
                    val message = (state as OcrState.Done).message
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(message, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Done")
                        }
                    }
                }
                is OcrState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Error", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text((state as OcrState.Error).message)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Go Back")
                        }
                    }
                }
                else -> { Box(Modifier.fillMaxSize()) }
            }
        }
    }
}

// Data class mapping the interactive UI row state representing an OCR entry
data class EditableOcrItemRow(
    val id: Int,
    val originalDto: OcrItemDto,
    var selectedProductId: Int? = null,
    var selectedProductName: String = "",
    var qty: String = "",
    var unitPrice: String = "",
    var isChecked: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrReviewContent(
    jobId: String,
    items: List<OcrItemDto>,
    inventoryProducts: List<com.retailiq.datasage.data.api.Product>,
    onConfirm: (List<ConfirmedItem>) -> Unit,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    val editableItems = remember(items) {
        val list = mutableStateListOf<EditableOcrItemRow>()
        items.forEachIndexed { index, dto ->
            val confidence = dto.confidence ?: 0.0
            val prefilledProductId = if (confidence > 0.6) dto.matchedProductId else null
            val prefilledProductName = if (confidence > 0.6) dto.matchedProductName ?: "" else ""
            list.add(
                EditableOcrItemRow(
                    id = index,
                    originalDto = dto,
                    selectedProductId = prefilledProductId,
                    selectedProductName = prefilledProductName,
                    qty = dto.qty?.toString() ?: "",
                    unitPrice = dto.unitPrice?.toString() ?: "",
                    isChecked = true
                )
            )
        }
        list
    }

    var showDismissDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            itemsIndexed(editableItems) { index, row ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (row.isChecked) MaterialTheme.colorScheme.surface else Color(0xFFF5F5F5)
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = row.isChecked,
                                onCheckedChange = { checked ->
                                    editableItems[index] = row.copy(isChecked = checked)
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Raw: ${row.originalDto.rawText}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        var expanded by remember { mutableStateOf(false) }
                        var searchedText by remember { mutableStateOf(row.selectedProductName) }

                        // Product Selection (Dropdown)
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = searchedText,
                                onValueChange = { 
                                    searchedText = it
                                    editableItems[index] = row.copy(selectedProductName = it, selectedProductId = null)
                                    expanded = true
                                },
                                label = { Text("Product Match") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                singleLine = true,
                                readOnly = !row.isChecked,
                                enabled = row.isChecked
                            )
                            val filteredOptions = inventoryProducts.filter {
                                it.name.contains(searchedText, ignoreCase = true)
                            }.take(5)

                            if (filteredOptions.isNotEmpty()) {
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    filteredOptions.forEach { product ->
                                        DropdownMenuItem(
                                            text = { Text(product.name) },
                                            onClick = {
                                                searchedText = product.name
                                                editableItems[index] = row.copy(
                                                    selectedProductId = product.productId,
                                                    selectedProductName = product.name
                                                )
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = row.qty,
                                onValueChange = { editableItems[index] = row.copy(qty = it) },
                                label = { Text("Qty") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                enabled = row.isChecked
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = row.unitPrice,
                                onValueChange = { editableItems[index] = row.copy(unitPrice = it) },
                                label = { Text("Unit Price") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                enabled = row.isChecked
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        androidx.compose.material3.Surface(
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { showDismissDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Discard")
                }
                Spacer(Modifier.width(16.dp))
                
                // Button is enabled if at least 1 item is checked and ALL checked items have a valid product selected
                val hasAnyChecked = editableItems.any { it.isChecked }
                val allCheckedValid = editableItems.filter { it.isChecked }.all { it.selectedProductId != null && it.qty.toDoubleOrNull() != null && it.unitPrice.toDoubleOrNull() != null }
                
                Button(
                    onClick = {
                        val requests = editableItems.filter { it.isChecked }.map {
                            ConfirmedItem(
                                itemId = it.originalDto.itemId,
                                matchedProductId = it.selectedProductId!!,
                                qty = it.qty.toDoubleOrNull() ?: 1.0,
                                unitPrice = it.unitPrice.toDoubleOrNull()
                            )
                        }
                        onConfirm(requests)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = hasAnyChecked && allCheckedValid
                ) {
                    Text("Update Stock")
                }
            }
        }
    }

    if (showDismissDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDismissDialog = false },
            title = { Text("Discard Invoice?") },
            text = { Text("Are you sure you want to dismiss this invoice? All extracted data will be lost.") },
            confirmButton = {
                Button(onClick = {
                    showDismissDialog = false
                    onDismiss()
                }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDismissDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
