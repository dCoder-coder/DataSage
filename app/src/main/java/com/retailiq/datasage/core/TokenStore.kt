package com.retailiq.datasage.core

interface TokenStore {
    fun saveTokens(accessToken: String, refreshToken: String)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun getRole(): String
    fun isChainOwner(): Boolean
    fun getChainGroupId(): String?
    fun isSetupComplete(): Boolean
    fun markSetupComplete()
    fun clearTokens()
}
