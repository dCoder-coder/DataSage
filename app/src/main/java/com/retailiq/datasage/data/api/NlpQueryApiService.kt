package com.retailiq.datasage.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

interface NlpQueryApiService {
    @POST("api/v1/query/")
    suspend fun query(@Body request: NlpQueryRequest): StoreApiResponse<NlpQueryResponse>
}

data class NlpQueryRequest(
    @SerializedName("query_text") val queryText: String
)

data class NlpQueryResponse(
    val intent: String = "",
    val headline: String = "",
    val detail: String = "",
    val action: String = "",
    @SerializedName("supporting_metrics") val supportingMetrics: Map<String, Any> = emptyMap()
)
