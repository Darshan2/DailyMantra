package com.get.dailymantra.common.core.data.network.interceptors

import com.get.dailymantra.common.core.data.network.AuthApi
import com.get.dailymantra.common.core.data.network.RefreshTokenRequest
import com.get.dailymantra.common.core.data.network.TokenProvider
import com.get.dailymantra.common.core.data.security.AppEventBus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

/**
 * Refreshes the access token on a 401 and retries the request once.
 *
 * Runs off the main thread (OkHttp dispatcher), so blocking via [runBlocking] is safe here.
 * [authApi] is built from a separate OkHttpClient that has no [TokenAuthenticator] attached,
 * so a failing refresh call can't recursively trigger this authenticator again.
 */
class TokenAuthenticator @Inject constructor(
    private val tokenProvider: TokenProvider,
    private val authApi: AuthApi,
    private val appEventBus: AppEventBus,
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MAX_ATTEMPTS) return null // already retried once and failed again

        return runBlocking {
            mutex.withLock {
                val failedToken = response.request.header(ApiHeaders.Names.AUTHORIZATION)
                    ?.removePrefix(ApiHeaders.Values.BEARER_PREFIX)
                val currentToken = tokenProvider.getAccessToken()

                // A concurrent request already refreshed the token while this one was in flight.
                if (currentToken != null && currentToken != failedToken) {
                    return@withLock response.request.withBearerToken(currentToken)
                }

                val refreshToken = tokenProvider.getRefreshToken() ?: return@withLock giveUp()

                val refreshed = runCatching {
                    authApi.refresh(RefreshTokenRequest(refreshToken))
                }.getOrNull() ?: return@withLock giveUp()

                tokenProvider.saveTokens(refreshed.accessToken, refreshed.refreshToken)
                response.request.withBearerToken(refreshed.accessToken)
            }
        }
    }

    private suspend fun giveUp(): Request? {
        tokenProvider.clearTokens()
        appEventBus.notifySessionExpired()
        return null
    }

    private fun Request.withBearerToken(token: String): Request = newBuilder()
        .header(ApiHeaders.Names.AUTHORIZATION, "${ApiHeaders.Values.BEARER_PREFIX}$token")
        .build()

    /**
     * Counts responses in this call's retry chain by walking [Response.priorResponse].
     *
     * OkHttp doesn't pass a retry counter to [authenticate], so this chain length is the
     * only signal available for how many times the request has already been retried.
     * Used to cap refresh attempts at [MAX_ATTEMPTS] and avoid looping forever if the
     * server keeps returning 401 even after a token refresh.
     *
     * Caveat: the chain includes *all* prior responses on the call, not just failed auth
     * attempts — e.g. a 301 redirect followed by two 401s produces a chain of length 3,
     * so this authenticator would give up after only one refresh instead of two. This is
     * treated as acceptable since redirects landing on 401-guarded endpoints are rare.
     */
    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_ATTEMPTS = 2
    }
}
