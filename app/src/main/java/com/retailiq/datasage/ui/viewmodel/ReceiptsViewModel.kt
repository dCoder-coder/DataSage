package com.retailiq.datasage.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.model.BarcodeProductDto
import com.retailiq.datasage.data.model.PrintJobRequest
import com.retailiq.datasage.data.model.ReceiptTemplateDto
import com.retailiq.datasage.data.model.ReceiptTemplateRequest
import com.retailiq.datasage.data.repository.ReceiptsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// ── Template States ─────────────────────────────────────────────
sealed class TemplateUiState {
    data object Idle : TemplateUiState()
    data object Loading : TemplateUiState()
    data class Success(val template: ReceiptTemplateDto) : TemplateUiState()
    data class Error(val message: String) : TemplateUiState()
}

sealed class SaveUiState {
    data object Idle : SaveUiState()
    data object Saving : SaveUiState()
    data object Saved : SaveUiState()
    data class Error(val message: String) : SaveUiState()
}

// ── Barcode Lookup States ────────────────────────────────────────
sealed class BarcodeLookupUiState {
    data object Idle : BarcodeLookupUiState()
    data object Loading : BarcodeLookupUiState()
    data class Success(val product: BarcodeProductDto) : BarcodeLookupUiState()
    data class Error(val message: String) : BarcodeLookupUiState()
}

// ── Print Job States ─────────────────────────────────────────────
sealed class PrintJobUiState {
    data object Idle : PrintJobUiState()
    data object Printing : PrintJobUiState()
    data object Completed : PrintJobUiState()
    data object Failed : PrintJobUiState()
}

@HiltViewModel
class ReceiptsViewModel @Inject constructor(
    private val repository: ReceiptsRepository
) : ViewModel() {

    companion object {
        private const val MAX_POLL_ATTEMPTS = 10
        private const val POLL_INTERVAL_MS = 2_000L
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
    }

    // Template
    private val _templateState = MutableStateFlow<TemplateUiState>(TemplateUiState.Idle)
    val templateState: StateFlow<TemplateUiState> = _templateState.asStateFlow()

    private val _templateSaveState = MutableStateFlow<SaveUiState>(SaveUiState.Idle)
    val templateSaveState: StateFlow<SaveUiState> = _templateSaveState.asStateFlow()

    // Barcode lookup — decoupled from sale state
    private val _barcodeLookupState = MutableStateFlow<BarcodeLookupUiState>(BarcodeLookupUiState.Idle)
    val barcodeLookupState: StateFlow<BarcodeLookupUiState> = _barcodeLookupState.asStateFlow()

    // Print job
    private val _printJobState = MutableStateFlow<PrintJobUiState>(PrintJobUiState.Idle)
    val printJobState: StateFlow<PrintJobUiState> = _printJobState.asStateFlow()

    // ── Template ──────────────────────────────────────────────────

    fun loadTemplate() {
        if (_templateState.value is TemplateUiState.Loading) return
        viewModelScope.launch {
            _templateState.value = TemplateUiState.Loading
            repository.getTemplate()
                .onSuccess { _templateState.value = TemplateUiState.Success(it) }
                .onFailure {
                    Timber.e(it, "Failed to load receipt template")
                    _templateState.value = TemplateUiState.Error(it.message ?: "Unknown error")
                }
        }
    }

    fun saveTemplate(req: ReceiptTemplateRequest) {
        viewModelScope.launch {
            _templateSaveState.value = SaveUiState.Saving
            repository.updateTemplate(req)
                .onSuccess {
                    _templateSaveState.value = SaveUiState.Saved
                    _templateState.value = TemplateUiState.Success(it)
                }
                .onFailure {
                    Timber.e(it, "Failed to save receipt template")
                    _templateSaveState.value = SaveUiState.Error(it.message ?: "Save failed")
                }
        }
    }

    fun resetSaveState() {
        _templateSaveState.value = SaveUiState.Idle
    }

    // ── Barcode Lookup ────────────────────────────────────────────

    fun lookupBarcode(value: String) {
        viewModelScope.launch {
            _barcodeLookupState.value = BarcodeLookupUiState.Loading
            repository.lookupBarcode(value)
                .onSuccess { _barcodeLookupState.value = BarcodeLookupUiState.Success(it) }
                .onFailure {
                    Timber.w(it, "Barcode lookup failed: %s", value)
                    _barcodeLookupState.value = BarcodeLookupUiState.Error(it.message ?: "Lookup failed")
                }
        }
    }

    fun resetBarcodeLookup() {
        _barcodeLookupState.value = BarcodeLookupUiState.Idle
    }

    // ── Print Job ─────────────────────────────────────────────────

    fun startPrintJob(transactionId: String, printerMacAddress: String) {
        viewModelScope.launch {
            _printJobState.value = PrintJobUiState.Printing
            val createResult = repository.createPrintJob(
                PrintJobRequest(transactionId, printerMacAddress)
            )
            createResult.onFailure {
                Timber.e(it, "Failed to create print job")
                _printJobState.value = PrintJobUiState.Failed
                return@launch
            }

            val jobId = createResult.getOrNull()?.jobId ?: run {
                _printJobState.value = PrintJobUiState.Failed
                return@launch
            }

            var attempts = 0
            while (attempts < MAX_POLL_ATTEMPTS) {
                delay(POLL_INTERVAL_MS)
                attempts++
                val pollResult = repository.pollPrintJob(jobId)
                val status = pollResult.getOrNull()?.status
                when (status) {
                    STATUS_COMPLETED -> {
                        _printJobState.value = PrintJobUiState.Completed
                        return@launch
                    }
                    STATUS_FAILED -> {
                        _printJobState.value = PrintJobUiState.Failed
                        return@launch
                    }
                    else -> Timber.d("Print job %s: status=%s (attempt %d)", jobId, status, attempts)
                }
            }

            // Exhausted max polls without a terminal status
            Timber.w("Print job %s timed out after %d polls", jobId, MAX_POLL_ATTEMPTS)
            _printJobState.value = PrintJobUiState.Failed
        }
    }

    fun resetPrintJob() {
        _printJobState.value = PrintJobUiState.Idle
    }
}
