package com.retailiq.datasage.ui.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.retailiq.datasage.data.api.Customer
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.CreditAccountDto
import com.retailiq.datasage.data.model.CreditTransactionDto
import com.retailiq.datasage.data.model.LoyaltyAccountDto
import com.retailiq.datasage.data.model.LoyaltyTransactionDto
import com.retailiq.datasage.ui.viewmodel.CustomerProfileUiState
import com.retailiq.datasage.ui.viewmodel.CustomerProfileViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileScreen(
    customerId: Int,
    onNavigateBack: () -> Unit,
    viewModel: CustomerProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Loyalty", "Credit")

    LaunchedEffect(customerId) {
        viewModel.loadCustomer(customerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is CustomerProfileUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is CustomerProfileUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadCustomer(customerId) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is CustomerProfileUiState.Loaded -> {
                    Column(Modifier.fillMaxWidth()) {
                        CustomerHeader(state.customer)
                        
                        TabRow(selectedTabIndex = selectedTab) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text(title) }
                                )
                            }
                        }

                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            when (selectedTab) {
                                0 -> OverviewTab(state.customer)
                                1 -> LoyaltyTab(state.loyaltyAccount, state.loyaltyTransactions)
                                2 -> CreditTab(
                                    customerId = customerId,
                                    creditAccount = state.creditAccount,
                                    creditTransactions = state.creditTransactions,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerHeader(customer: Customer) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(customer.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            customer.mobileNumber?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            customer.totalSpend?.let { 
                Spacer(Modifier.height(8.dp))
                Text("Lifetime Spend: ₹${String.format("%,.2f", it)}", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun OverviewTab(customer: Customer) {
    Column(Modifier.padding(16.dp)) {
        Text("Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Email: ${customer.email ?: "N/A"}")
        Text("Visits: ${customer.visitCount ?: 0}")
        Text("Address: ${customer.address ?: "N/A"}")
    }
}

@Composable
fun LoyaltyTab(account: LoyaltyAccountDto?, transactions: List<LoyaltyTransactionDto>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (account == null) {
                Text("No Loyalty Account Found", color = MaterialTheme.colorScheme.error)
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Available Points", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "${account.pointsBalance}",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Redeemable", style = MaterialTheme.typography.bodySmall)
                                Text("${account.redeemablePoints}", fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Value", style = MaterialTheme.typography.bodySmall)
                                Text("₹${String.format("%.2f", account.valueInCurrency)}", fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Lifetime", style = MaterialTheme.typography.bodySmall)
                                Text("${account.lifetimeEarned}", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        if (transactions.isNotEmpty()) {
            item {
                Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(transactions) { tx ->
                val dateStr = formatIsoDate(tx.createdAt)
                val sign = if (tx.type == "EARN") "+" else "-"
                val color = if (tx.type == "EARN") Color(0xFF4CAF50) else Color(0xFFF44336)
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(tx.type, fontWeight = FontWeight.SemiBold)
                        Text(dateStr, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        tx.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                    Text("$sign${tx.points}", color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditTab(
    customerId: Int,
    creditAccount: CreditAccountDto?,
    creditTransactions: List<CreditTransactionDto>,
    viewModel: CustomerProfileViewModel
) {
    var showRepaySheet by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (creditAccount == null) {
                Text("No Credit Account Found", color = MaterialTheme.colorScheme.error)
            } else {
                val balanceColor = if (creditAccount.currentBalance > 0) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurfaceVariant
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(24.dp).fillMaxWidth()) {
                        Text("Current Balance Due", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "₹${String.format("%,.2f", creditAccount.currentBalance)}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = balanceColor
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Credit Limit: ₹${String.format("%,.2f", creditAccount.creditLimit)}")
                            Text("Available: ₹${String.format("%,.2f", creditAccount.availableCredit)}")
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { showRepaySheet = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = creditAccount.currentBalance > 0
                        ) {
                            Text("Repay Balance")
                        }
                    }
                }
            }
        }

        if (creditTransactions.isNotEmpty()) {
            item {
                Text("Credit Ledger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(creditTransactions) { tx ->
                val dateStr = formatIsoDate(tx.createdAt)
                val isCredit = tx.type == "CREDIT"
                val sign = if (isCredit) "+" else "-"
                val color = if (isCredit) Color(0xFFF44336) else Color(0xFF4CAF50)
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(if (isCredit) "Purchase on Credit" else "Repayment", fontWeight = FontWeight.SemiBold)
                        Text(dateStr, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        tx.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                    Text("$sign₹${String.format("%.2f", tx.amount)}", color = color, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
            }
        }
    }

    if (showRepaySheet && creditAccount != null) {
        RepaymentBottomSheet(
            maxAmount = creditAccount.currentBalance,
            onDismiss = { showRepaySheet = false },
            onSubmit = { amount, notes ->
                viewModel.submitRepayment(customerId, amount, notes)
                showRepaySheet = false
            }
        )
    }

    val repayState by viewModel.repaymentState.collectAsState()
    if (repayState is NetworkResult.Loading) {
        // Overlay a loader or let it finish quietly depending on UI rules.
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepaymentBottomSheet(
    maxAmount: Double,
    onDismiss: () -> Unit,
    onSubmit: (Double, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf(maxAmount.toString()) }
    var notes by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Text("Receive Repayment", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { 
                    amountText = it
                    val parsed = it.toDoubleOrNull()
                    isError = parsed == null || parsed <= 0 || parsed > maxAmount
                },
                label = { Text("Amount (₹)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = isError,
                supportingText = { if (isError) Text("Invalid amount. Max: $maxAmount") }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { 
                    amountText.toDoubleOrNull()?.let { onSubmit(it, notes) } 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isError && amountText.isNotEmpty()
            ) {
                Text("Confirm Repayment")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun formatIsoDate(isoString: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val date = parser.parse(isoString.substringBefore("."))
        SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(date ?: return isoString)
    } catch (e: Exception) {
        isoString
    }
}
