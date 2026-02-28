package com.retailiq.datasage.ui.supplier

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.data.model.supplier.SupplierDto
import com.retailiq.datasage.ui.viewmodel.SupplierCreateUiState
import com.retailiq.datasage.ui.viewmodel.SupplierListUiState
import com.retailiq.datasage.ui.viewmodel.SupplierViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierListScreen(
    viewModel: SupplierViewModel = hiltViewModel(),
    onNavigateToSupplier: (Int) -> Unit
) {
    val listState by viewModel.listState.collectAsState()
    var isSheetOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Suppliers") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { isSheetOpen = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Supplier")
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val state = listState) {
                is SupplierListUiState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                is SupplierListUiState.Error -> {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadSuppliers() }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Retry")
                        }
                    }
                }
                is SupplierListUiState.Loaded -> {
                    if (state.suppliers.isEmpty()) {
                        Text("No suppliers found.\nTap + to add one.", 
                             modifier = Modifier.align(Alignment.Center),
                             color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.suppliers) { supplier ->
                                SupplierCard(supplier, onClick = { onNavigateToSupplier(supplier.id) })
                            }
                        }
                    }
                }
            }
        }
    }

    if (isSheetOpen) {
        AddSupplierBottomSheet(
            onDismiss = {
                isSheetOpen = false
                viewModel.resetCreateState()
            }
        )
    }
}

@Composable
fun SupplierCard(supplier: SupplierDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(supplier.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (!supplier.contactName.isNullOrBlank()) {
                        Text(supplier.contactName, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                FillRateChip(supplier.fillRate)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Lead time: ${supplier.avgLeadTimeDays?.let { "%.1fd".format(it) } ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Terms: ${supplier.paymentTermsDays}d",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun FillRateChip(fillRate: Double?) {
    if (fillRate == null) return
    val percentage = (fillRate * 100).toInt()
    val (color, textCol) = when {
        percentage >= 90 -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        percentage >= 70 -> Color(0xFFFFF3E0) to Color(0xFFEF6C00)
        else -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color,
        modifier = Modifier.wrapContentSize()
    ) {
        Text(
            "$percentage%",
            color = textCol,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSupplierBottomSheet(
    viewModel: SupplierViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val createState by viewModel.createState.collectAsState()

    var name by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var terms by remember { mutableStateOf("30") }

    LaunchedEffect(createState) {
        if (createState is SupplierCreateUiState.Success) {
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp).fillMaxWidth().windowInsetsPadding(WindowInsets.ime)) {
            Text("Add New Supplier", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Company Name *") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Business, null) },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = contactName,
                onValueChange = { contactName = it },
                label = { Text("Contact Person") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                OutlinedTextField(
                    value = terms,
                    onValueChange = { terms = it.filter { c -> c.isDigit() } },
                    label = { Text("Terms (Days)") },
                    modifier = Modifier.weight(0.7f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Email, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )

            if (createState is SupplierCreateUiState.Error) {
                Text(
                    (createState as SupplierCreateUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.createSupplier(name, contactName.takeIf { it.isNotBlank() }, phone.takeIf { it.isNotBlank() }, email.takeIf { it.isNotBlank() }, terms.toIntOrNull() ?: 30)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && createState !is SupplierCreateUiState.Creating
            ) {
                if (createState is SupplierCreateUiState.Creating) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save Supplier")
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
