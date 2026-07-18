package com.get.dailymantra.common.core.data.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

/**
 * Attaches a unique [ApiHeaders.Names.REQUEST_ID] to POST/PATCH requests so the server can
 * detect and safely ignore duplicate submissions caused by client-side retries (e.g. from
 * [RetryInterceptor], or a user double-tapping "submit" after a slow/timed-out response).
 *
 * Without this, a retried POST could create the resource twice (e.g. two identical orders
 * placed for one checkout). With an idempotency key, the server can recognize the second
 * request as a retry of the first and return the original result instead of repeating the
 * side effect.
 *
 * Example: a request times out after the server already created the order, but before the
 * client received the response. The client's retry logic resends the same POST with the same
 * [ApiHeaders.Names.REQUEST_ID]. The server sees it has already processed that ID and returns
 * the existing order rather than creating a duplicate one.
 *
 * If the request already carries the header (e.g. set explicitly by the caller), it is left
 * untouched.
 */
class IdempotencyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.method !in listOf("POST", "PATCH") ||
            original.header(ApiHeaders.Names.REQUEST_ID) != null) {
            return chain.proceed(original) // already has one, or method doesn't need it
        }
        val request = original.newBuilder()
            .header(ApiHeaders.Names.REQUEST_ID, UUID.randomUUID().toString())
            .build()
        return chain.proceed(request)
    }
}