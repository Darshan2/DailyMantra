package com.get.dailymantra.common.core.data.network

import com.get.dailymantra.common.core.coroutines.CoroutineDispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Invocation
import javax.inject.Inject

class HeaderInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val skipAuth = chain.request().tag(Invocation::class.java)
            ?.method()
            ?.isAnnotationPresent(NoAuthorization::class.java) == true

        if (skipAuth) return chain.proceed(chain.request())

        val token = runBlocking { tokenProvider.getToken() }
        val request = chain.request().newBuilder()
            .apply { token?.let { addHeader("Authorization", "Bearer $it") } }
            .build()
        return chain.proceed(request)
    }
}
