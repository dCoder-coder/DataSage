package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.WhatsAppApiService
import com.retailiq.datasage.data.model.SendAlertRequest
import com.retailiq.datasage.data.model.SendPoRequest
import com.retailiq.datasage.data.model.WhatsAppConfigDto
import com.retailiq.datasage.data.model.WhatsAppLogResponse
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsAppRepository @Inject constructor(
    private val api: WhatsAppApiService
) {
    suspend fun getConfig(): NetworkResult<WhatsAppConfigDto> {
        return try {
            val response = api.getConfig()
            if (response.success && response.data != null) {
                NetworkResult.Success(response.data)
            } else {
                NetworkResult.Error(0, response.error?.message ?: "Failed to fetch WhatsApp config")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching WhatsApp config")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }

    suspend fun updateConfig(config: WhatsAppConfigDto): NetworkResult<WhatsAppConfigDto> {
        return try {
            val response = api.updateConfig(config)
            if (response.success && response.data != null) {
                NetworkResult.Success(response.data)
            } else {
                NetworkResult.Error(0, response.error?.message ?: "Failed to update WhatsApp config")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating WhatsApp config")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }

    suspend fun sendAlert(alertId: Int, recipientPhone: String? = null): NetworkResult<Boolean> {
        return try {
            val response = api.sendAlert(SendAlertRequest(alertId, recipientPhone))
            if (response.success) {
                NetworkResult.Success(true)
            } else {
                NetworkResult.Error(0, response.error?.message ?: "Failed to send alert via WhatsApp")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error sending alert via WhatsApp")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }

    suspend fun sendPo(poId: String, supplierPhone: String): NetworkResult<Boolean> {
        return try {
            val response = api.sendPo(SendPoRequest(poId, supplierPhone))
            if (response.success) {
                NetworkResult.Success(true)
            } else {
                NetworkResult.Error(0, response.error?.message ?: "Failed to send PO via WhatsApp")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error sending PO via WhatsApp")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }

    suspend fun getLogs(page: Int = 1, pageSize: Int = 50): NetworkResult<WhatsAppLogResponse> {
        return try {
            val response = api.getLogs(page, pageSize)
            if (response.success && response.data != null) {
                NetworkResult.Success(response.data)
            } else {
                NetworkResult.Error(0, response.error?.message ?: "Failed to fetch WhatsApp logs")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching WhatsApp logs")
            NetworkResult.Error(0, "Network error: ${e.message}")
        }
    }
}
