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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(viewModel: SalesViewModel = hiltViewModel()) {
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val saleState by viewModel.saleState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var selectedPaymentMode by remember { mutableStateOf("cash") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("New Sale") })
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

                // Payment mode
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("cash", "card", "upi").forEach { mode ->
                        FilterChip(
                            selected = selectedPaymentMode == mode,
                            onClick = { selectedPaymentMode = mode },
                            label = { Text(mode.replaceFirstChar { it.uppercase() }) }
                        )
                    }
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
}
