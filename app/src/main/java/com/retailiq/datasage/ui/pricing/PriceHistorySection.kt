package com.retailiq.datasage.ui.pricing

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.retailiq.datasage.data.api.PriceHistoryEntry
import java.text.SimpleDateFormat
import java.util.Locale

// ── Price History Section (expandable) ───────────────────────────────────────

@Composable
fun PriceHistorySection(
    productId: Int,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(productId) { viewModel.loadPriceHistory(productId) }

    val priceHistoryState by viewModel.priceHistoryState.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header row (clickable → toggle expanded)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Price History", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                    when (val state = priceHistoryState) {
                        is PriceHistoryState.Loading -> {
                            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            }
                        }

                        is PriceHistoryState.Error -> {
                            Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        is PriceHistoryState.Loaded -> {
                            if (state.entries.isEmpty()) {
                                Text("No price history available.", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                // Line chart
                                PriceHistoryChart(entries = state.entries)
                                Spacer(Modifier.height(12.dp))
                                // Last 5 text entries
                                val last5 = state.entries.takeLast(5).reversed()
                                last5.forEachIndexed { idx, entry ->
                                    if (idx > 0) {
                                        val prev = last5.getOrNull(idx - 1)
                                        if (prev != null) {
                                            PriceChangeRow(old = prev, new = entry)
                                        }
                                    } else if (idx == 0 && last5.size == 1) {
                                        Text(
                                            "₹${String.format("%.2f", entry.sellingPrice)} on ${formatDate(entry.changedAt)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                // Show oldest→newest transitions
                                val all = state.entries
                                if (all.size >= 2) {
                                    val transitions = all.zipWithNext().takeLast(5).reversed()
                                    transitions.forEach { (old, new) -> PriceChangeRow(old = old, new = new) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceChangeRow(old: PriceHistoryEntry, new: PriceHistoryEntry) {
    Text(
        text = "₹${String.format("%.2f", old.sellingPrice)} → ₹${String.format("%.2f", new.sellingPrice)} on ${formatDate(new.changedAt)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

private fun formatDate(iso: String): String {
    return try {
        val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val parsed = isoFmt.parse(iso)
        if (parsed != null) outFmt.format(parsed) else iso
    } catch (_: Exception) { iso }
}

// ── PriceHistoryChart ────────────────────────────────────────────────────────

@Composable
fun PriceHistoryChart(
    entries: List<PriceHistoryEntry>,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        Box(modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
            Text("No data yet", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        return
    }

    val tealColorInt = AndroidColor.parseColor("#009688")
    val labels = entries.map { it.changedAt.take(10) } // YYYY-MM-DD labels

    AndroidView(
        modifier = modifier.fillMaxWidth().height(180.dp),
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(false)
                setPinchZoom(false)
                setViewPortOffsets(40f, 10f, 10f, 40f)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val idx = value.toInt()
                            return if (idx in labels.indices) labels[idx].drop(5) else "" // MM-DD
                        }
                    }
                    textSize = 9f
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String = "₹${value.toInt()}"
                    }
                    textSize = 9f
                }
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val chartEntries = entries.mapIndexed { idx, entry ->
                Entry(idx.toFloat(), entry.sellingPrice.toFloat())
            }
            val dataSet = LineDataSet(chartEntries, "Price").apply {
                color = tealColorInt
                lineWidth = 2f
                circleRadius = 3f
                setCircleColor(tealColorInt)
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = AndroidColor.parseColor("#20009688")
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        },
        onReset = { it.clear() }
    )
}

// ── State ─────────────────────────────────────────────────────────────────────

sealed class PriceHistoryState {
    data object Loading : PriceHistoryState()
    data class Loaded(val entries: List<PriceHistoryEntry>) : PriceHistoryState()
    data class Error(val message: String) : PriceHistoryState()
}
