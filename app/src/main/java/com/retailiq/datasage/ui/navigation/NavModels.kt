package com.retailiq.datasage.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.ui.graphics.vector.ImageVector

enum class UserRole { OWNER, STAFF, VIEWER }

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

fun tabsForRole(role: UserRole, isChainOwner: Boolean = false): List<BottomNavItem> {
    val ownerTabs = mutableListOf(
        BottomNavItem("home", "Home", Icons.Default.Dashboard),
        BottomNavItem("sales", "Sales", Icons.Default.PointOfSale),
        BottomNavItem("inventory", "Inventory", Icons.Default.Inventory),
        BottomNavItem("suppliers", "Suppliers", Icons.Outlined.LocalShipping),
        BottomNavItem("analytics", "Analytics", Icons.Default.Analytics),
        BottomNavItem("pricing/suggestions", "Pricing", Icons.Default.PriceChange),
        BottomNavItem("more", "More", Icons.Default.Menu)
    )
    if (isChainOwner) {
        ownerTabs.add(BottomNavItem("chain/dashboard", "Chain", Icons.Outlined.AccountTree))
    }
    return if (role == UserRole.OWNER) ownerTabs
    else listOf(
        BottomNavItem("home", "Home", Icons.Default.Dashboard),
        BottomNavItem("sales", "Sales", Icons.Default.PointOfSale),
        BottomNavItem("inventory", "Inventory", Icons.Default.Inventory)
    )
}
