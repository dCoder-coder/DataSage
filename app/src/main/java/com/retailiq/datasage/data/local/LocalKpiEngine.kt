package com.retailiq.datasage.data.local

import com.retailiq.datasage.data.model.SnapshotDto

data class DateRevenuePair(val date: String, val revenue: Float)

class LocalKpiEngine(private val snapshot: SnapshotDto) {

    fun revenueVsYesterday(): Float {
        val today = snapshot.kpis?.todayRevenue ?: 0.0
        val yesterday = snapshot.kpis?.yesterdayRevenue ?: 0.0
        if (yesterday == 0.0) return if (today > 0.0) 100f else 0f
        return (((today - yesterday) / yesterday) * 100).toFloat()
    }

    fun weeklyRevenueForChart(): List<DateRevenuePair> {
        return snapshot.revenue30d
            ?.take(7)
            ?.map {
                DateRevenuePair(
                    date = it.date ?: "",
                    revenue = (it.revenue ?: 0.0).toFloat()
                )
            }
            ?.reversed() ?: emptyList() // reversed to present oldest first in charts
    }

    fun lowStockCount(): Int {
        return snapshot.lowStockProducts?.size ?: 0
    }

    fun topProductThisWeek(): String {
        return snapshot.topProducts7d
            ?.firstOrNull()
            ?.name ?: "N/A"
    }
}
