package com.retailiq.datasage.data.api

import com.google.gson.annotations.SerializedName

/** Standard API response envelope used by most modules. */
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: T?,
    @SerializedName("error") val error: ApiError?,
    @SerializedName("meta") val meta: PaginationMeta?,
    @SerializedName("timestamp") val timestamp: String? = null
)

/** Alternate envelope used by Store, Recommendations, and NLP modules. */
data class StoreApiResponse<T>(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?,
    @SerializedName("meta") val meta: PaginationMeta?
) {
    val isSuccess: Boolean get() = status == "success"
}

data class ApiError(
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("details") val details: Map<String, String>? = null
)

data class PaginationMeta(
    @SerializedName("page") val page: Int? = null,
    @SerializedName("page_size") val pageSize: Int? = null,
    @SerializedName("total") val total: Int? = null
)

sealed class NetworkResult<T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error<T>(val code: Int, val message: String) : NetworkResult<T>()
    class Loading<T> : NetworkResult<T>()
}

// ── Error Code Handling ──

enum class ApiErrorCode(val code: String, val userMessage: String) {
    VALIDATION_ERROR("VALIDATION_ERROR", "Please check your input"),
    DUPLICATE_MOBILE("DUPLICATE_MOBILE", "This mobile number is already registered"),
    INVALID_OTP("INVALID_OTP", "Invalid or expired OTP"),
    INVALID_DATE("INVALID_DATE", "Date must be in YYYY-MM-DD format"),
    BAD_REQUEST("BAD_REQUEST", "Invalid request"),
    UNAUTHORIZED("UNAUTHORIZED", "Please login to continue"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "Session expired, please login again"),
    INVALID_TOKEN("INVALID_TOKEN", "Invalid session, please login again"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Wrong mobile number or password"),
    INACTIVE_ACCOUNT("INACTIVE_ACCOUNT", "Please verify your account first"),
    FORBIDDEN("FORBIDDEN", "You don't have permission for this action"),
    NOT_FOUND("NOT_FOUND", "Resource not found"),
    USER_NOT_FOUND("USER_NOT_FOUND", "User does not exist"),
    SERVER_ERROR("SERVER_ERROR", "Something went wrong, please try again");
}

fun ApiError?.toUserMessage(): String {
    val known = ApiErrorCode.entries.find { it.code == this?.code }
    return known?.userMessage ?: this?.message ?: "An unexpected error occurred"
}
