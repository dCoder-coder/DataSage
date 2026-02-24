package com.retailiq.datasage.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.retailiq.datasage.ui.alerts.AlertsScreen
import com.retailiq.datasage.ui.analytics.AnalyticsScreen
import com.retailiq.datasage.ui.customers.CustomersScreen
import com.retailiq.datasage.ui.dashboard.DashboardScreen
import com.retailiq.datasage.ui.inventory.InventoryScreen
import com.retailiq.datasage.ui.sales.SalesScreen
import com.retailiq.datasage.ui.settings.SettingsScreen

@Composable
fun MainNavigation(role: UserRole) {
    val navController = rememberNavController()
    val tabs = tabsForRole(role)
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { DashboardScreen() }
            composable("sales") { SalesScreen() }
            composable("inventory") { InventoryScreen() }
            composable("analytics") { AnalyticsScreen() }
            composable("more") { SettingsScreen() }
            composable("customers") { CustomersScreen() }
            composable("alerts") { AlertsScreen() }
        }
    }
}
