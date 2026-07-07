package com.get.dailymantra.common.core.data.security

interface TokenStore {
    fun getToken(): String?
    fun saveToken(token: String)
    fun clearToken()
}
