package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.StaffApiService
import com.retailiq.datasage.data.model.DailyTargetRequest
import com.retailiq.datasage.data.model.StaffPerformanceSummaryDto
import com.retailiq.datasage.data.model.StaffSessionDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaffRepository @Inject constructor(
    private val api: StaffApiService
) {
    suspend fun startSession(): Result<StaffSessionDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.startSession()
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception(e.message ?: "Network error"))
        } catch (e: HttpException) {
            Result.failure(Exception(e.message ?: "HTTP error"))
        } catch (e: java.lang.Exception) {
            Result.failure(Exception(e.message ?: "Unknown error"))
        }
    }

    suspend fun endSession(): Result<StaffSessionDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.endSession()
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception(e.message ?: "Network error"))
        } catch (e: HttpException) {
            Result.failure(Exception(e.message ?: "HTTP error"))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Unknown error"))
        }
    }

    suspend fun getDailyPerformance(date: String): Result<List<StaffPerformanceSummaryDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getDailyPerformance(date)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to load staff performance"))
            }
        } catch (e: IOException) {
            Result.failure(Exception(e.message ?: "Network error"))
        } catch (e: HttpException) {
            Result.failure(Exception(e.message ?: "HTTP error"))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Unknown error"))
        }
    }

    suspend fun setDailyTarget(request: DailyTargetRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.setDailyTarget(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception(e.message ?: "Network error"))
        } catch (e: HttpException) {
            Result.failure(Exception(e.message ?: "HTTP error"))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Unknown error"))
        }
    }
}
