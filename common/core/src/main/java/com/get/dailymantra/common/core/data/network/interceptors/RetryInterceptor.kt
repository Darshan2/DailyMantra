package com.get.dailymantra.common.core.data.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import kotlin.math.pow

/**
 * Retries requests with exponential backoff on a fixed set of transient HTTP status codes:
 * - 502 (Bad Gateway) — upstream returned an invalid response, often transient
 * - 503 (Service Unavailable) — server temporarily overloaded or down for maintenance
 * - 504 (Gateway Timeout) — upstream took too long to respond
 *
 * 500 (Internal Server Error) is deliberately excluded: it usually indicates a server-side
 * bug rather than a transient condition, so retrying is unlikely to help and can be unsafe
 * for non-idempotent requests.
 *
 * 429 (Too Many Requests) is deliberately excluded too: it's owned by [RateLimitInterceptor],
 * which honors the server's `Retry-After` header instead of blind exponential backoff.
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val baseDelayMs: Long = 500
) : Interceptor {

    private val retryableCodes = setOf(502, 503, 504)

    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var lastException: IOException? = null

        while (attempt < maxRetries) {
            try {
                val response = chain.proceed(chain.request())
                if (response.code !in retryableCodes) {
                    return response // success or non-retryable — return as-is
                }
                if (attempt == maxRetries - 1) return response // out of retries, hand back last response

                response.close()
            } catch (e: IOException) {
                lastException = e
                if (attempt == maxRetries - 1) throw e
            }

            val delay = (baseDelayMs * 2.0.pow(attempt)).toLong() // exponential
            Thread.sleep(delay) // interceptors run off the main thread already (OkHttp dispatcher), so this is fine
            attempt++
        }
        throw lastException ?: IOException("Retry failed")
    }
}