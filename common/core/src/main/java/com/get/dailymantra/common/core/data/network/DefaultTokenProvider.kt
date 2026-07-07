package com.get.dailymantra.common.core.data.network

import com.get.dailymantra.common.core.data.security.TokenStore
import javax.inject.Inject

class DefaultTokenProvider @Inject constructor(
    private val tokenStore: TokenStore,
) : TokenProvider {
    override suspend fun getToken(): String? = tokenStore.getToken()
}
