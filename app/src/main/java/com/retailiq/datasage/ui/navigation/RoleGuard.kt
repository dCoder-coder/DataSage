package com.retailiq.datasage.ui.navigation

import androidx.compose.runtime.Composable

@Composable
fun RoleGuard(role: String, requiredRole: String, content: @Composable () -> Unit) {
    if (role.equals(requiredRole, ignoreCase = true)) content()
}
