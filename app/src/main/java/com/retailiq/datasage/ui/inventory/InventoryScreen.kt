package com.retailiq.datasage.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.ui.inventory.ocr.OcrState
import com.retailiq.datasage.ui.inventory.ocr.OcrViewModel
import androidx.lifecycle.repeatOnLifecycle
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onNavigateToAddProduct: () -> Unit = {},
    onNavigateToProduct: (Int) -> Unit = {},
    onNavigateToOcrReview: (String) -> Unit = {},
    onNavigateToAudit: () -> Unit = {},
    viewModel: InventoryViewModel = hiltViewModel(),
    ocrViewModel: OcrViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val ocrState by ocrViewModel.state.collectAsState()

    // Reload products every time this screen resumes
    // (e.g., when navigating back from inventory/add)
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            viewModel.loadProducts()
        }
    }

    val context = LocalContext.current
    var capturedPhotoFile by remember { mutableStateOf<File?>(null) }
    var photoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            showPreviewDialog = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val file = File(context.cacheDir, "ocr_invoice_${System.currentTimeMillis()}.jpg")
            capturedPhotoFile = file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            photoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    LaunchedEffect(ocrState) {
        if (ocrState is OcrState.Polling) {
            val jobId = (ocrState as OcrState.Polling).jobId
            onNavigateToOcrReview(jobId)
            ocrViewModel.reset()
            showPreviewDialog = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory") },
                actions = {
                    IconButton(onClick = onNavigateToAudit) {
                        Icon(Icons.Default.FactCheck, contentDescription = "Stock Audit")
                    }
                    IconButton(onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) }) {
                        Icon(Icons.Outlined.DocumentScanner, contentDescription = "Scan Invoice")
                    }
                    IconButton(onClick = { viewModel.loadProducts() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(onClick = onNavigateToAddProduct) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.search(it) },
                label = { Text("Search products") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            when (val state = uiState) {
                is InventoryUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is InventoryUiState.Loaded -> {
                    if (state.products.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No products found")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.products, key = { it.productId }) { product ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToProduct(product.productId) },
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(product.name, fontWeight = FontWeight.SemiBold)
                                            product.skuCode?.let {
                                                Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            }
                                            Text(
                                                "₹${String.format("%.2f", product.sellingPrice)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            val stockColor = when {
                                                product.currentStock <= 0 -> MaterialTheme.colorScheme.error
                                                product.currentStock <= product.reorderLevel -> Color(0xFFFF9800)
                                                else -> Color(0xFF4CAF50)
                                            }
                                            Text(
                                                "Stock: ${product.currentStock.toInt()}",
                                                fontWeight = FontWeight.Bold,
                                                color = stockColor
                                            )
                                        }
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
                is InventoryUiState.Error -> {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadProducts() }) { Text("Retry") }
                    }
                }
            }
        }
    }

    if (showPreviewDialog && capturedPhotoFile != null) {
        AlertDialog(
            onDismissRequest = { if (ocrState !is OcrState.Uploading) showPreviewDialog = false },
            title = { Text("Invoice Captured") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text("Photo saved temporarily. Do you want to process this invoice?")
                    Spacer(Modifier.height(16.dp))
                    if (ocrState is OcrState.Uploading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text("Uploading...", style = MaterialTheme.typography.bodySmall)
                    } else if (ocrState is OcrState.Error) {
                        Text((ocrState as OcrState.Error).message, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { ocrViewModel.uploadInvoice(capturedPhotoFile!!) },
                    enabled = ocrState !is OcrState.Uploading
                ) {
                    Text("Use This Photo")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showPreviewDialog = false
                        capturedPhotoFile?.delete()
                        ocrViewModel.reset()
                    },
                    enabled = ocrState !is OcrState.Uploading
                ) {
                    Text("Retake")
                }
            }
        )
    }
}
