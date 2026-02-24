package com.retailiq.datasage.ui.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

data class CustomerItem(
    val id: String = "",
    val name: String = "",
    val mobile: String = "",
    val totalSpent: Double = 0.0
)

sealed class CustomersUiState {
    data object Loading : CustomersUiState()
    data class Loaded(val customers: List<CustomerItem>) : CustomersUiState()
    data class Error(val message: String) : CustomersUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen() {
    // Placeholder - will be connected to ViewModel when customer features are expanded
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Customers") })
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Customer management", style = MaterialTheme.typography.titleMedium)
                Text("Coming in next release", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
