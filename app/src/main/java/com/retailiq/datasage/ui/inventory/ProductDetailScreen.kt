package com.retailiq.datasage.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.data.api.Product
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.StockUpdateRequest
import com.retailiq.datasage.ui.pricing.PriceHistorySection
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Int,
    onNavigateBack: () -> Unit,
    inventoryViewModel: InventoryViewModel = hiltViewModel()
) {
    val uiState by inventoryViewModel.uiState.collectAsState()

    val stockUpdateState by inventoryViewModel.stockUpdateState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAdjustDialog by remember { mutableStateOf(false) }

    // Ensure product list is loaded
    LaunchedEffect(Unit) {
        if (uiState is InventoryUiState.Loading) inventoryViewModel.loadProducts()
    }

    LaunchedEffect(stockUpdateState) {
        when (stockUpdateState) {
            is NetworkResult.Success -> {
                snackbarHostState.showSnackbar("Stock updated successfully")
                inventoryViewModel.resetStockUpdateState()
                showAdjustDialog = false
            }
            is NetworkResult.Error -> {
                snackbarHostState.showSnackbar((stockUpdateState as NetworkResult.Error).message ?: "Error updating stock")
                inventoryViewModel.resetStockUpdateState()
            }
            else -> {}
        }
    }

    if (showAdjustDialog) {
        val product = (uiState as? InventoryUiState.Loaded)?.products?.find { it.productId == productId }
        product?.let {
            AdjustStockBottomSheet(
                product = it,
                onDismiss = { showAdjustDialog = false },
                onSubmit = { req -> inventoryViewModel.submitStockUpdate(productId, req) },
                isLoading = stockUpdateState is NetworkResult.Loading
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Product Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState is InventoryUiState.Loaded) {
                FloatingActionButton(onClick = { showAdjustDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Adjust Stock")
                }
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is InventoryUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is InventoryUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is InventoryUiState.Loaded -> {
                val product = state.products.find { it.productId == productId }
                if (product == null) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("Product not found")
                    }
                } else {
                    ProductDetailContent(
                        product = product,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductDetailContent(
    product: Product,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Basic info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(product.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                product.skuCode?.let {
                    Text("SKU: $it", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailItem("Selling Price", "₹${String.format("%.2f", product.sellingPrice)}")
                    DetailItem("Cost Price", "₹${String.format("%.2f", product.costPrice)}")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val stockColor = when {
                        product.currentStock <= 0 -> MaterialTheme.colorScheme.error
                        product.currentStock <= product.reorderLevel -> Color(0xFFFF9800)
                        else -> Color(0xFF4CAF50)
                    }
                    DetailItem("Stock", "${product.currentStock.toInt()}", stockColor)
                    DetailItem("Reorder Level", "${product.reorderLevel.toInt()}")
                }
                product.supplierName?.let { DetailItem("Supplier", it) }
            }
        }

        // Price History expandable section
        PriceHistorySection(productId = product.productId)

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DetailItem(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustStockBottomSheet(
    product: Product,
    onDismiss: () -> Unit,
    onSubmit: (StockUpdateRequest) -> Unit,
    isLoading: Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var qtyAdded by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf(product.costPrice.toString()) }
    var supplierName by remember { mutableStateOf(product.supplierName ?: "") }
    var updateCostPrice by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Adjust Stock: ${product.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = qtyAdded,
                onValueChange = { qtyAdded = it },
                label = { Text("Quantity Added") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = purchasePrice,
                onValueChange = { purchasePrice = it },
                label = { Text("Purchase Price (₹)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            
            OutlinedTextField(
                value = supplierName,
                onValueChange = { supplierName = it },
                label = { Text("Supplier Name (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = updateCostPrice,
                    onCheckedChange = { updateCostPrice = it }
                )
                Text("Update product cost price")
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { 
                    val qty = qtyAdded.toDoubleOrNull() ?: 0.0
                    val price = purchasePrice.toDoubleOrNull() ?: product.costPrice
                    
                    val req = StockUpdateRequest(
                        quantityAdded = qty,
                        purchasePrice = price,
                        supplierName = supplierName.takeIf { it.isNotBlank() },
                        updateCostPrice = updateCostPrice
                    )
                    onSubmit(req)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && qtyAdded.isNotEmpty() && qtyAdded.toDoubleOrNull() != null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp), color = Color.White)
                } else {
                    Text("Confirm Stock Adjustment")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
