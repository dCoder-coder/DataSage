package com.retailiq.datasage.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.data.model.WhatsAppConfigDto
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppConfigScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLog: () -> Unit,
    viewModel: WhatsAppConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val testMessageState by viewModel.testMessageState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saveState, testMessageState) {
        saveState?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        testMessageState?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WhatsApp Settings") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val state = uiState) {
                is WhatsAppConfigUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is WhatsAppConfigUiState.Error -> {
                    Text("Error loading config: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
                is WhatsAppConfigUiState.Success -> {
                    WhatsAppConfigForm(
                        initialConfig = state.config,
                        onSave = { config -> viewModel.saveConfig(config) },
                        onTestConnection = { viewModel.testConnection() },
                        onNavigateToLog = onNavigateToLog
                    )
                }
            }
        }
    }
}

@Composable
fun WhatsAppConfigForm(
    initialConfig: WhatsAppConfigDto,
    onSave: (WhatsAppConfigDto) -> Unit,
    onTestConnection: () -> Unit,
    onNavigateToLog: () -> Unit
) {
    var phoneNumberId by remember { mutableStateOf(initialConfig.phone_number_id) }
    var accessToken by remember { mutableStateOf(initialConfig.access_token) }
    var webhookVerifyToken by remember { mutableStateOf(initialConfig.webhook_verify_token) }
    var isActive by remember { mutableStateOf(initialConfig.is_active) }
    var accessTokenVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = phoneNumberId ?: "",
            onValueChange = { phoneNumberId = it },
            label = { Text("Phone Number ID") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        OutlinedTextField(
            value = accessToken ?: "",
            onValueChange = { accessToken = it },
            label = { Text("Access Token") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (accessTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { accessTokenVisible = !accessTokenVisible }) {
                    Text(if (accessTokenVisible) "HIDE" else "SHOW")
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = webhookVerifyToken ?: "",
                onValueChange = { webhookVerifyToken = it },
                label = { Text("Webhook Verify Token") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { webhookVerifyToken = UUID.randomUUID().toString() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Auto-generate Token")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Active", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = isActive,
                onCheckedChange = { isActive = it }
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                onSave(
                    WhatsAppConfigDto(
                        phone_number_id = phoneNumberId,
                        access_token = accessToken,
                        waba_id = null,
                        webhook_verify_token = webhookVerifyToken,
                        is_active = isActive
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Configuration")
        }

        OutlinedButton(
            onClick = onTestConnection,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test Connection")
        }

        Text(
            text = "View Message Log",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { onNavigateToLog() }
                .padding(16.dp)
        )
    }
}
