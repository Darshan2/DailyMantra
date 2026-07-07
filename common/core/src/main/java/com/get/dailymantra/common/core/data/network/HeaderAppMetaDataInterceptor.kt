package com.get.dailymantra.common.core.data.network

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
            .addHeader("Accept-Language", context.resources.configuration.locales[0].toLanguageTag())
            .addHeader("X-App-Version", @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty())
            .build()
        return chain.proceed(request)
    }
}
