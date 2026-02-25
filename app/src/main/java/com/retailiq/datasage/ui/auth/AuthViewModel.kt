package com.retailiq.datasage.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val message: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _otpSecondsRemaining = MutableStateFlow(300)
    val otpSecondsRemaining: StateFlow<Int> = _otpSecondsRemaining.asStateFlow()

    private val _resendCount = MutableStateFlow(0)
    val resendCount: StateFlow<Int> = _resendCount.asStateFlow()

    private var otpCountdownJob: Job? = null

    fun startOtpCountdown() {
        otpCountdownJob?.cancel()
        otpCountdownJob = viewModelScope.launch {
            _otpSecondsRemaining.value = 300
            while (_otpSecondsRemaining.value > 0) {
                delay(1000)
                _otpSecondsRemaining.value -= 1
            }
        }
    }

    fun canResendOtp(): Boolean = _otpSecondsRemaining.value == 0 && _resendCount.value < 3

    fun resendOtp(mobile: String) {
        if (!canResendOtp()) return
        _resendCount.value += 1
        forgotPassword(mobile)
        startOtpCountdown()
    }

    fun login(mobile: String, password: String, onSuccess: (String) -> Unit) = viewModelScope.launch {
        _uiState.value = AuthUiState.Loading
        when (val result = authRepository.login(mobile, password)) {
            is NetworkResult.Success -> {
                _uiState.value = AuthUiState.Success("Login successful")
                onSuccess(result.data)
            }
            is NetworkResult.Error -> {
                val msg = if (result.code == 429) "Too many attempts. Try again in 15 minutes." else result.message
                _uiState.value = AuthUiState.Error(msg)
            }
            is NetworkResult.Loading -> Unit
        }
    }

    fun register(fullName: String, mobile: String, store: String, password: String, onSuccess: () -> Unit) = viewModelScope.launch {
        _uiState.value = AuthUiState.Loading
        when (val result = authRepository.register(fullName, mobile, store, password)) {
            is NetworkResult.Success -> {
                _uiState.value = AuthUiState.Success("OTP sent")
                onSuccess()
            }
            is NetworkResult.Error -> _uiState.value = AuthUiState.Error(result.message)
            is NetworkResult.Loading -> Unit
        }
    }

    /** After OTP verification the account is activated. User must login separately. */
    fun verifyOtp(mobile: String, otp: String, onSuccess: () -> Unit) = viewModelScope.launch {
        _uiState.value = AuthUiState.Loading
        when (val result = authRepository.verifyOtp(mobile, otp)) {
            is NetworkResult.Success -> {
                _uiState.value = AuthUiState.Success("Account verified. Please login.")
                onSuccess()
            }
            is NetworkResult.Error -> _uiState.value = AuthUiState.Error(result.message)
            is NetworkResult.Loading -> Unit
        }
    }

    fun forgotPassword(mobile: String) = viewModelScope.launch {
        _uiState.value = AuthUiState.Loading
        when (val result = authRepository.forgotPassword(mobile)) {
            is NetworkResult.Success -> _uiState.value = AuthUiState.Success("OTP sent")
            is NetworkResult.Error -> _uiState.value = AuthUiState.Error(result.message)
            is NetworkResult.Loading -> Unit
        }
    }

    fun resetPassword(token: String, newPassword: String, onSuccess: () -> Unit) = viewModelScope.launch {
        _uiState.value = AuthUiState.Loading
        when (val result = authRepository.resetPassword(token, newPassword)) {
            is NetworkResult.Success -> {
                _uiState.value = AuthUiState.Success("Password reset")
                onSuccess()
            }
            is NetworkResult.Error -> _uiState.value = AuthUiState.Error(result.message)
            is NetworkResult.Loading -> Unit
        }
    }

    fun hasToken(): Boolean = authRepository.hasValidSession()
    fun isSetupComplete(): Boolean = authRepository.isSetupComplete()
    fun role(): String = authRepository.getRole()
    fun completeSetup() = authRepository.markSetupComplete()
    fun logout() = authRepository.logout()

    override fun onCleared() {
        otpCountdownJob?.cancel()
        super.onCleared()
    }
}
