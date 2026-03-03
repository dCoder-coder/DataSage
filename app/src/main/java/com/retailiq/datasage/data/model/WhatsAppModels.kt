package com.retailiq.datasage.data.model

import com.google.gson.annotations.SerializedName

data class WhatsAppConfigDto(
    val phone_number_id: String?,
    val access_token: String?,
    val waba_id: String?,
    val webhook_verify_token: String?,
    val is_active: Boolean,
    val has_access_token: Boolean? = null
)

data class SendAlertRequest(
    val alert_id: Int,
    val recipient_phone: String? = null // if null, backend might use owner's phone or something else
)

data class SendPoRequest(
    val po_id: String,
    val supplier_phone: String? = null
)

data class WhatsAppLogDto(
    val id: String,
    val recipient_phone: String,
    val content_preview: String,
    val status: String, // QUEUED, SENT, DELIVERED, FAILED
    @SerializedName("sent_at") val timestamp: String?,
    val error_message: String? = null
)

data class WhatsAppLogResponse(
    val logs: List<WhatsAppLogDto>,
    val total: Int,
    val pages: Int
)
