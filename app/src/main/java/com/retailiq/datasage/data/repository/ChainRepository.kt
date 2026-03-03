package com.retailiq.datasage.data.repository

import com.retailiq.datasage.data.api.ChainApiService
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.toUserMessage
import com.retailiq.datasage.data.model.ChainDashboardDto
import com.retailiq.datasage.data.model.StoreCompareResponseDto
import com.retailiq.datasage.data.model.TransferSuggestionDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChainRepository @Inject constructor(
    private val api: ChainApiService
) {

    suspend fun getDashboard(): NetworkResult<ChainDashboardDto> = try {
        val resp = api.getDashboard()
        if (resp.success && resp.data != null) NetworkResult.Success(resp.data)
        else NetworkResult.Error(422, resp.error.toUserMessage())
    } catch (e: Throwable) {
        NetworkResult.Error(500, e.message ?: "Network error")
    }

    suspend fun getComparison(period: String): NetworkResult<StoreCompareResponseDto> = try {
        val resp = api.getComparison(period)
        if (resp.success && resp.data != null) NetworkResult.Success(resp.data)
        else NetworkResult.Error(422, resp.error.toUserMessage())
    } catch (e: Throwable) {
        NetworkResult.Error(500, e.message ?: "Network error")
    }

    suspend fun getTransfers(): NetworkResult<List<TransferSuggestionDto>> = try {
        val resp = api.getTransfers()
        if (resp.success && resp.data != null) NetworkResult.Success(resp.data)
        else NetworkResult.Error(422, resp.error.toUserMessage())
    } catch (e: Throwable) {
        NetworkResult.Error(500, e.message ?: "Network error")
    }

    suspend fun confirmTransfer(id: String): NetworkResult<Boolean> = try {
        val resp = api.confirmTransfer(id)
        if (resp.success) NetworkResult.Success(true)
        else NetworkResult.Error(422, resp.error.toUserMessage())
    } catch (e: Throwable) {
        NetworkResult.Error(500, e.message ?: "Network error")
    }
}
