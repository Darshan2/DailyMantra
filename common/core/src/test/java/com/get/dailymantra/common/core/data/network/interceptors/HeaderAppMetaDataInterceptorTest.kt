package com.get.dailymantra.common.core.data.network.interceptors

import com.get.dailymantra.common.core.data.network.FakeAppMetadataProvider
import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class HeaderAppMetaDataInterceptorTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun execute(appMetadataProvider: FakeAppMetadataProvider) {
        val client = OkHttpClient.Builder()
            .addInterceptor(HeaderAppMetaDataInterceptor(appMetadataProvider))
            .build()
        mockWebServer.enqueue(MockResponse().setBody(""))
        client.newCall(Request.Builder().url(mockWebServer.url("/")).get().build()).execute().close()
    }

    @Test
    fun `App-Language header is set from the metadata provider`() {
        execute(FakeAppMetadataProvider(languageTag = "fr-FR"))

        val header = mockWebServer.takeRequest().headers[ApiHeaders.Names.APP_LANGUAGE]
        assertThat(header).isEqualTo("fr-FR")
    }

    @Test
    fun `App-Version header is set from the metadata provider`() {
        execute(FakeAppMetadataProvider(versionName = "9.9.9"))

        val header = mockWebServer.takeRequest().headers[ApiHeaders.Names.APP_VERSION]
        assertThat(header).isEqualTo("9.9.9")
    }

    @Test
    fun `existing App-Language header is left untouched`() {
        val client = OkHttpClient.Builder()
            .addInterceptor(HeaderAppMetaDataInterceptor(FakeAppMetadataProvider(languageTag = "fr-FR")))
            .build()
        mockWebServer.enqueue(MockResponse().setBody(""))

        val request = Request.Builder()
            .url(mockWebServer.url("/"))
            .header(ApiHeaders.Names.APP_LANGUAGE, "caller-supplied-language")
            .get()
            .build()

        client.newCall(request).execute().close()

        val header = mockWebServer.takeRequest().headers[ApiHeaders.Names.APP_LANGUAGE]
        assertThat(header).isEqualTo("caller-supplied-language")
    }

    @Test
    fun `existing App-Version header is left untouched`() {
        val client = OkHttpClient.Builder()
            .addInterceptor(HeaderAppMetaDataInterceptor(FakeAppMetadataProvider(versionName = "9.9.9")))
            .build()
        mockWebServer.enqueue(MockResponse().setBody(""))

        val request = Request.Builder()
            .url(mockWebServer.url("/"))
            .header(ApiHeaders.Names.APP_VERSION, "caller-supplied-version")
            .get()
            .build()

        client.newCall(request).execute().close()

        val header = mockWebServer.takeRequest().headers[ApiHeaders.Names.APP_VERSION]
        assertThat(header).isEqualTo("caller-supplied-version")
    }
}
