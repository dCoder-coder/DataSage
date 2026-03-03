package com.retailiq.datasage.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.Customer
import com.retailiq.datasage.data.api.CustomerApiService
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.CreditAccountDto
import com.retailiq.datasage.data.model.CreditTransactionDto
import com.retailiq.datasage.data.model.LoyaltyAccountDto
import com.retailiq.datasage.data.model.LoyaltyTransactionDto
import com.retailiq.datasage.data.repository.CreditRepository
import com.retailiq.datasage.data.repository.LoyaltyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed class CustomerProfileUiState {
    data object Loading : CustomerProfileUiState()
    data class Loaded(
        val customer: Customer,
        val loyaltyAccount: LoyaltyAccountDto?,
        val loyaltyTransactions: List<LoyaltyTransactionDto>,
        val creditAccount: CreditAccountDto?,
        val creditTransactions: List<CreditTransactionDto>
    ) : CustomerProfileUiState()
    data class Error(val message: String) : CustomerProfileUiState()
}

@HiltViewModel
class CustomerProfileViewModel @Inject constructor(
    private val customerApi: CustomerApiService,
    private val loyaltyRepo: LoyaltyRepository,
    private val creditRepo: CreditRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CustomerProfileUiState>(CustomerProfileUiState.Loading)
    val uiState: StateFlow<CustomerProfileUiState> = _uiState.asStateFlow()

    private val _repaymentState = MutableStateFlow<NetworkResult<CreditTransactionDto>?>(null)
    val repaymentState: StateFlow<NetworkResult<CreditTransactionDto>?> = _repaymentState.asStateFlow()

    fun loadCustomer(customerId: Int) {
        viewModelScope.launch {
            _uiState.value = CustomerProfileUiState.Loading
            try {
                // Fetch customer details
                val customerRes = customerApi.getCustomer(customerId)
                if (!customerRes.success || customerRes.data == null) {
                    _uiState.value = CustomerProfileUiState.Error(customerRes.error?.message ?: "Failed to load customer")
                    return@launch
                }
                
                // Fetch loyalty data
                val loyaltyAcc = when (val res = loyaltyRepo.getAccount(customerId)) {
                    is NetworkResult.Success -> res.data
                    else -> null
                }
                val loyaltyTxs = when (val res = loyaltyRepo.getTransactions(customerId)) {
                    is NetworkResult.Success -> res.data
                    else -> emptyList()
                }

                // Fetch credit data
                val creditAcc = when (val res = creditRepo.getAccount(customerId)) {
                    is NetworkResult.Success -> res.data
                    else -> null
                }
                val creditTxs = when (val res = creditRepo.getTransactions(customerId)) {
                    is NetworkResult.Success -> res.data
                    else -> emptyList()
                }

                _uiState.value = CustomerProfileUiState.Loaded(
                    customer = customerRes.data,
                    loyaltyAccount = loyaltyAcc,
                    loyaltyTransactions = loyaltyTxs,
                    creditAccount = creditAcc,
                    creditTransactions = creditTxs
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading customer profile")
                _uiState.value = CustomerProfileUiState.Error("Network error: ${e.message}")
            }
        }
    }

    fun submitRepayment(customerId: Int, amount: Double, notes: String?) {
        viewModelScope.launch {
            _repaymentState.value = NetworkResult.Loading()
            val result = creditRepo.repay(customerId, amount, notes)
            _repaymentState.value = result
            if (result is NetworkResult.Success) {
                // Reload profile data to reflect changes
                loadCustomer(customerId)
            }
        }
    }

    fun resetRepaymentState() {
        _repaymentState.value = null
    }
}
