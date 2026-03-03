package com.retailiq.datasage.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.HsnDto
import com.retailiq.datasage.data.repository.GstRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HsnSearchUiState {
    data object Idle : HsnSearchUiState()
    data object Loading : HsnSearchUiState()
    data class Success(val results: List<HsnDto>) : HsnSearchUiState()
    data class Error(val message: String) : HsnSearchUiState()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class HsnSearchViewModel @Inject constructor(
    private val repository: GstRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<HsnSearchUiState>(HsnSearchUiState.Idle)
    val uiState: StateFlow<HsnSearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300L)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.length >= 2) {
                        performSearch(query)
                    } else if (query.isEmpty()) {
                        _uiState.value = HsnSearchUiState.Idle
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private suspend fun performSearch(query: String) {
        _uiState.value = HsnSearchUiState.Loading
        when (val result = repository.searchHsn(query)) {
            is NetworkResult.Success -> {
                _uiState.value = HsnSearchUiState.Success(result.data)
            }
            is NetworkResult.Error -> {
                _uiState.value = HsnSearchUiState.Error(result.message)
            }
            is NetworkResult.Loading -> Unit
        }
    }

    fun selectHsn(hsnDto: HsnDto) {
        // Typically handled by UI passing this back via callback. 
        // Can hold selected state here if needed, but we'll let UI bubble it up.
    }
}
