package com.retailiq.datasage.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.retailiq.datasage.data.api.TopProduct
import com.retailiq.datasage.ui.worker.SyncStatusBar

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val data by viewModel.state.collectAsState()
    val payload = data ?: return Text("Loading dashboard...")

    LazyColumn(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (payload.critical_count > 0 || payload.high_count > 0) {
            item {
                Text(
                    "${payload.critical_count} critical alerts require attention",
                    modifier = Modifier.fillMaxWidth().background(if (payload.critical_count > 0) Color.Red else Color(0xFFFF9800)).padding(8.dp),
                    color = Color.White
                )
            }
        }
        item { SyncStatusBar(payload.sync_pending, payload.sync_failed) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard("Revenue", payload.total_revenue.toString(), Modifier.weight(1f))
                KpiCard("Profit", payload.gross_profit.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard("Transactions", payload.transactions.toString(), Modifier.weight(1f))
                KpiCard("Avg Basket", payload.avg_basket.toString(), Modifier.weight(1f))
            }
        }
        item {
            AndroidView(factory = { ctx -> BarChart(ctx) }, modifier = Modifier.fillMaxWidth().height(220.dp), update = { chart ->
                val entries = payload.trend.mapIndexed { i, t -> BarEntry(i.toFloat(), t.revenue) }
                chart.data = BarData(BarDataSet(entries, "Revenue Trend"))
                chart.invalidate()
            })
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(payload.insights) { insight -> Card { Column(Modifier.padding(10.dp)) { Text(insight.type); Text(insight.headline); Button(onClick = {}) { Text("View detail") } } } }
            }
        }
        item {
            Text("Top Products Today", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(payload.top_products) { top -> ProductPill(top) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}) { Text("New Sale") }
                Button(onClick = {}) { Text("Add Stock") }
                Button(onClick = {}) { Text("View Alerts") }
                Button(onClick = {}) { Text("Reports") }
            }
        }
    }
}

@Composable
private fun KpiCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) { Column(Modifier.padding(12.dp)) { Text(label); Text(value, style = MaterialTheme.typography.titleLarge) } }
}

@Composable
private fun ProductPill(item: TopProduct) { Card { Column(Modifier.padding(10.dp)) { Text(item.name); Text("₹${item.revenue}") } } }
