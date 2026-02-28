package com.retailiq.datasage.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.retailiq.datasage.data.api.ForecastPoint

// Helper: Custom Font for Charts
fun getChartTypeface(context: Context): Typeface {
    return try {
        Typeface.SERIF // Fallback since we don't have the explicit font asset, but Times New Roman or serif is requested
    } catch (e: Exception) {
        Typeface.DEFAULT
    }
}

// 1. RevenueLineChart
@Composable
fun RevenueLineChart(data: List<DateRevenuePair>, modifier: Modifier = Modifier, isThumbnail: Boolean = false) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder(modifier, "No data yet")
        return
    }

    val context = LocalContext.current
    val typeface = remember { getChartTypeface(context) }
    val textColor = MaterialTheme.colorScheme.onSurface.run { Color.argb(255, (red*255).toInt(), (green*255).toInt(), (blue*255).toInt()) }
    val navyColor = android.graphics.Color.parseColor("#1A237E")

    AndroidView(
        modifier = modifier.fillMaxWidth().height(if (isThumbnail) 180.dp else 280.dp),
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(!isThumbnail)
                isDragEnabled = !isThumbnail
                setScaleEnabled(!isThumbnail)
                setPinchZoom(false)
                
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    this.typeface = typeface
                    this.textColor = textColor
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val index = value.toInt()
                            return if (index in data.indices) data[index].date else ""
                        }
                    }
                }
                
                axisLeft.apply {
                    this.typeface = typeface
                    this.textColor = textColor
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return if (value >= 1000) "₹${(value / 1000).toInt()}K" else "₹${value.toInt()}"
                        }
                    }
                }
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val entries = data.mapIndexed { index, pair -> Entry(index.toFloat(), pair.revenue.toFloat()) }
            val dataSet = LineDataSet(entries, "Revenue").apply {
                color = navyColor
                lineWidth = 2f
                setDrawCircles(!isThumbnail)
                setDrawValues(!isThumbnail)
                setCircleColor(navyColor)
                this.valueTypeface = typeface
                this.valueTextColor = textColor
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        },
        onReset = { chart ->
            chart.clear()
        }
    )
}

// Pure function for extracting Forecast datasets for tests
fun buildForecastDataSets(
    historical: List<HistoricalPoint>,
    forecast: List<ForecastPoint>,
    navyColor: Int,
    tealColor: Int,
    bandColor: Int = try { Color.parseColor("#801A237E") } catch (_: Exception) { 0x801A237E.toInt() },
    bandFillColor: Int = try { Color.parseColor("#401A237E") } catch (_: Exception) { 0x401A237E.toInt() }
): List<ILineDataSet> {
    val dataSets = mutableListOf<ILineDataSet>()
    
    val histEntries = historical.mapIndexed { index, pt -> Entry(index.toFloat(), pt.revenue.toFloat()) }
    if (histEntries.isNotEmpty()) {
        val histSet = LineDataSet(histEntries, "Actual").apply {
            color = tealColor
            lineWidth = 2f
            setDrawCircles(true)
            setCircleColor(tealColor)
            setDrawValues(false)
        }
        dataSets.add(histSet)
    }

    val offset = historical.size
    val boundsEntries = mutableListOf<Entry>()
    val forecastEntries = forecast.mapIndexed { index, pt -> 
        val x = (offset + index).toFloat()
        
        if (pt.lowerBound > 0 || pt.upperBound > 0) {
            boundsEntries.add(Entry(x, pt.upperBound.toFloat()))
        }
        
        Entry(x, pt.forecastMean.toFloat())
    }

    if (forecastEntries.isNotEmpty()) {
        val foreSet = LineDataSet(forecastEntries, "Forecast").apply {
            color = navyColor
            lineWidth = 2f
            enableDashedLine(10f, 5f, 0f)
            setDrawCircles(true)
            setCircleColor(navyColor)
            setDrawValues(false)
        }
        dataSets.add(foreSet)
    }
    
    if (boundsEntries.isNotEmpty()) {
        val boundsSet = LineDataSet(boundsEntries, "Confidence Band").apply {
            color = bandColor
            setDrawFilled(true)
            fillColor = bandFillColor
            lineWidth = 0f
            setDrawCircles(false)
            setDrawValues(false)
        }
        dataSets.add(boundsSet)
    }

    return dataSets
}

