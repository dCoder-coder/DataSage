package com.retailiq.datasage.data.repository

import com.retailiq.datasage.core.TokenStore
import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.AuthApiService
import com.retailiq.datasage.data.api.AuthTokens
import com.retailiq.datasage.data.api.ForgotPasswordRequest
import com.retailiq.datasage.data.api.LoginRequest
import com.retailiq.datasage.data.api.LogoutRequest
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.OtpVerifyRequest
import com.retailiq.datasage.data.api.RefreshRequest
import com.retailiq.datasage.data.api.RegisterRequest
import com.retailiq.datasage.data.api.ResetPasswordRequest
import com.retailiq.datasage.data.api.SimpleMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {
    @Test
    fun login_savesTokensAndReturnsRole() = runBlocking {
        val tokenStore = FakeTokenStore()
        val repo = AuthRepository(FakeAuthApi(), tokenStore)

        val result = repo.login("9999999999", "secret")

        assertTrue(result is NetworkResult.Success)
        assertEquals("owner", (result as NetworkResult.Success).data)
        assertEquals("access-1", tokenStore.access)
    }

    @Test
    fun register_returnsSuccess() = runBlocking {
        val tokenStore = FakeTokenStore()
        val repo = AuthRepository(FakeAuthApi(), tokenStore)

        val result = repo.register("Test User", "9999999999", "My Store", "secret123")

        assertTrue(result is NetworkResult.Success)
    }

    @Test
    fun verifyOtp_returnsSuccess() = runBlocking {
        val tokenStore = FakeTokenStore()
        val repo = AuthRepository(FakeAuthApi(), tokenStore)

        val result = repo.verifyOtp("9999999999", "123456")

        assertTrue(result is NetworkResult.Success)
    }

    private class FakeAuthApi : AuthApiService {
        override suspend fun register(request: RegisterRequest) = ApiResponse(true, SimpleMessage("OTP sent successfully."), null, null)
        override suspend fun verifyOtp(request: OtpVerifyRequest) = ApiResponse(true, SimpleMessage("Account verified successfully."), null, null)
        override suspend fun login(request: LoginRequest) = ApiResponse(true, AuthTokens("access-1", "refresh-1", 1, "owner", 1), null, null)
        override suspend fun refresh(request: RefreshRequest) = ApiResponse(true, AuthTokens("access-2", "refresh-2", 1, "owner", 1), null, null)
        override suspend fun logout(request: LogoutRequest?) = ApiResponse(true, SimpleMessage("ok"), null, null)
        override suspend fun forgotPassword(request: ForgotPasswordRequest) = ApiResponse(true, SimpleMessage("ok"), null, null)
        override suspend fun resetPassword(request: ResetPasswordRequest) = ApiResponse(true, SimpleMessage("ok"), null, null)
    }

    private data class FakeTokenStore(
        var access: String? = null,
        var refresh: String? = null,
        var role: String = "staff",
        var setupComplete: Boolean = false
    ) : TokenStore {
        override fun saveTokens(accessToken: String, refreshToken: String) { access = accessToken; refresh = refreshToken; role = "owner" }
        override fun getAccessToken(): String? = access
        override fun getRefreshToken(): String? = refresh
        override fun getRole(): String = role
        override fun isSetupComplete(): Boolean = setupComplete
        override fun markSetupComplete() { setupComplete = true }
        override fun clearTokens() { access = null; refresh = null }
    }
}
