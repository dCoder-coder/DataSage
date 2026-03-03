package com.retailiq.datasage.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.retailiq.datasage.data.model.HsnDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    onNavigateBack: () -> Unit,
    onSaveProduct: (name: String, costPrice: Double, sellingPrice: Double, hsnCode: String?, gstRate: Double?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    
    // HSN Selection State
    var selectedHsn by remember { mutableStateOf<HsnDto?>(null) }
    var showHsnSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Product") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = costPrice,
                    onValueChange = { costPrice = it },
                    label = { Text("Cost Price") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = sellingPrice,
                    onValueChange = { sellingPrice = it },
                    label = { Text("Selling Price *") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            Divider()
            
            Text("GST Information", style = MaterialTheme.typography.titleMedium)

            // HSN Code Field - Read Only, tap to open sheet
            OutlinedTextField(
                value = selectedHsn?.let { "${it.hsn_code} - ${it.description}" } ?: "",
                onValueChange = { },
                label = { Text("HSN Code") },
                placeholder = { Text("Tap to search HSN") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showHsnSheet = true },
                enabled = false, // Use enabled false to act purely as a button wrapper but styling requires colors adjustment
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            if (selectedHsn != null) {
                Text(
                    text = "Rate set by HSN default (${selectedHsn!!.default_rate}%) — tap to override",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    val cp = costPrice.toDoubleOrNull() ?: 0.0
                    val sp = sellingPrice.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && sp > 0) {
                        onSaveProduct(name, cp, sp, selectedHsn?.hsn_code, selectedHsn?.default_rate) 
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && sellingPrice.isNotBlank()
            ) {
                Text("Save Product")
            }
        }
        
        if (showHsnSheet) {
            HsnSearchBottomSheet(
                onDismissRequest = { showHsnSheet = false },
                onHsnSelected = { hsn -> selectedHsn = hsn }
            )
        }
    }
}