// 2. ForecastLineChart
@Composable
fun ForecastLineChart(historical: List<HistoricalPoint>, forecast: List<ForecastPoint>, modifier: Modifier = Modifier) {
    if (historical.isEmpty() && forecast.isEmpty()) {
        EmptyChartPlaceholder(modifier, "No data yet")
        return
    }

    val context = LocalContext.current
    val typeface = remember { getChartTypeface(context) }
    val textColor = MaterialTheme.colorScheme.onSurface.run { Color.argb(255, (red*255).toInt(), (green*255).toInt(), (blue*255).toInt()) }
    val navyColor = android.graphics.Color.parseColor("#1A237E")
    val tealColor = android.graphics.Color.parseColor("#009688")

    AndroidView(
        modifier = modifier.fillMaxWidth().height(280.dp),
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                legend.apply {
                    isEnabled = true
                    this.typeface = typeface
                    this.textColor = textColor
                }
                
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    this.typeface = typeface
                    this.textColor = textColor
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val index = value.toInt()
                            return if (index < historical.size) {
                                historical.getOrNull(index)?.date ?: ""
                            } else {
                                forecast.getOrNull(index - historical.size)?.date ?: ""
                            }
                        }
                    }
                }
                
                axisLeft.apply {
                    this.typeface = typeface
                    this.textColor = textColor
                }
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val dataSets = buildForecastDataSets(historical, forecast, navyColor, tealColor)
            dataSets.forEach { 
                if (it is LineDataSet) {
                    it.valueTypeface = typeface
                    it.valueTextColor = textColor
                }
            }
            chart.data = LineData(dataSets)
            chart.invalidate()
        },
        onReset = { it.clear() }
    )
}

// 3. CategoryPieChart
@Composable
fun CategoryPieChart(data: List<CategoryBreakdown>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder(modifier, "No data yet")
        return
    }

    val context = LocalContext.current
    val typeface = remember { getChartTypeface(context) }
    val textColor = MaterialTheme.colorScheme.onSurface.run { Color.argb(255, (red*255).toInt(), (green*255).toInt(), (blue*255).toInt()) }

    AndroidView(
        modifier = modifier.fillMaxWidth().height(280.dp),
        factory = { ctx ->
            PieChart(ctx).apply {
                description.isEnabled = false
                isDrawHoleEnabled = true
                holeRadius = 50f
                transparentCircleRadius = 55f
                setHoleColor(Color.TRANSPARENT)
                
                legend.apply {
                    isEnabled = true
                    this.typeface = typeface
                    this.textColor = textColor
                    isWordWrapEnabled = true
                }
            }
        },
        update = { chart ->
            val total = data.sumOf { it.value }
            val sorted = data.sortedByDescending { it.value }
            
            val displayData = if (sorted.size > 6) {
                val top5 = sorted.take(5)
                val otherSum = sorted.drop(5).sumOf { it.value }
                top5 + CategoryBreakdown("Other", otherSum)
            } else {
                sorted
            }

            val entries = displayData.map { com.github.mikephil.charting.data.PieEntry(it.value.toFloat(), it.category) }
            val dataSet = com.github.mikephil.charting.data.PieDataSet(entries, "").apply {
                setColors(listOf(
                    Color.parseColor("#1A237E"),
                    Color.parseColor("#009688"),
                    Color.parseColor("#5C6BC0"),
                    Color.parseColor("#4DB6AC"),
                    Color.parseColor("#3949AB"),
                    Color.parseColor("#80CBC4")
                ))
                sliceSpace = 3f
                valueTextColor = Color.WHITE
                valueTextSize = 12f
                this.valueTypeface = typeface
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val percentage = (value / total.toFloat()) * 100
                        return if (percentage > 5f) "${percentage.toInt()}%" else ""
                    }
                }
            }
            chart.data = com.github.mikephil.charting.data.PieData(dataSet)
            chart.invalidate()
        },
        onReset = { it.clear() }
    )
}

// 4. PaymentModeBarChart
@Composable
fun PaymentModeBarChart(data: List<PaymentModeBreakdown>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder(modifier, "No data yet")
        return
    }

    val context = LocalContext.current
    val typeface = remember { getChartTypeface(context) }
    val textColor = MaterialTheme.colorScheme.onSurface.run { Color.argb(255, (red*255).toInt(), (green*255).toInt(), (blue*255).toInt()) }
    val navyColor = android.graphics.Color.parseColor("#1A237E")

    AndroidView(
        modifier = modifier.fillMaxWidth().height(280.dp),
        factory = { ctx ->
            HorizontalBarChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setDrawValueAboveBar(true)
                
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    this.typeface = typeface
                    this.textColor = textColor
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val index = value.toInt()
                            return if (index in data.indices) data[index].mode else ""
                        }
                    }
                }
                
                axisLeft.apply {
                    this.typeface = typeface
                    this.textColor = textColor
                    axisMinimum = 0f
                }
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val entries = data.mapIndexed { index, item -> BarEntry(index.toFloat(), item.amount.toFloat()) }
            val dataSet = BarDataSet(entries, "Payment Modes").apply {
                color = navyColor
                this.valueTypeface = typeface
                this.valueTextColor = textColor
            }
            chart.data = BarData(dataSet)
            chart.invalidate()
        },
        onReset = { it.clear() }
    )
}

