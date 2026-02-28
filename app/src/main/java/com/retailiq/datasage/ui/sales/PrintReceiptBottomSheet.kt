package com.retailiq.datasage.ui.sales

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retailiq.datasage.ui.viewmodel.PrintJobUiState
import com.retailiq.datasage.ui.viewmodel.ReceiptsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintReceiptBottomSheet(
    transactionId: String,
    receiptsViewModel: ReceiptsViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val printJobState by receiptsViewModel.printJobState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var hasBluetoothPermission by remember { mutableStateOf(false) }
    var bondedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var selectedMacAddress by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.values.all { it }
        hasBluetoothPermission = allGranted
        if (allGranted) {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            try {
                bondedDevices = adapter?.bondedDevices?.toList() ?: emptyList()
            } catch (e: SecurityException) {
                // Ignore, should not happen since we just got permissions
            }
        }
    }

    LaunchedEffect(Unit) {
        receiptsViewModel.resetPrintJob()
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH)
        }
        permissionLauncher.launch(permissions)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Print Receipt?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Printer Selector
            if (hasBluetoothPermission) {
                if (bondedDevices.isEmpty()) {
                    Text("No bonded bluetooth printers found.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(
                        modifier = Modifier.height(150.dp)
                    ) {
                        items(bondedDevices, key = { it.address }) { device ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (printJobState == PrintJobUiState.Idle) {
                                            selectedMacAddress = device.address
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedMacAddress == device.address,
                                    onClick = {
                                        if (printJobState == PrintJobUiState.Idle) {
                                            selectedMacAddress = device.address
                                        }
                                    },
                                    enabled = printJobState == PrintJobUiState.Idle
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.Bluetooth, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        try { device.name ?: "Unknown Device" } catch (e: SecurityException) { "Unknown Device" },
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(device.address, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            } else {
                Text("Bluetooth permissions are required to print.", style = MaterialTheme.typography.bodyMedium)
            }

            // Print Job Status
            when (printJobState) {
                is PrintJobUiState.Printing -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.height(24.dp).width(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Printing (polling status)...")
                    }
                }
                is PrintJobUiState.Completed -> {
                    Text(
                        "Printed ✓",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                is PrintJobUiState.Failed -> {
                    Text(
                        "Print failed.",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                is PrintJobUiState.Idle -> {
                    // Nothing to show
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = {
                        receiptsViewModel.resetPrintJob()
                        onDismiss()
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Skip")
                }
                Button(
                    onClick = {
                        selectedMacAddress?.let { mac ->
                            receiptsViewModel.startPrintJob(transactionId, mac)
                        }
                    },
                    enabled = selectedMacAddress != null && printJobState == PrintJobUiState.Idle
                ) {
                    Text("Print")
                }
            }
        }
    }
}
