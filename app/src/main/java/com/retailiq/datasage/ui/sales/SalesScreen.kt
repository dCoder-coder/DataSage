package com.retailiq.datasage.ui.sales

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.retailiq.datasage.data.api.Product
import com.retailiq.datasage.ui.viewmodel.BarcodeLookupUiState
import com.retailiq.datasage.ui.viewmodel.ReceiptsViewModel
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.repeatOnLifecycle
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    viewModel: SalesViewModel = hiltViewModel(),
    receiptsViewModel: ReceiptsViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val saleState by viewModel.saleState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val loyaltyAccount by viewModel.loyaltyAccount.collectAsState()
    val creditAccount by viewModel.creditAccount.collectAsState()
    val redemptionPoints by viewModel.redemptionPoints.collectAsState()

    var selectedPaymentMode by remember { mutableStateOf("cash") }
    var customerSearchQuery by remember { mutableStateOf("") }
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    val barcodeLookupState by receiptsViewModel.barcodeLookupState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Reload products every time this screen becomes visible (e.g. navigating back from inventory)
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadProducts()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            receiptsViewModel.lookupBarcode(result.contents)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scanLauncher.launch(ScanOptions().apply { setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES) })
        } else {
            coroutineScope.launch { snackbarHostState.showSnackbar("Camera permission is required to scan barcodes") }
        }
    }

    // Handle barcode lookup success
    LaunchedEffect(barcodeLookupState) {
        when (val state = barcodeLookupState) {
            is BarcodeLookupUiState.Success -> {
                val productDto = state.product
                val product = Product(
                    productId = productDto.productId,
                    name = productDto.productName,
                    categoryId = 0, // Placeholder mapping since BarcodeProductDto doesn't supply it
                    sellingPrice = productDto.price,
                    costPrice = productDto.price,
                    currentStock = productDto.currentStock,
                    reorderLevel = 0.0
                )
                viewModel.addToCart(product)
                receiptsViewModel.resetBarcodeLookup()
            }
            is BarcodeLookupUiState.Error -> {
                coroutineScope.launch { snackbarHostState.showSnackbar(state.message) }
                receiptsViewModel.resetBarcodeLookup()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("New Sale") },
                actions = {
                    IconButton(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchProducts(it) },
                label = { Text("Search products") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            // Product list
            AnimatedVisibility(visible = cart.isEmpty() || searchQuery.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(products, key = { it.productId }) { product ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            onClick = { viewModel.addToCart(product) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Medium)
                                    Text(
                                        "₹${String.format("%.2f", product.sellingPrice)} • Stock: ${product.currentStock.toInt()}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        }
                    }
                }
            }

            // Cart section
            if (cart.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                Text(
                    "Cart (${cart.size} items)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(cart, key = { it.product.productId }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.product.name, fontWeight = FontWeight.Medium)
                                    Text(
                                        "₹${String.format("%.2f", item.lineTotal)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        viewModel.updateQuantity(item.product.productId, item.quantity - 1)
                                    }) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                    }
                                    Text("${item.quantity.toInt()}", fontWeight = FontWeight.Bold)
                                    IconButton(onClick = {
                                        viewModel.updateQuantity(item.product.productId, item.quantity + 1)
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase")
                                    }
                                    IconButton(onClick = {
                                        viewModel.removeFromCart(item.product.productId)
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                // Customer Selection
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        if (selectedCustomer != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(selectedCustomer!!.name, fontWeight = FontWeight.Bold)
                                    selectedCustomer!!.mobileNumber?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                }
                                IconButton(onClick = { viewModel.selectCustomer(null) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Customer")
                                }
                            }
                            
                            // Loyalty Redemption Card Let's assume min points = 50 
                            if (loyaltyAccount != null && loyaltyAccount!!.redeemablePoints >= 50) {
                                Spacer(Modifier.height(8.dp))
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                                    Column(Modifier.padding(12.dp).fillMaxWidth()) {
                                        Text("Redeem Points", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Text("Available to redeem: ${loyaltyAccount!!.redeemablePoints} points (= ₹${String.format("%.2f", loyaltyAccount!!.valueInCurrency)})", style = MaterialTheme.typography.bodySmall)
                                        Slider(
                                            value = redemptionPoints.toFloat(),
                                            onValueChange = { viewModel.setRedemptionPoints(it.toInt()) },
                                            valueRange = 0f..loyaltyAccount!!.redeemablePoints.toFloat(),
                                            steps = if (loyaltyAccount!!.redeemablePoints > 50) (loyaltyAccount!!.redeemablePoints / 10) else 0
                                        )
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Selected: $redemptionPoints")
                                            Text("Discount: -₹${String.format("%.2f", viewModel.loyaltyDiscount)}", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Search Box
                            Box {
                                OutlinedTextField(
                                    value = customerSearchQuery,
                                    onValueChange = { 
                                        customerSearchQuery = it
                                        viewModel.searchCustomers(it)
                                        customerDropdownExpanded = true
                                    },
                                    placeholder = { Text("Assign to Customer (Mobile/Name)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                                )
                                DropdownMenu(
                                    expanded = customerDropdownExpanded && customers.isNotEmpty(),
                                    onDismissRequest = { customerDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    customers.forEach { customer ->
                                        DropdownMenuItem(
                                            text = { Text("${customer.name} - ${customer.mobileNumber ?: ""}") },
                                            onClick = {
                                                viewModel.selectCustomer(customer)
                                                customerDropdownExpanded = false
                                                customerSearchQuery = ""
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Payment mode
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("cash", "card", "upi", "credit").forEach { mode ->
                        FilterChip(
                            selected = selectedPaymentMode == mode,
                            onClick = { selectedPaymentMode = mode },
                            label = { Text(mode.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                
                // Credit Warning
                if (selectedPaymentMode == "credit" && selectedCustomer != null) {
                    val availableCredit = creditAccount?.availableCredit ?: 0.0
                    val isExceeding = viewModel.cartTotal > availableCredit
                    Text(
                        text = if (isExceeding) "Warning: Sale exceeds available credit limit (₹${String.format("%.2f", availableCredit)})!" else "Available Credit: ₹${String.format("%.2f", availableCredit)}",
                        color = if (isExceeding) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()
                    )
                } else if (selectedPaymentMode == "credit" && selectedCustomer == null) {
                    Text(
                        text = "Warning: Credit sales require a customer to be selected.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // Total and submit
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "₹${String.format("%.2f", viewModel.cartTotal)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (viewModel.gstTotal > 0.0) {
                                Text(
                                    "Incl. GST: ₹${String.format("%.2f", viewModel.gstTotal)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.submitSale(selectedPaymentMode) },
                            enabled = saleState !is SaleUiState.Submitting
                        ) {
                            if (saleState is SaleUiState.Submitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(20.dp).width(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Complete Sale")
                            }
                        }
                    }
                }
            }
        }
    }

    // Success dialog
    if (saleState is SaleUiState.Success) {
        AlertDialog(
            onDismissRequest = { viewModel.resetSaleState() },
            confirmButton = {
                TextButton(onClick = { viewModel.resetSaleState() }) { Text("OK") }
            },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Sale Saved") },
            text = { Text("Transaction saved offline and queued for sync.\nID: ${(saleState as SaleUiState.Success).transactionId.take(8)}...") }
        )
    }

    // Error dialog
    if (saleState is SaleUiState.Error) {
        AlertDialog(
            onDismissRequest = { viewModel.resetSaleState() },
            confirmButton = {
                TextButton(onClick = { viewModel.resetSaleState() }) { Text("OK") }
            },
            title = { Text("Error") },
            text = { Text((saleState as SaleUiState.Error).message) }
        )
    }

    // Show Print Receipt Bottom Sheet
    if (saleState is SaleUiState.Success) {
        viewModel.lastTransactionId?.let { txId ->
            PrintReceiptBottomSheet(
                transactionId = txId,
                receiptsViewModel = receiptsViewModel,
                onDismiss = { viewModel.resetSaleState() } // Resetting sale state closes everything
            )
        } ?: run {
            // Fallback if lastTransactionId is somehow null
            viewModel.resetSaleState()
        }
    }
}
