package com.retailiq.datasage.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.ui.graphics.vector.ImageVector

enum class UserRole { OWNER, STAFF }

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

fun tabsForRole(role: UserRole): List<BottomNavItem> = if (role == UserRole.OWNER) {
    listOf(
        BottomNavItem("home", "Home", Icons.Default.Dashboard),
        BottomNavItem("sales", "Sales", Icons.Default.PointOfSale),
        BottomNavItem("inventory", "Inventory", Icons.Default.Inventory),
        BottomNavItem("analytics", "Analytics", Icons.Default.Analytics),
        BottomNavItem("more", "More", Icons.Default.Menu)
    )
} else {
    listOf(
        BottomNavItem("home", "Home", Icons.Default.Dashboard),
        BottomNavItem("sales", "Sales", Icons.Default.PointOfSale),
        BottomNavItem("inventory", "Inventory", Icons.Default.Inventory)
    )
}
