package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.GstApiService
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.model.GstConfigDto
import com.retailiq.datasage.data.model.GstSlabDto
import com.retailiq.datasage.data.model.GstSummaryDto
import com.retailiq.datasage.data.model.HsnDto
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GstRepository @Inject constructor(
    private val api: GstApiService
) {
    suspend fun getConfig(): NetworkResult<GstConfigDto> {
        return try {
            val response = api.getConfig()
            if (response.success && response.data != null) {
                NetworkResult.Success(response.data)
            } else {
                NetworkResult.Error(0, response.error?.message ?: "Failed to fetch GST config")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching GST config")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }

    suspend fun updateConfig(config: GstConfigDto): NetworkResult<GstConfigDto> {
        return try {
            val response = api.updateConfig(config)
            if (response.success && response.data != null) {
                NetworkResult.Success(response.data)
            } else {
                NetworkResult.Error(0, response.error?.message ?: "Failed to update GST config")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating GST config")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }

    suspend fun searchHsn(query: String): NetworkResult<List<HsnDto>> {
        return try {
            val response = api.searchHsn(query)
            if (response.success && response.data != null) {
                NetworkResult.Success(response.data)
            } else {
                NetworkResult.Error(0, response.error?.message ?: "Failed to search HSN")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error searching HSN")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }

    suspend fun getSummary(period: String): NetworkResult<GstSummaryDto> {
        return try {
            val response = api.getSummary(period)
            if (response.success && response.data != null) {
                NetworkResult.Success(response.data)
            } else {
                NetworkResult.Error(0, response.error?.message ?: "Failed to fetch GST summary")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching GST summary")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }

    suspend fun getLiabilitySlabs(period: String): NetworkResult<List<GstSlabDto>> {
        return try {
            val response = api.getLiabilitySlabs(period)
            if (response.success && response.data != null) {
                NetworkResult.Success(response.data)
            } else {
                NetworkResult.Error(0, response.error?.message ?: "Failed to fetch GST slabs")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching GST slabs")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }

    suspend fun getGstr1(period: String): NetworkResult<Map<String, Any>> {
        return try {
            val response = api.exportGstr1(period)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            Timber.e(e, "Error exporting GSTR-1")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }
}
