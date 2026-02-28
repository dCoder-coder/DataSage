package com.retailiq.datasage.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.retailiq.datasage.core.ConnectivityObserver
import com.retailiq.datasage.ui.alerts.AlertsScreen
import com.retailiq.datasage.ui.analytics.AnalyticsScreen
import com.retailiq.datasage.ui.common.OfflineBanner
import com.retailiq.datasage.ui.customers.CustomersScreen
import com.retailiq.datasage.ui.dashboard.DashboardScreen
import com.retailiq.datasage.ui.inventory.InventoryScreen
import com.retailiq.datasage.ui.sales.SalesScreen
import com.retailiq.datasage.ui.settings.SettingsScreen

@Composable
fun MainNavigation(
    role: UserRole,
    connectivityObserver: ConnectivityObserver,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val tabs = tabsForRole(role)
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val isOnline by connectivityObserver.isOnline.collectAsState(initial = true)

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
        Column(Modifier.padding(padding)) {
            OfflineBanner(isOffline = !isOnline)
            NavHost(navController, startDestination = "home") {
                composable("home") { DashboardScreen(role = role) }
                composable("sales") { SalesScreen() }
                composable("inventory") { InventoryScreen() }
                composable("analytics") { AnalyticsScreen() }
                composable("suppliers") { 
                    com.retailiq.datasage.ui.supplier.SupplierListScreen(
                        onNavigateToSupplier = { id -> navController.navigate("suppliers/$id") }
                    ) 
                }
                composable("suppliers/{supplierId}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("supplierId")?.toIntOrNull() ?: return@composable
                    com.retailiq.datasage.ui.supplier.SupplierProfileScreen(
                        supplierId = id,
                        onNavigateBack = { navController.popBackStack() },
                        onCreatePo = { supId -> navController.navigate("purchaseorders/create?supplierId=$supId") },
                        onViewAllPos = { supId -> navController.navigate("purchase-orders?supplierId=$supId") }
                    )
                }
                composable("purchase-orders?supplierId={supplierId}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("supplierId")?.toIntOrNull()
                    com.retailiq.datasage.ui.purchaseorder.PurchaseOrderListScreen(
                        supplierId = id,
                        onNavigateBack = { navController.popBackStack() },
                        onCreatePo = { supId -> 
                            val query = if (supId != null) "?supplierId=$supId" else ""
                            navController.navigate("purchaseorders/create$query") 
                        },
                        onNavigateToReceive = { poId -> navController.navigate("purchase-orders/$poId/receive") }
                    )
                }
                composable("purchaseorders/create?supplierId={supplierId}&prefillProductId={prefillProductId}") { backStackEntry ->
                    val supId = backStackEntry.arguments?.getString("supplierId")?.toIntOrNull()
                    val prodId = backStackEntry.arguments?.getString("prefillProductId")?.toIntOrNull()
                    com.retailiq.datasage.ui.purchaseorder.CreatePurchaseOrderScreen(
                        prefillSupplierId = supId,
                        prefillProductId = prodId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("purchase-orders/{poId}/receive") { backStackEntry ->
                    val poId = backStackEntry.arguments?.getString("poId")?.toIntOrNull() ?: return@composable
                    com.retailiq.datasage.ui.purchaseorder.GoodsReceiptScreen(
                        poId = poId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("more") { 
                    SettingsScreen(
                        userRole = role.name,
                        onNavigateToStaffPerformance = { navController.navigate("staff/performance") },
                        onLogout = onLogout
                    ) 
                }
                composable("staff/performance") {
                    com.retailiq.datasage.ui.staff.StaffPerformanceScreen(
                        userRole = role.name,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("customers") { CustomersScreen() }
                composable("alerts") { AlertsScreen(
                    onNavigateToCreatePo = { prodId -> navController.navigate("purchaseorders/create?prefillProductId=$prodId") }
                ) }
            }
        }
    }
}

