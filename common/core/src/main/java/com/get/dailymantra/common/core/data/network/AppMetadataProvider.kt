package com.get.dailymantra.common.core.data.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface AppMetadataProvider {
    fun languageTag(): String
    fun versionName(): String
}

class DefaultAppMetadataProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppMetadataProvider {

    override fun languageTag(): String =
        context.resources.configuration.locales[0].toLanguageTag()

    override fun versionName(): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
}