// 5. StockTrendMiniChart
@Composable
fun StockTrendMiniChart(data: List<DateRevenuePair>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder(modifier, "No data yet")
        return
    }
    val navyColor = android.graphics.Color.parseColor("#1A237E")

    AndroidView(
        modifier = modifier.fillMaxWidth().height(180.dp),
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(false)
                xAxis.isEnabled = false
                axisLeft.isEnabled = false
                axisRight.isEnabled = false
                setViewPortOffsets(0f, 0f, 0f, 0f)
            }
        },
        update = { chart ->
            val entries = data.mapIndexed { index, pair -> Entry(index.toFloat(), pair.revenue.toFloat()) }
            val dataSet = LineDataSet(entries, "").apply {
                color = navyColor
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = Color.parseColor("#401A237E")
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        },
        onReset = { it.clear() }
    )
}

// 6. ContributionBarChart
@Composable
fun ContributionBarChart(data: List<CategoryBreakdown>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder(modifier, "No data yet")
        return
    }

    val context = LocalContext.current
    val typeface = remember { getChartTypeface(context) }
    val textColor = MaterialTheme.colorScheme.onSurface.run { Color.argb(255, (red*255).toInt(), (green*255).toInt(), (blue*255).toInt()) }
    val tealColor = android.graphics.Color.parseColor("#009688")

    AndroidView(
        modifier = modifier.fillMaxWidth().height(280.dp),
        factory = { ctx ->
            BarChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    this.typeface = typeface
                    this.textColor = textColor
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val index = value.toInt()
                            return if (index in data.indices) data[index].category else ""
                        }
                    }
                }
                
                axisLeft.apply {
                    this.typeface = typeface
                    this.textColor = textColor
                    axisMinimum = 0f
                }
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val entries = data.mapIndexed { index, item -> BarEntry(index.toFloat(), item.value.toFloat()) }
            val dataSet = BarDataSet(entries, "Contribution").apply {
                color = tealColor
                this.valueTypeface = typeface
                this.valueTextColor = textColor
            }
            chart.data = BarData(dataSet)
            chart.invalidate()
        },
        onReset = { it.clear() }
    )
}

@Composable
fun EmptyChartPlaceholder(modifier: Modifier = Modifier, text: String) {
    Box(modifier = modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// Previews
@Preview(showBackground = true)
@Composable
fun PreviewRevenueLineChart() {
    val data = listOf(
        DateRevenuePair("Mon", 1200.0),
        DateRevenuePair("Tue", 1500.0),
        DateRevenuePair("Wed", 1000.0),
        DateRevenuePair("Thu", 2000.0),
    )
    RevenueLineChart(data = data)
}

@Preview(showBackground = true)
@Composable
fun PreviewForecastLineChart() {
    val hist = listOf(
        HistoricalPoint("Jan", 100.0), HistoricalPoint("Feb", 150.0)
    )
    val fore = listOf(
        ForecastPoint("Mar", 160.0, 140.0, 180.0), ForecastPoint("Apr", 170.0, 150.0, 200.0)
    )
    ForecastLineChart(historical = hist, forecast = fore)
}

@Preview(showBackground = true)
@Composable
fun PreviewCategoryPieChart() {
    val data = listOf(
        CategoryBreakdown("Beverages", 400.0),
        CategoryBreakdown("Snacks", 300.0),
        CategoryBreakdown("Produce", 100.0),
        CategoryBreakdown("Meat", 50.0),
        CategoryBreakdown("Dairy", 20.0),
        CategoryBreakdown("Bakery", 15.0),
        CategoryBreakdown("Other", 5.0)
    )
    CategoryPieChart(data = data)
}

@Preview(showBackground = true)
@Composable
fun PreviewPaymentModeBarChart() {
    val data = listOf(
        PaymentModeBreakdown("Cash", 1000.0),
        PaymentModeBreakdown("Card", 2500.0),
        PaymentModeBreakdown("UPI", 1500.0)
    )
    PaymentModeBarChart(data = data)
}

@Preview(showBackground = true)
@Composable
fun PreviewStockTrendMiniChart() {
    val data = listOf(
        DateRevenuePair("1", 10.0), DateRevenuePair("2", 15.0), DateRevenuePair("3", 12.0)
    )
    StockTrendMiniChart(data = data)
}

@Preview(showBackground = true)
@Composable
fun PreviewContributionBarChart() {
    val data = listOf(
        CategoryBreakdown("Store A", 40.0),
        CategoryBreakdown("Store B", 30.0)
    )
    ContributionBarChart(data = data)
}
