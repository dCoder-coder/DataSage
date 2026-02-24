package com.retailiq.datasage.ui.auth

import com.retailiq.datasage.core.TokenStore
import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.AuthApiService
import com.retailiq.datasage.data.api.AuthTokens
import com.retailiq.datasage.data.api.ForgotPasswordRequest
import com.retailiq.datasage.data.api.LoginRequest
import com.retailiq.datasage.data.api.OtpVerifyRequest
import com.retailiq.datasage.data.api.RefreshRequest
import com.retailiq.datasage.data.api.RegisterRequest
import com.retailiq.datasage.data.api.ResetPasswordRequest
import com.retailiq.datasage.data.api.SimpleMessage
import com.retailiq.datasage.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun hasToken_reflectsRepositoryState() = runTest {
        val vm = AuthViewModel(AuthRepository(FakeAuthApi(), FakeTokenStore(access = "token")))
        assertTrue(vm.hasToken())
    }

    private class FakeAuthApi : AuthApiService {
        override suspend fun register(request: RegisterRequest) = ApiResponse(true, AuthTokens("a", "b", "owner"), null, null, "")
        override suspend fun verifyOtp(request: OtpVerifyRequest) = ApiResponse(true, AuthTokens("a", "b", "owner"), null, null, "")
        override suspend fun login(request: LoginRequest) = ApiResponse(true, AuthTokens("a", "b", "owner"), null, null, "")
        override suspend fun refresh(request: RefreshRequest) = ApiResponse(true, AuthTokens("a", "b", "owner"), null, null, "")
        override suspend fun forgotPassword(request: ForgotPasswordRequest) = ApiResponse(true, SimpleMessage("ok"), null, null, "")
        override suspend fun resetPassword(request: ResetPasswordRequest) = ApiResponse(true, SimpleMessage("ok"), null, null, "")
    }

    private data class FakeTokenStore(
        var access: String? = null,
        var refresh: String? = null,
        var role: String = "staff",
        var setup: Boolean = false
    ) : TokenStore {
        override fun saveTokens(accessToken: String, refreshToken: String) { access = accessToken; refresh = refreshToken }
        override fun getAccessToken(): String? = access
        override fun getRefreshToken(): String? = refresh
        override fun getRole(): String = role
        override fun isSetupComplete(): Boolean = setup
        override fun markSetupComplete() { setup = true }
        override fun clearTokens() { access = null; refresh = null }
    }
}
