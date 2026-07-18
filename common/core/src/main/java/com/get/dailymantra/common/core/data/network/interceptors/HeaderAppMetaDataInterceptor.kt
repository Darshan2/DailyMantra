package com.get.dailymantra.common.core.data.network.interceptors

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class HeaderAppMetaDataInterceptor @Inject constructor(
    @ApplicationContext private val context: Context,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader(ApiHeaders.Names.APP_LANGUAGE, context.resources.configuration.locales[0].toLanguageTag())
            .addHeader(ApiHeaders.Names.APP_VERSION, context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty())
            .build()
        return chain.proceed(request)
    }
}