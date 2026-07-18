package com.get.dailymantra.common.core.data.network

import com.get.dailymantra.common.core.data.security.TokenStore
import javax.inject.Inject

class DefaultTokenProvider @Inject constructor(
    private val tokenStore: TokenStore,
) : TokenProvider {
    override suspend fun getAccessToken(): String? = tokenStore.getAccessToken()
    override suspend fun getRefreshToken(): String? = tokenStore.getRefreshToken()
    override suspend fun saveTokens(accessToken: String, refreshToken: String) =
        tokenStore.saveTokens(accessToken, refreshToken)
    override suspend fun clearTokens() = tokenStore.clearTokens()
}
