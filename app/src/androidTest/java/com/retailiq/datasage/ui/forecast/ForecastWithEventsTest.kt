package com.retailiq.datasage.ui.forecast

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import com.retailiq.datasage.data.api.EventMarkerDto
import com.retailiq.datasage.data.api.ForecastPoint
import com.retailiq.datasage.ui.components.ForecastLineChart
import com.retailiq.datasage.ui.components.HistoricalPoint
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ForecastWithEventsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun forecastLineChart_withEvents_rendersWithoutCrashing() {
        val historical = listOf(
            HistoricalPoint("2024-01-01", 100.0),
            HistoricalPoint("2024-01-02", 110.0)
        )
        val forecast = listOf(
            ForecastPoint("2024-01-03", 120.0, 100.0, 140.0),
            ForecastPoint("2024-01-04", 130.0, 110.0, 150.0)
        )
        val adjusted = listOf(
            ForecastPoint("2024-01-03", 125.0, 100.0, 140.0),
            ForecastPoint("2024-01-04", 150.0, 110.0, 160.0)
        )
        val events = listOf(
            EventMarkerDto("2024-01-04", "Promo", "PROMOTION")
        )

        composeTestRule.setContent {
            ForecastLineChart(
                historical = historical,
                forecast = forecast,
                adjustedForecast = adjusted,
                events = events
            )
        }

        // We can't easily assert exactly how MPAndroidChart renders internally via compose ui-test,
        // but we can assert the AndroidView composable successfully mounts and the chart
        // component doesn't crash given the parameters.
        println(composeTestRule.onRoot().printToString())
        
        // Let's assert the AndroidView container exists. MPAndroidChart doesn't expose test tags directly.
        // If it reaches here without crash, the test passes.
    }
}
