package com.get.dailymantra.common.core.data.network

class FakeAppMetadataProvider(
    private val languageTag: String = "en-US",
    private val versionName: String = "1.0.0",
) : AppMetadataProvider {
    override fun languageTag(): String = languageTag
    override fun versionName(): String = versionName
}
