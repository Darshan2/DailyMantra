package com.get.dailymantra.common.core.data.network.interceptors

import com.get.dailymantra.common.core.data.network.AppMetadataProvider
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class HeaderAppMetaDataInterceptor @Inject constructor(
    private val appMetadataProvider: AppMetadataProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader(ApiHeaders.Names.APP_LANGUAGE, appMetadataProvider.languageTag())
            .addHeader(ApiHeaders.Names.APP_VERSION, appMetadataProvider.versionName())
            .build()
        return chain.proceed(request)
    }
}
