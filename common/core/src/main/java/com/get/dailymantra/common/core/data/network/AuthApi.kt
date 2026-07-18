package com.get.dailymantra.common.core.data.network

import com.get.dailymantra.common.core.data.network.interceptors.NoAuthorization
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @NoAuthorization
    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): RefreshTokenResponse
}

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
data class RefreshTokenResponse(val accessToken: String, val refreshToken: String)
