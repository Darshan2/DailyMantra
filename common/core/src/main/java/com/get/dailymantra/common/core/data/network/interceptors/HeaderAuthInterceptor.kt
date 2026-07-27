package com.get.dailymantra.common.core.data.network.interceptors

import com.get.dailymantra.common.core.data.network.TokenProvider
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Invocation
import javax.inject.Inject

class HeaderAuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val skipAuth = chain.request().tag(Invocation::class.java)
            ?.method()
            ?.isAnnotationPresent(NoAuthorization::class.java) == true

        val hasAuthorizationHeader = chain.request().header(ApiHeaders.Names.AUTHORIZATION) != null

        if (skipAuth || hasAuthorizationHeader) return chain.proceed(chain.request())

        val token = runBlocking { tokenProvider.getAccessToken() } ?: throw UnauthenticatedException()
        val request = chain.request().newBuilder()
            .addHeader(ApiHeaders.Names.AUTHORIZATION, "${ApiHeaders.Values.BEARER_PREFIX}$token")
            .build()
        return chain.proceed(request)
    }
}
