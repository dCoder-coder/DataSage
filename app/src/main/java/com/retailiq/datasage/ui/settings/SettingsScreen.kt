package com.retailiq.datasage.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.retailiq.datasage.BuildConfig

@Composable
fun SettingsScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings & Connectivity")
        Text("Active API URL: ${BuildConfig.API_BASE_URL}")
        Text("Override at build time: ./gradlew assembleDebug -PAPI_BASE_URL=https://api.example.com/")
    }
}
