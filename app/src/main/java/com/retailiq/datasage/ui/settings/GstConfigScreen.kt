package com.retailiq.datasage.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.ui.viewmodel.GstConfigUiState
import com.retailiq.datasage.ui.viewmodel.GstConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GstConfigScreen(
    onNavigateBack: () -> Unit,
    viewModel: GstConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val gstEnabled by viewModel.gstEnabled.collectAsState()
    val registrationType by viewModel.registrationType.collectAsState()
    val stateCode by viewModel.stateCode.collectAsState()
    val gstin by viewModel.gstin.collectAsState()
    val isValidGstin by viewModel.isValidGstin.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is GstConfigUiState.Success) {
            val msg = (uiState as GstConfigUiState.Success).message
            if (msg != null) {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearMessage()
            }
        } else if (uiState is GstConfigUiState.Error) {
            snackbarHostState.showSnackbar((uiState as GstConfigUiState.Error).message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GST Configuration") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState is GstConfigUiState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Enable Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable GST Computation", style = MaterialTheme.typography.titleMedium)
                Switch(checked = gstEnabled, onCheckedChange = { viewModel.updateGstEnabled(it) })
            }

            if (gstEnabled && gstin.isBlank()) {
                Text(
                    text = "Enter your GSTIN to enable GST computation.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Divider()

            // Registration Type Segmented Control Equivalent
            Text("Registration Type", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val types = listOf("REGULAR", "COMPOSITION", "UNREGISTERED")
                types.forEach { type ->
                    FilterChip(
                        selected = registrationType == type,
                        onClick = { viewModel.updateRegistrationType(type) },
                        label = { Text(type.lowercase().capitalize()) }
                    )
                }
            }

            // State Code String dropdown mapped
            OutlinedTextField(
                value = stateCode,
                onValueChange = { if (it.length <= 2) viewModel.updateStateCode(it) },
                label = { Text("State Code (01-37)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true
            )

            // GSTIN Field
            OutlinedTextField(
                value = gstin,
                onValueChange = { if (it.length <= 15) viewModel.updateGstin(it) },
                label = { Text("GSTIN") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused) {
                            viewModel.validateGstinFormat(gstin)
                        }
                    },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                placeholder = { Text("22AAAAA0000A1Z5") },
                isError = isValidGstin == false,
                trailingIcon = {
                    if (isValidGstin == true) {
                        Icon(Icons.Default.CheckCircle, "Valid", tint = Color(0xFF4CAF50))
                    } else if (isValidGstin == false) {
                        Icon(Icons.Default.Error, "Invalid Format", tint = MaterialTheme.colorScheme.error)
                    }
                },
                supportingText = {
                    if (isValidGstin == false) {
                        Text("Invalid format. Must be 15 characters (e.g., 22AAAAA0000A1Z5)")
                    } else {
                        Text("15-character Goods and Services Tax Identification Number")
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.saveConfig() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is GstConfigUiState.Loading
            ) {
                Text("Save GST Configuration")
            }
        }
    }
}
