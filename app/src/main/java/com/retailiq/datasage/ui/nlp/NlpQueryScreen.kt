package com.retailiq.datasage.ui.nlp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.NlpQueryApiService
import com.retailiq.datasage.data.api.NlpQueryRequest
import com.retailiq.datasage.data.api.NlpQueryResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// ── ViewModel ────────────────────────────────────────────────────────────────

data class NlpConversationItem(
    val query: String,
    val response: NlpQueryResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NlpQueryViewModel @Inject constructor(
    private val nlpApi: NlpQueryApiService
) : ViewModel() {

    val conversation = mutableStateListOf<NlpConversationItem>()

    fun askQuery(queryText: String) {
        val idx = conversation.size
        conversation.add(NlpConversationItem(query = queryText, isLoading = true))

        viewModelScope.launch {
            try {
                val response = nlpApi.query(NlpQueryRequest(queryText))
                if (response.isSuccess && response.data != null) {
                    conversation[idx] = NlpConversationItem(query = queryText, response = response.data)
                } else {
                    conversation[idx] = NlpConversationItem(query = queryText, error = response.message ?: "Query failed")
                }
            } catch (e: Exception) {
                Timber.e(e, "NLP query error")
                conversation[idx] = NlpConversationItem(query = queryText, error = e.message ?: "Network error")
            }
        }
    }
}

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NlpQueryScreen(
    onNavigateBack: () -> Unit,
    viewModel: NlpQueryViewModel = hiltViewModel()
) {
    var queryText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Query") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Conversation history
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                reverseLayout = false
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                if (viewModel.conversation.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Ask anything about your store",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Try: \"What was my revenue last week?\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    "\"Which product sells the most?\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    "\"Show me low stock items\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                items(viewModel.conversation) { item ->
                    // User query bubble
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            item.query,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Response
                    when {
                        item.isLoading -> {
                            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.CenterStart) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("Thinking...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }

                        item.error != null -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Text(
                                    item.error,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        item.response != null -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (item.response.headline.isNotBlank()) {
                                        Text(
                                            item.response.headline,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                    if (item.response.detail.isNotBlank()) {
                                        Text(item.response.detail, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    // Supporting metrics
                                    if (item.response.supportingMetrics.isNotEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        item.response.supportingMetrics.forEach { (key, value) ->
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    key.replace("_", " ").replaceFirstChar { it.uppercase() },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    value.toString(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }

            // Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask a question...") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )
                IconButton(
                    onClick = {
                        if (queryText.isNotBlank()) {
                            viewModel.askQuery(queryText.trim())
                            queryText = ""
                        }
                    },
                    enabled = queryText.isNotBlank()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (queryText.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
