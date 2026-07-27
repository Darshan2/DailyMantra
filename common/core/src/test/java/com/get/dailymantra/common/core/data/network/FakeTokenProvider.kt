package com.get.dailymantra.common.core.data.network

class FakeTokenProvider(
    private var accessToken: String? = null,
    private var refreshToken: String? = null,
) : TokenProvider {

    var saveTokensCalls = 0
        private set
    var clearTokensCalls = 0
        private set

    override suspend fun getAccessToken(): String? = accessToken

    override suspend fun getRefreshToken(): String? = refreshToken

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        saveTokensCalls++
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    override suspend fun clearTokens() {
        clearTokensCalls++
        accessToken = null
        refreshToken = null
    }
}
