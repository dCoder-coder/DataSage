package com.retailiq.datasage.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.data.model.LoyaltyProgramSettingsDto
import com.retailiq.datasage.ui.viewmodel.LoyaltySettingsUiState
import com.retailiq.datasage.ui.viewmodel.LoyaltySettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: LoyaltySettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var pointsPerRupee by remember { mutableStateOf("") }
    var redemptionRate by remember { mutableStateOf("") }
    var minRedemptionPoints by remember { mutableStateOf("") }
    var expiryDays by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is LoyaltySettingsUiState.Loaded -> {
                pointsPerRupee = state.settings.pointsPerRupee.toString()
                redemptionRate = state.settings.redemptionRate.toString()
                minRedemptionPoints = state.settings.minRedemptionPoints.toString()
                expiryDays = state.settings.expiryDays.toString()
                isActive = state.settings.isActive
            }
            is LoyaltySettingsUiState.Success -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Settings saved successfully!")
                }
                viewModel.resetStateToLoaded()
            }
            is LoyaltySettingsUiState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Error: ${state.message}")
                }
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Loyalty Programme Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (uiState) {
                is LoyaltySettingsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text("Configure how customers earn and redeem points.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(24.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Programme Active", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Switch(checked = isActive, onCheckedChange = { isActive = it })
                        }
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = pointsPerRupee,
                            onValueChange = { pointsPerRupee = it },
                            label = { Text("Points earned per ₹1 spent") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = redemptionRate,
                            onValueChange = { redemptionRate = it },
                            label = { Text("Value of 1 point (₹)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            supportingText = { Text("e.g. 0.1 means 10 points = 1 Rupee") }
                        )
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = minRedemptionPoints,
                            onValueChange = { minRedemptionPoints = it },
                            label = { Text("Minimum points required to redeem") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = expiryDays,
                            onValueChange = { expiryDays = it },
                            label = { Text("Points expire after (days)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = { Text("0 for never expire") }
                        )
                        Spacer(Modifier.height(32.dp))

                        Button(
                            onClick = {
                                val dto = LoyaltyProgramSettingsDto(
                                    pointsPerRupee = pointsPerRupee.toDoubleOrNull() ?: 0.0,
                                    redemptionRate = redemptionRate.toDoubleOrNull() ?: 0.0,
                                    minRedemptionPoints = minRedemptionPoints.toIntOrNull() ?: 0,
                                    expiryDays = expiryDays.toIntOrNull() ?: 0,
                                    isActive = isActive
                                )
                                viewModel.saveSettings(dto)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState !is LoyaltySettingsUiState.Saving
                        ) {
                            if (uiState is LoyaltySettingsUiState.Saving) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.height(20.dp).padding(end = 8.dp))
                            }
                            Text("Save Settings")
                        }
                    }
                }
            }
        }
    }
}
