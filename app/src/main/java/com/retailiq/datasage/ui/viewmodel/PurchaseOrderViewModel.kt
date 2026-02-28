package com.retailiq.datasage.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.model.supplier.CreatePoItemRequest
import com.retailiq.datasage.data.model.supplier.CreatePoRequest
import com.retailiq.datasage.data.model.supplier.GoodsReceiptItemRequest
import com.retailiq.datasage.data.model.supplier.GoodsReceiptRequest
import com.retailiq.datasage.data.model.supplier.PurchaseOrderDto
import com.retailiq.datasage.data.repository.SupplierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PoListUiState {
    data object Loading : PoListUiState()
    data class Loaded(val orders: List<PurchaseOrderDto>) : PoListUiState()
    data class Error(val message: String) : PoListUiState()
}

sealed class PoDetailUiState {
    data object Loading : PoDetailUiState()
    data class Loaded(val order: PurchaseOrderDto) : PoDetailUiState()
    data class Error(val message: String) : PoDetailUiState()
}

sealed class PoActionUiState {
    data object Idle : PoActionUiState()
    data object InProgress : PoActionUiState()
    data class Success(val message: String) : PoActionUiState()
    data class Error(val message: String) : PoActionUiState()
}

@HiltViewModel
class PurchaseOrderViewModel @Inject constructor(
    private val repository: SupplierRepository
) : ViewModel() {

    private val _listState = MutableStateFlow<PoListUiState>(PoListUiState.Loading)
    val listState: StateFlow<PoListUiState> = _listState.asStateFlow()
    
    private val _detailState = MutableStateFlow<PoDetailUiState>(PoDetailUiState.Loading)
    val detailState: StateFlow<PoDetailUiState> = _detailState.asStateFlow()

    private val _actionState = MutableStateFlow<PoActionUiState>(PoActionUiState.Idle)
    val actionState: StateFlow<PoActionUiState> = _actionState.asStateFlow()

    fun loadPurchaseOrders(supplierId: Int? = null) = viewModelScope.launch {
        _listState.value = PoListUiState.Loading
        repository.getPurchaseOrders(supplierId).fold(
            onSuccess = { _listState.value = PoListUiState.Loaded(it) },
            onFailure = { _listState.value = PoListUiState.Error(it.message ?: "Unknown error") }
        )
    }

    fun loadPurchaseOrder(id: Int) = viewModelScope.launch {
        _detailState.value = PoDetailUiState.Loading
        repository.getPurchaseOrder(id).fold(
            onSuccess = { _detailState.value = PoDetailUiState.Loaded(it) },
            onFailure = { _detailState.value = PoDetailUiState.Error(it.message ?: "Unknown error") }
        )
    }

    fun createPo(
        supplierId: Int,
        expectedDelivery: String?,
        notes: String?,
        isDraft: Boolean,
        items: List<CreatePoItemRequest>
    ) = viewModelScope.launch {
        _actionState.value = PoActionUiState.InProgress
        if (items.isEmpty()) {
            _actionState.value = PoActionUiState.Error("At least one product is required")
            return@launch
        }

        val request = CreatePoRequest(
            supplierId = supplierId,
            expectedDelivery = expectedDelivery,
            notes = notes,
            status = if (isDraft) "DRAFT" else "SENT",
            items = items
        )

        repository.createPurchaseOrder(request).fold(
            onSuccess = {
                _actionState.value = PoActionUiState.Success(if (isDraft) "Draft saved" else "PO Sent successfully")
                loadPurchaseOrders()
            },
            onFailure = {
                _actionState.value = PoActionUiState.Error(it.message ?: "Unknown error")
            }
        )
    }

    fun sendPo(id: Int) = viewModelScope.launch {
        _actionState.value = PoActionUiState.InProgress
        
        // Optimistic UI update
        val currentState = _detailState.value
        if (currentState is PoDetailUiState.Loaded && currentState.order.id == id) {
            val updatedOrder = currentState.order.copy(status = "SENT")
            _detailState.value = PoDetailUiState.Loaded(updatedOrder)
        }

        val listStateValue = _listState.value
        if (listStateValue is PoListUiState.Loaded) {
            val updatedList = listStateValue.orders.map { 
                if (it.id == id) it.copy(status = "SENT") else it 
            }
            _listState.value = PoListUiState.Loaded(updatedList)
        }

        repository.sendPurchaseOrder(id).fold(
            onSuccess = {
                _actionState.value = PoActionUiState.Success("PO Sent successfully")
                _detailState.value = PoDetailUiState.Loaded(it) // Refresh with actual server response
                loadPurchaseOrders() // Refresh lists
            },
            onFailure = {
                _actionState.value = PoActionUiState.Error(it.message ?: "Unknown error")
                // Revert optimistic update
                if (currentState is PoDetailUiState.Loaded && currentState.order.id == id) {
                    _detailState.value = currentState
                }
                if (listStateValue is PoListUiState.Loaded) {
                    _listState.value = listStateValue
                }
            }
        )
    }

    fun receiveGoods(id: Int, items: List<GoodsReceiptItemRequest>) = viewModelScope.launch {
        _actionState.value = PoActionUiState.InProgress
        
        // Validate at least one item
        if (items.isEmpty() || items.all { it.receivedQty == 0 }) {
            _actionState.value = PoActionUiState.Error("Must receive at least one item")
            return@launch
        }

        val request = GoodsReceiptRequest(items)
        repository.receiveGoods(id, request).fold(
            onSuccess = {
                val sum = items.sumOf { it.receivedQty }
                _actionState.value = PoActionUiState.Success("Stock updated for $sum products")
                loadPurchaseOrder(id) // Refresh details
            },
            onFailure = {
                _actionState.value = PoActionUiState.Error(it.message ?: "Unknown error")
            }
        )
    }

    fun resetActionState() {
        _actionState.value = PoActionUiState.Idle
    }
}
