package com.retailiq.datasage.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<SimpleMessage>

    @POST("api/v1/auth/verify-otp")
    suspend fun verifyOtp(@Body request: OtpVerifyRequest): ApiResponse<SimpleMessage>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthTokens>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): ApiResponse<AuthTokens>

    @DELETE("api/v1/auth/logout")
    suspend fun logout(@Body request: LogoutRequest? = null): ApiResponse<SimpleMessage>

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): ApiResponse<SimpleMessage>

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): ApiResponse<SimpleMessage>
}

data class RegisterRequest(
    @SerializedName("full_name") val fullName: String,
    @SerializedName("mobile_number") val mobileNumber: String,
    @SerializedName("password") val password: String,
    @SerializedName("store_name") val storeName: String? = null,
    val email: String? = null,
    val role: String? = null
)

data class OtpVerifyRequest(
    @SerializedName("mobile_number") val mobileNumber: String,
    @SerializedName("otp") val otp: String
)

data class LoginRequest(
    @SerializedName("mobile_number") val mobileNumber: String,
    @SerializedName("password") val password: String
)

data class RefreshRequest(@SerializedName("refresh_token") val refreshToken: String)

data class LogoutRequest(@SerializedName("refresh_token") val refreshToken: String? = null)

data class ForgotPasswordRequest(@SerializedName("mobile_number") val mobileNumber: String)

data class ResetPasswordRequest(
    @SerializedName("token") val token: String,
    @SerializedName("new_password") val newPassword: String
)

data class AuthTokens(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("user_id") val userId: Int = 0,
    @SerializedName("role") val role: String = "staff",
    @SerializedName("store_id") val storeId: Int? = null
)

data class SimpleMessage(@SerializedName("message") val message: String)
