package com.retailiq.datasage.ui.analytics

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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.data.api.DashboardPayload
import com.retailiq.datasage.ui.components.CategoryBreakdown
import com.retailiq.datasage.ui.components.CategoryPieChart
import com.retailiq.datasage.ui.components.ContributionBarChart
import com.retailiq.datasage.data.local.AnalyticsSnapshot
import com.retailiq.datasage.data.local.LocalKpiEngine
import com.retailiq.datasage.data.model.SnapshotDto
import com.google.gson.Gson
import com.retailiq.datasage.ui.components.DateRevenuePair
import com.retailiq.datasage.ui.components.RevenueLineChart
import com.retailiq.datasage.data.model.LoyaltyAnalyticsDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateToGstReports: () -> Unit = {},
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val loyaltyAnalytics by viewModel.loyaltyAnalytics.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                actions = {
                    IconButton(onClick = { viewModel.loadAnalytics() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is AnalyticsUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AnalyticsUiState.Loaded -> {
                AnalyticsContent(
                    data = state.data, 
                    categoryBreakdown = categoryBreakdown, 
                    loyaltyAnalytics = loyaltyAnalytics, 
                    onNavigateToGstReports = onNavigateToGstReports,
                    modifier = Modifier.padding(padding)
                )
            }
            is AnalyticsUiState.Offline -> {
                Column(Modifier.padding(padding)) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Offline Mode — showing latest cached trends.", 
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    AnalyticsOfflineContent(state.snapshot, state.kpiEngine)
                }
            }
            is AnalyticsUiState.Error -> {
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.message)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadAnalytics() }) { Text("Retry") }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsContent(
    data: DashboardPayload,
    categoryBreakdown: List<CategoryBreakdown>,
    loyaltyAnalytics: LoyaltyAnalyticsDto?,
    onNavigateToGstReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kpis = data.todayKpis

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Text("Today's KPIs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Revenue", "₹${String.format("%,.0f", kpis.revenue)}", Modifier.weight(1f))
                StatCard("Profit", "₹${String.format("%,.0f", kpis.profit)}", Modifier.weight(1f))
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Transactions", kpis.transactions.toString(), Modifier.weight(1f))
                StatCard("Avg Basket", "₹${String.format("%.0f", kpis.avgBasket)}", Modifier.weight(1f))
            }
        }

        item {
            androidx.compose.material3.OutlinedButton(
                onClick = onNavigateToGstReports,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View GST Reports & GSTR-1")
            }
        }

        if (data.revenue7d.isNotEmpty()) {
            item {
                Text("Revenue Trend (7 days)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        data.revenue7d.forEach { point ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(point.date, style = MaterialTheme.typography.bodySmall)
                                Text("₹${String.format("%,.0f", point.revenue)}", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        // Alerts summary
        if (data.alertsSummary.isNotEmpty()) {
            item {
                Text("Alerts Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.alertsSummary.forEach { (level, count) ->
                        StatCard(level.replaceFirstChar { it.uppercase() }, count.toString(), Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Text("Category Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                CategoryPieChart(data = categoryBreakdown, modifier = Modifier.padding(16.dp))
            }
        }

        item {
            Text("Product Contribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
             Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                // Mocking Contribution data
                val contributionData = data.topProductsToday.map {
                    CategoryBreakdown(it.name, it.revenue)
                }
                ContributionBarChart(data = contributionData.take(5), modifier = Modifier.padding(16.dp))
            }
        }

        if (data.topProductsToday.isNotEmpty()) {
            item {
                Text("Top Products Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(data.topProductsToday) { product ->
                        Card(shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(product.name, fontWeight = FontWeight.Medium)
                                Text("₹${String.format("%,.0f", product.revenue)}", style = MaterialTheme.typography.bodySmall)
                                Text("Sold: ${product.unitsSold.toInt()}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        if (loyaltyAnalytics != null) {
            item {
                Text("Loyalty & Rewards", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Enrolled Customers", style = MaterialTheme.typography.labelMedium)
                                Text("${loyaltyAnalytics.enrolledCustomers}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Redemption Rate", style = MaterialTheme.typography.labelMedium)
                                Text("${String.format("%.1f", loyaltyAnalytics.redemptionRatePercent)}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Points Issued This Month:", style = MaterialTheme.typography.bodySmall)
                            Text("${loyaltyAnalytics.pointsIssuedThisMonth}", fontWeight = FontWeight.SemiBold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Active Liability Value:", style = MaterialTheme.typography.bodySmall)
                            Text("₹${String.format("%,.0f", loyaltyAnalytics.activeLiability)}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (data.insights.isNotEmpty()) {
            item {
                Text("Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(data.insights) { insight ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(insight.type.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(insight.title, fontWeight = FontWeight.SemiBold)
                        Text(insight.body, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AnalyticsOfflineContent(snapshot: AnalyticsSnapshot, kpiEngine: LocalKpiEngine, modifier: Modifier = Modifier) {
    val dto = Gson().fromJson(snapshot.snapshotJson, SnapshotDto::class.java)
    
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item { Text("Offline Analytics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val revVsYest = kpiEngine.revenueVsYesterday()
                val growthStr = if (revVsYest >= 0) "+${String.format("%.1f", revVsYest)}%" else "${String.format("%.1f", revVsYest)}%"
                StatCard("Revenue Growth", growthStr, Modifier.weight(1f))
                StatCard("Low Stock Items", kpiEngine.lowStockCount().toString(), Modifier.weight(1f))
            }
        }
        
        val revChartData = kpiEngine.weeklyRevenueForChart().map {
            com.retailiq.datasage.ui.components.DateRevenuePair(it.date, it.revenue.toDouble())
        }
        if (revChartData.isNotEmpty()) {
            item { Text("Revenue Trend (7 days)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    RevenueLineChart(data = revChartData, modifier = Modifier.padding(16.dp))
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
