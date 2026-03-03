package com.retailiq.datasage.ui.supplier

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.data.model.supplier.PurchaseOrderSummaryDto
import com.retailiq.datasage.data.model.supplier.SupplierProductDto
import com.retailiq.datasage.data.model.supplier.SupplierProfileDto
import com.retailiq.datasage.ui.viewmodel.SupplierProfileUiState
import com.retailiq.datasage.ui.viewmodel.SupplierViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierProfileScreen(
    supplierId: String,
    viewModel: SupplierViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onCreatePo: (String) -> Unit,
    onViewAllPos: (String) -> Unit
) {
    val profileState by viewModel.profileState.collectAsState()

    LaunchedEffect(supplierId) {
        viewModel.loadSupplierProfile(supplierId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Supplier Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { onCreatePo(supplierId) },
                        modifier = Modifier.padding(end = 8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = "Create Purchase Order", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Create PO")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val state = profileState) {
                is SupplierProfileUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is SupplierProfileUiState.Error -> {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadSupplierProfile(supplierId) }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Retry")
                        }
                    }
                }
                is SupplierProfileUiState.Loaded -> {
                    ProfileContent(state.profile, onViewAllPos)
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(profile: SupplierProfileDto, onViewAllPos: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ContactCard(profile) }
        item { AnalyticsCard(profile) }
        item { TimelineHeader(profile.id, onViewAllPos) }
        item { PoTimeline(profile.recentPurchaseOrders) }
        item { Text("Sourced Products", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (profile.sourcedProducts.isEmpty()) {
            item { Text("No products sourced yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(profile.sourcedProducts) { product ->
                SupplierProductCard(product)
            }
        }
    }
}

@Composable
private fun ContactCard(profile: SupplierProfileDto) {
    val contact = profile.contact
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(profile.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (!contact?.name.isNullOrBlank()) Text("Contact: ${contact?.name}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = "Phone", Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(contact?.phone ?: "No phone", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Email, contentDescription = "Email", Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(contact?.email ?: "No email", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun AnalyticsCard(profile: SupplierProfileDto) {
    val analytics = profile.analytics
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Lead Time", style = MaterialTheme.typography.labelMedium)
                Text(analytics?.avgLeadTimeDays?.let { "%.1f d".format(it) } ?: "-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Fill Rate (90d)", style = MaterialTheme.typography.labelMedium)
                // fill_rate_90d is already a percentage (e.g. 85.0)
                val fillRateFraction = (analytics?.fillRate90d ?: 0.0) / 100.0
                FillRateChip(fillRateFraction)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Terms", style = MaterialTheme.typography.labelMedium)
                Text("${profile.paymentTermsDays}d", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TimelineHeader(supplierId: String, onViewAllPos: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("90-Day PO History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = { onViewAllPos(supplierId) }) { Text("View All") }
    }
}

@Composable
private fun PoTimeline(pos: List<PurchaseOrderSummaryDto>) {
    if (pos.isEmpty()) {
        Text("No recent orders.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        pos.forEach { po ->
            AssistChip(
                onClick = { /* Could open PO detail */ },
                label = { Text("PO#${po.id.take(8)} • ${po.status}") },
                leadingIcon = { Icon(Icons.Default.Receipt, null, Modifier.size(16.dp)) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun SupplierProductCard(product: SupplierProductDto) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(product.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(String.format("$%.2f", product.quotedPrice), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Lead: ${product.leadTimeDays}d", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
