package com.retailiq.datasage.data.local

import com.retailiq.datasage.data.model.OfflineKpisDto
import com.retailiq.datasage.data.model.OfflineLowStockDto
import com.retailiq.datasage.data.model.OfflineRevenueDto
import com.retailiq.datasage.data.model.OfflineTopProductDto
import com.retailiq.datasage.data.model.SnapshotDto
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalKpiEngineTest {

    @Test
    fun `revenueVsYesterday computes percentage growth correctly`() {
        // Given
        val snapshot = SnapshotDto(
            kpis = OfflineKpisDto(
                todayRevenue = 150.0,
                yesterdayRevenue = 100.0,
                todayProfit = 50.0,
                todayTransactions = 10,
                thisWeekRevenue = null,
                thisMonthRevenue = null
            ),
            builtAt = "2026-02-28T12:00:00Z"
        )
        val engine = LocalKpiEngine(snapshot)

        // When
        val pct = engine.revenueVsYesterday()

        // Then expected ((150 - 100) / 100) * 100 = 50.0f
        assertEquals(50.0f, pct, 0.001f)
    }

    @Test
    fun `weeklyRevenueForChart returns exactly 7 items reversed`() {
        // Given
        val history = List(10) { i ->
            OfflineRevenueDto("2026-02-0${i+1}", (i * 10).toDouble(), 0.0)
        }
        val snapshot = SnapshotDto(
            kpis = null,
            revenue30d = history,
            builtAt = "2026-02-28T10:00:00Z"
        )
        val engine = LocalKpiEngine(snapshot)

        // When
        val result = engine.weeklyRevenueForChart()

        // Then
        assertEquals(7, result.size)
        // Since input was 1..10, the first 7 are 1..7. The reversed result should be 7..1
        assertEquals("2026-02-07", result[0].date)
        assertEquals(60f, result[0].revenue, 0.001f)
        assertEquals("2026-02-01", result[6].date)
    }

    @Test
    fun `lowStockCount returns list size`() {
        val snapshot = SnapshotDto(
            kpis = null,
            lowStockProducts = listOf(
                OfflineLowStockDto("p1", "Item 1", 2.0, 5.0),
                OfflineLowStockDto("p2", "Item 2", 1.0, 10.0)
            ),
            builtAt = "2026-02-28T10:00:00Z"
        )
        val engine = LocalKpiEngine(snapshot)

        assertEquals(2, engine.lowStockCount())
    }

    @Test
    fun `topProductThisWeek returns the object with highest revenue or first`() {
        val snapshot = SnapshotDto(
            kpis = null,
            topProducts7d = listOf(
                OfflineTopProductDto("p1", "Best Seller", 500.0, 50),
                OfflineTopProductDto("p2", "Second", 300.0, 30)
            ),
            builtAt = "2026-02-28T10:00:00Z"
        )
        val engine = LocalKpiEngine(snapshot)

        assertEquals("Best Seller", engine.topProductThisWeek())
    }
}
