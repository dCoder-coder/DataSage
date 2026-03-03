package com.retailiq.datasage.ui.inventory.ocr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.ConfirmedItem
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.OcrItemDto
import com.retailiq.datasage.data.repository.InventoryRepository
import com.retailiq.datasage.data.repository.VisionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

sealed class OcrState {
    data object Idle : OcrState()
    data object Capturing : OcrState()
    data object Uploading : OcrState()
    data class Polling(val jobId: String) : OcrState()
    data class Review(val jobId: String, val items: List<OcrItemDto>) : OcrState()
    data object Confirming : OcrState()
    data class Done(val message: String) : OcrState()
    data class Error(val message: String) : OcrState()
}

@HiltViewModel
class OcrViewModel @Inject constructor(
    private val visionRepo: VisionRepository,
    private val inventoryRepo: InventoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow<OcrState>(OcrState.Idle)
    val state: StateFlow<OcrState> = _state.asStateFlow()

    fun startCapture() {
        _state.value = OcrState.Capturing
    }

    fun uploadInvoice(file: File) = viewModelScope.launch {
        _state.value = OcrState.Uploading
        when (val result = visionRepo.uploadInvoice(file)) {
            is NetworkResult.Success -> {
                val jobId = result.data
                _state.value = OcrState.Polling(jobId)
                startPolling(jobId)
            }
            is NetworkResult.Error -> {
                _state.value = OcrState.Error(result.message)
            }
            is NetworkResult.Loading -> Unit
        }
    }

    fun startPolling(jobId: String) = viewModelScope.launch {
        var attempts = 0
        val maxAttempts = 30
        val delayMs = 3000L

        while (attempts < maxAttempts) {
            when (val result = visionRepo.getJobStatus(jobId)) {
                is NetworkResult.Success -> {
                    val job = result.data
                    if (job.status == "REVIEW") {
                        _state.value = OcrState.Review(jobId, job.items)
                        return@launch
                    } else if (job.status == "FAILED") {
                        _state.value = OcrState.Error("Invoice processing failed on server.")
                        return@launch
                    }
                    // If QUEUED or PROCESSING, just wait
                }
                is NetworkResult.Error -> {
                    Timber.w("Polling error: ${result.message}")
                    // we can tolerate transient network errors during polling, 
                    // or immediately fail. Let's fail fast for now
                    _state.value = OcrState.Error(result.message)
                    return@launch
                }
                is NetworkResult.Loading -> Unit
            }

            delay(delayMs)
            attempts++
        }

        // If loop completes without return, timeout occurred
        _state.value = OcrState.Error("Processing is taking longer than expected. Try again later.")
    }

    fun confirm(jobId: String, items: List<ConfirmedItem>) = viewModelScope.launch {
        _state.value = OcrState.Confirming
        when (val result = visionRepo.confirmJob(jobId, items)) {
            is NetworkResult.Success -> {
                _state.value = OcrState.Done("Stock updated for ${items.size} products.")
                // Potentially trigger inventory sync here manually
                inventoryRepo.getProducts(forceRefresh = true)
            }
            is NetworkResult.Error -> _state.value = OcrState.Error(result.message)
            is NetworkResult.Loading -> Unit
        }
    }

    fun dismiss(jobId: String) = viewModelScope.launch {
        _state.value = OcrState.Confirming // use same loading state
        when (val result = visionRepo.dismissJob(jobId)) {
            is NetworkResult.Success -> _state.value = OcrState.Done("Invoice dismissed.")
            is NetworkResult.Error -> _state.value = OcrState.Error(result.message)
            is NetworkResult.Loading -> Unit
        }
    }

    fun reset() {
        _state.value = OcrState.Idle
    }
}
