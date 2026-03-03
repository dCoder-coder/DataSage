package com.retailiq.datasage.ui.forecast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.ui.components.ForecastLineChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(viewModel: ForecastViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadDemandSensing(1) // Load demand sensing with dummy product ID 1
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Demand Forecast") },
                actions = {
                    IconButton(onClick = { viewModel.loadDemandSensing(1) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ForecastUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ForecastUiState.Loaded -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Revenue Prediction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        val chipColor = when (state.confidenceTier) {
                            "Prophet (high)" -> Color(0xFF4CAF50)
                            "Ridge (medium)" -> Color(0xFFFFC107)
                            else -> Color(0xFFF44336)
                        }

                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text(state.confidenceTier, color = Color.White) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = chipColor)
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        ForecastLineChart(
                            historical = state.historical,
                            forecast = state.forecast,
                            modifier = Modifier.padding(16.dp),
                            adjustedForecast = state.adjustedForecast,
                            events = state.events
                        )
                    }
                }
            }
            is ForecastUiState.Error -> {
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.message)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadDemandSensing(1) }) { Text("Retry") }
                }
            }
        }
    }
}
