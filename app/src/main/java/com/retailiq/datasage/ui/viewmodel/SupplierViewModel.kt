package com.retailiq.datasage.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.model.supplier.CreateSupplierRequest
import com.retailiq.datasage.data.model.supplier.SupplierDto
import com.retailiq.datasage.data.model.supplier.SupplierProfileDto
import com.retailiq.datasage.data.repository.SupplierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SupplierListUiState {
    data object Loading : SupplierListUiState()
    data class Loaded(val suppliers: List<SupplierDto>) : SupplierListUiState()
    data class Error(val message: String) : SupplierListUiState()
}

sealed class SupplierProfileUiState {
    data object Loading : SupplierProfileUiState()
    data class Loaded(val profile: SupplierProfileDto) : SupplierProfileUiState()
    data class Error(val message: String) : SupplierProfileUiState()
}

sealed class SupplierCreateUiState {
    data object Idle : SupplierCreateUiState()
    data object Creating : SupplierCreateUiState()
    data class Success(val supplierId: String) : SupplierCreateUiState()
    data class Error(val message: String) : SupplierCreateUiState()
}

@HiltViewModel
class SupplierViewModel @Inject constructor(
    private val repository: SupplierRepository
) : ViewModel() {

    private val _listState = MutableStateFlow<SupplierListUiState>(SupplierListUiState.Loading)
    val listState: StateFlow<SupplierListUiState> = _listState.asStateFlow()

    private val _profileState = MutableStateFlow<SupplierProfileUiState>(SupplierProfileUiState.Loading)
    val profileState: StateFlow<SupplierProfileUiState> = _profileState.asStateFlow()

    private val _createState = MutableStateFlow<SupplierCreateUiState>(SupplierCreateUiState.Idle)
    val createState: StateFlow<SupplierCreateUiState> = _createState.asStateFlow()

    init {
        loadSuppliers()
    }

    fun loadSuppliers() = viewModelScope.launch {
        _listState.value = SupplierListUiState.Loading
        repository.getSuppliers().fold(
            onSuccess = { _listState.value = SupplierListUiState.Loaded(it) },
            onFailure = { _listState.value = SupplierListUiState.Error(it.message ?: "Unknown error") }
        )
    }

    fun loadSupplierProfile(id: String) = viewModelScope.launch {
        _profileState.value = SupplierProfileUiState.Loading
        repository.getSupplierProfile(id).fold(
            onSuccess = { _profileState.value = SupplierProfileUiState.Loaded(it) },
            onFailure = { _profileState.value = SupplierProfileUiState.Error(it.message ?: "Unknown error") }
        )
    }

    fun createSupplier(
        name: String,
        contactName: String?,
        phone: String?,
        email: String?,
        paymentTermsDays: Int
    ) = viewModelScope.launch {
        if (name.isBlank()) {
            _createState.value = SupplierCreateUiState.Error("Supplier name is required")
            return@launch
        }
        _createState.value = SupplierCreateUiState.Creating
        val request = CreateSupplierRequest(name, contactName, phone, email, paymentTermsDays)
        repository.createSupplier(request).fold(
            onSuccess = { id ->
                _createState.value = SupplierCreateUiState.Success(id)
                loadSuppliers() // Refresh list
            },
            onFailure = {
                _createState.value = SupplierCreateUiState.Error(it.message ?: "Unknown error")
            }
        )
    }

    fun resetCreateState() {
        _createState.value = SupplierCreateUiState.Idle
    }
}
