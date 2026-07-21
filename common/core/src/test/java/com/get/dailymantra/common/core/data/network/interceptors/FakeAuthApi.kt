package com.get.dailymantra.common.core.data.network.interceptors

import com.get.dailymantra.common.core.data.network.AuthApi
import com.get.dailymantra.common.core.data.network.RefreshTokenRequest
import com.get.dailymantra.common.core.data.network.RefreshTokenResponse

class FakeAuthApi(
    private val result: Result<RefreshTokenResponse> = Result.failure(IllegalStateException("FakeAuthApi: no result configured")),
) : AuthApi {

    var lastRequest: RefreshTokenRequest? = null
        private set

    override suspend fun refresh(request: RefreshTokenRequest): RefreshTokenResponse {
        lastRequest = request
        return result.getOrThrow()
    }
}
