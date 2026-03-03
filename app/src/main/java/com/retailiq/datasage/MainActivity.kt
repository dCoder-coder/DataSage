package com.retailiq.datasage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.retailiq.datasage.core.AuthEvent
import com.retailiq.datasage.core.AuthEventBus
import com.retailiq.datasage.core.ConnectivityObserver
import com.retailiq.datasage.core.GlobalErrorHandler
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
    @Inject lateinit var authEventBus: AuthEventBus
    @Inject lateinit var globalErrorHandler: GlobalErrorHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = hiltViewModel()
            var authenticated by remember { mutableStateOf(authViewModel.hasToken()) }
            val navController = rememberNavController()
            val snackbarHostState = remember { SnackbarHostState() }

            // Observe global auth events (Logout / SessionExpired)
            LaunchedEffect(Unit) {
                authEventBus.events.collect { event ->
                    when (event) {
                        is AuthEvent.Logout, is AuthEvent.SessionExpired -> {
                            authenticated = false
                        }
                    }
                }
            }

            // Observe global snackbar events from GlobalErrorHandler
            LaunchedEffect(Unit) {
                globalErrorHandler.snackbarEvents.collect { message ->
                    snackbarHostState.showSnackbar(message)
                }
            }

            DataSageTheme {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    if (!authenticated) {
                        AuthNavHost(
                            navController = navController,
                            onFinish = { authenticated = true },
                            viewModel = authViewModel
                        )
                    } else {
                        val roleStr = authViewModel.role()?.lowercase()
                        val role = when (roleStr) {
                            "owner" -> UserRole.OWNER
                            "viewer" -> UserRole.VIEWER
                            else -> UserRole.STAFF
                        }
                        val isChainOwner = authViewModel.isChainOwner()
                        MainNavigation(
                            role = role,
                            connectivityObserver = connectivityObserver,
                            isChainOwner = isChainOwner,
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
}

