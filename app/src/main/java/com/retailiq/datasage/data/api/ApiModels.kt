package com.retailiq.datasage.data.api

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: T?,
    @SerializedName("error") val error: ApiError?,
    @SerializedName("meta") val meta: PaginationMeta?,
    @SerializedName("timestamp") val timestamp: String
)

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
