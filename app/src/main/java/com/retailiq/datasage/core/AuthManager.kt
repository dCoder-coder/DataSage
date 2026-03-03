package com.retailiq.datasage.core

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(@ApplicationContext context: Context) : TokenStore {
    private val prefs = EncryptedSharedPreferences.create(
        "auth_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun saveTokens(accessToken: String, refreshToken: String) {
        val role = decodeField(accessToken, "role", "staff")
        val chainRole = decodeField(accessToken, "chain_role", "")
        val chainGroupId = decodeField(accessToken, "chain_group_id", "")
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .putString(KEY_ROLE, role)
            .putString(KEY_CHAIN_ROLE, chainRole)
            .putString(KEY_CHAIN_GROUP_ID, chainGroupId)
            .apply()
    }

    override fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)

    override fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    override fun getRole(): String = prefs.getString(KEY_ROLE, "staff") ?: "staff"

    override fun isChainOwner(): Boolean =
        (prefs.getString(KEY_CHAIN_ROLE, "") ?: "") == "CHAIN_OWNER"

    override fun getChainGroupId(): String? =
        prefs.getString(KEY_CHAIN_GROUP_ID, null)?.takeIf { it.isNotBlank() }

    override fun isSetupComplete(): Boolean = prefs.getBoolean(KEY_SETUP_COMPLETE, false)

    override fun markSetupComplete() {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, true).apply()
    }

    override fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS).remove(KEY_REFRESH).remove(KEY_ROLE)
            .remove(KEY_CHAIN_ROLE).remove(KEY_CHAIN_GROUP_ID)
            .apply()
    }

    private fun decodeField(jwt: String, field: String, default: String): String {
        return runCatching {
            val payload = jwt.split(".")[1]
            val decoded = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            JSONObject(decoded).optString(field, default)
        }.getOrDefault(default)
    }

    // kept for compatibility if referenced elsewhere
    private fun decodeRole(jwt: String) = decodeField(jwt, "role", "staff")

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_ROLE = "role"
        private const val KEY_CHAIN_ROLE = "chain_role"
        private const val KEY_CHAIN_GROUP_ID = "chain_group_id"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
    }
}
