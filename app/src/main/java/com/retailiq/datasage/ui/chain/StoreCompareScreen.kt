package com.retailiq.datasage.ui.chain

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.data.model.CompareRowDto
import com.retailiq.datasage.data.model.StoreCompareResponseDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreCompareScreen(
    viewModel: StoreCompareViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Store Comparison", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("← Back") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            // Period selector chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                viewModel.periods.zip(viewModel.periodLabels).forEach { (period, label) ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { viewModel.selectPeriod(period) },
                        label = { Text(label) }
                    )
                }
            }

            when (val state = uiState) {
                is StoreCompareUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is StoreCompareUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
                is StoreCompareUiState.Loaded -> CompareTable(state.data)
            }
        }
    }
}

@Composable
private fun CompareTable(data: StoreCompareResponseDto) {
    val cellWidth = 100.dp
    val labelWidth = 140.dp

    Box(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            // Header row
            Row {
                Box(Modifier.width(labelWidth).padding(4.dp)) {
                    Text("KPI", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
                data.stores.forEach { store ->
                    Box(Modifier.width(cellWidth).padding(4.dp)) {
                        Text(store, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    }
                }
            }
            HorizontalDivider()

            // Data rows
            data.rows.forEachIndexed { rowIdx, row ->
                val rowBg = if (rowIdx % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                Row(Modifier.background(rowBg)) {
                    Box(Modifier.width(labelWidth).padding(6.dp)) {
                        Text(row.kpi, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                    row.values.forEach { cell ->
                        val cellBg = when (cell.relative_to_avg) {
                            "above" -> Color(0xFFE8F5E9) // green
                            "near" -> Color(0xFFFFF9C4)  // amber
                            "below" -> Color(0xFFFFEBEE) // red
                            else -> Color.Transparent
                        }
                        Box(
                            Modifier
                                .width(cellWidth)
                                .padding(4.dp)
                                .background(cellBg, RoundedCornerShape(4.dp))
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "%.1f".format(cell.value),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}
