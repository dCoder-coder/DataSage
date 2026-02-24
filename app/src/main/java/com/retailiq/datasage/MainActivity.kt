package com.retailiq.datasage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.retailiq.datasage.core.ConnectivityObserver
import com.retailiq.datasage.ui.auth.AuthNavHost
import com.retailiq.datasage.ui.auth.AuthViewModel
import com.retailiq.datasage.ui.navigation.MainNavigation
import com.retailiq.datasage.ui.navigation.UserRole
import com.retailiq.datasage.ui.theme.DataSageTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var connectivityObserver: ConnectivityObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = hiltViewModel()
            var authenticated by remember { mutableStateOf(authViewModel.hasToken()) }
            val navController = rememberNavController()

            DataSageTheme {
                if (!authenticated) {
                    AuthNavHost(
                        navController = navController,
                        onFinish = { authenticated = true },
                        viewModel = authViewModel
                    )
                } else {
                    val role = if (authViewModel.role().equals("owner", true)) UserRole.OWNER else UserRole.STAFF
                    MainNavigation(
                        role = role,
                        connectivityObserver = connectivityObserver,
                        onLogout = {
                            authViewModel.logout()
                            authenticated = false
                        }
                    )
                }
            }
        }
    }
}
