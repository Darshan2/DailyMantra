package com.get.dailymantra.common.core.data.network.interceptors

import com.get.dailymantra.common.core.data.network.AppMetadataProvider
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class HeaderAppMetaDataInterceptor @Inject constructor(
    private val appMetadataProvider: AppMetadataProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val hasLanguageHeader = chain.request().header(ApiHeaders.Names.APP_LANGUAGE) != null
        val hasAppVersionHeader = chain.request().header(ApiHeaders.Names.APP_VERSION) != null

        val requestBuilder = chain.request().newBuilder()
        if (!hasLanguageHeader) {
            requestBuilder.addHeader(ApiHeaders.Names.APP_LANGUAGE, appMetadataProvider.languageTag())
        }
        if (!hasAppVersionHeader) {
            requestBuilder.addHeader(ApiHeaders.Names.APP_VERSION, appMetadataProvider.versionName())
        }
        return chain.proceed(requestBuilder.build())
    }
}
