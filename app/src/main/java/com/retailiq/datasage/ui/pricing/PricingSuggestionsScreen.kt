package com.retailiq.datasage.ui.pricing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.data.api.PricingSuggestion
import com.retailiq.datasage.ui.components.EmptyStateView
import com.retailiq.datasage.ui.components.ShimmerLoadingList

// ── Teal colour used for Apply button and HIGH confidence chip ────────────────
private val TealColor = Color(0xFF009688)
private val AmberColor = Color(0xFFFFA000)
private val ConfidenceLow = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingSuggestionsScreen(
    viewModel: PricingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar for action feedback
    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is PricingActionState.Error   -> { snackbarHostState.showSnackbar(s.message); viewModel.resetAction() }
            is PricingActionState.Success -> { snackbarHostState.showSnackbar(s.message); viewModel.resetAction() }
            else                          -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pricing Suggestions") },
                actions = {
                    IconButton(onClick = { viewModel.loadSuggestions() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = uiState) {
            is PricingUiState.Loading -> {
                ShimmerLoadingList(
                    modifier = Modifier.padding(padding)
                )
            }

            is PricingUiState.Error -> {
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.message)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadSuggestions() }) { Text("Retry") }
                }
            }

            is PricingUiState.Loaded -> {
                if (state.suggestions.isEmpty()) {
                    // Empty state
                    EmptyStateView(
                        message = "No pricing suggestions this week. Check back Sunday.",
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(Modifier.height(4.dp)) }
                        items(
                            items = state.suggestions,
                            key = { it.id }
                        ) { suggestion ->
                            // Track whether this card is visible (for exit animation)
                            var visible by remember { mutableStateOf(true) }
                            var showConfirmDialog by remember { mutableStateOf(false) }

                            AnimatedVisibility(
                                visible = visible,
                                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                            ) {
                                PricingSuggestionCard(
                                    suggestion = suggestion,
                                    onApply = { showConfirmDialog = true },
                                    onDismiss = {
                                        visible = false
                                        viewModel.dismissSuggestion(suggestion.id)
                                    }
                                )
                            }

                            // Confirmation dialog for Apply
                            if (showConfirmDialog) {
                                AlertDialog(
                                    onDismissRequest = { showConfirmDialog = false },
                                    title = { Text("Confirm Price Update") },
                                    text = {
                                        Text(
                                            "Update ${suggestion.productName} price to ₹${
                                                String.format("%.2f", suggestion.suggestedPrice)
                                            }?"
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                showConfirmDialog = false
                                                visible = false
                                                viewModel.applySuggestion(suggestion.id)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = TealColor)
                                        ) { Text("Confirm") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
                                    }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

// ── SuggestionCard ────────────────────────────────────────────────────────────

@Composable
fun PricingSuggestionCard(
    suggestion: PricingSuggestion,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("suggestion_card_${suggestion.id}"),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row: product name + confidence chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = suggestion.productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                ConfidenceChip(confidence = suggestion.confidence)
            }

            // Price arrow
            Text(
                text = "₹${String.format("%.2f", suggestion.currentPrice)} → ₹${String.format("%.2f", suggestion.suggestedPrice)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = TealColor
            )

            // Reason
            Text(
                text = suggestion.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Margin impact
            Text(
                text = "Margin improves from ${String.format("%.0f", suggestion.marginCurrent)}% to ${String.format("%.0f", suggestion.marginSuggested)}%.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f).testTag("apply_${suggestion.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = TealColor)
                ) { Text("Apply") }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).testTag("dismiss_${suggestion.id}")
                ) { Text("Dismiss") }
            }
        }
    }
}

// ── Confidence Chip ───────────────────────────────────────────────────────────

@Composable
fun ConfidenceChip(confidence: String) {
    val chipColor = when (confidence.uppercase()) {
        "HIGH"   -> TealColor
        "MEDIUM" -> AmberColor
        else     -> ConfidenceLow
    }
    SuggestionChip(
        onClick = {},
        label = { Text(confidence.uppercase(), style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = chipColor.copy(alpha = 0.15f)),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = chipColor
        )
    )
}
