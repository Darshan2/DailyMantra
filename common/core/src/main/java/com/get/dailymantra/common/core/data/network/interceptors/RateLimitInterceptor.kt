package com.get.dailymantra.common.core.data.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Retries once on 429 (Too Many Requests), honoring the server's `Retry-After` header instead
 * of blind exponential backoff. Owns 429 exclusively — [RetryInterceptor] deliberately excludes
 * it so the two don't double-retry the same response.
 */
class RateLimitInterceptor @Inject constructor(
    private val sleeper: (Long) -> Unit = Thread::sleep
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (response.code == 429) {
            val retryAfter = response.header(ApiHeaders.Names.RETRY_AFTER)
            val delayMs = (parseRetryAfter(retryAfter) ?: FALLBACK_DELAY_MS).coerceAtMost(MAX_DELAY_MS)

            response.close()
            sleeper(delayMs)
            return chain.proceed(chain.request())
        }
        return response
    }

    private fun parseRetryAfter(header: String?): Long? {
        if (header == null) return null
        // Retry-After is in seconds ("120")
        return header.toLongOrNull()?.times(1000)
    }

    companion object {
        const val FALLBACK_DELAY_MS = 1_000L

        // Retry-After is server-controlled input; clamp it so one 429 can't park an OkHttp
        // dispatcher thread for an unbounded amount of time.
        const val MAX_DELAY_MS = 30_000L
    }
}