package com.retailiq.datasage.ui.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.local.PendingTransactionDao
import com.retailiq.datasage.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncStatusViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    pendingDao: PendingTransactionDao
) : ViewModel() {

    val pending: StateFlow<Int> = pendingDao.countByStatusFlow("pending")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val failed: StateFlow<Int> = pendingDao.countByStatusFlow("failed")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun retryFailed() = viewModelScope.launch {
        transactionRepository.retryFailed()
    }
}
