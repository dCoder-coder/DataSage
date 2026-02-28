package com.retailiq.datasage.ui.components

import com.retailiq.datasage.data.api.ForecastPoint

data class DateRevenuePair(
    val date: String,
    val revenue: Double
)

data class HistoricalPoint(
    val date: String,
    val revenue: Double
)

data class CategoryBreakdown(
    val category: String,
    val value: Double
)

data class PaymentModeBreakdown(
    val mode: String,
    val amount: Double
)
