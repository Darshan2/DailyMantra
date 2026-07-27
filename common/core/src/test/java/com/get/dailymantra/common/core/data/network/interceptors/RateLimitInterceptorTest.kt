package com.get.dailymantra.common.core.data.network.interceptors

import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class RateLimitInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private val recordedDelays = mutableListOf<Long>()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun client() = OkHttpClient
        .Builder()
        .addInterceptor(RateLimitInterceptor(sleeper = { recordedDelays.add(it) }))
        .build()

    private fun execute() = client()
        .newCall(
            Request.Builder()
                .url(mockWebServer.url("/"))
                .get()
                .build()
        ).execute()

    @Test
    fun `successful response is returned without retrying`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val response = execute()

        assertThat(response.code).isEqualTo(200)
        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(recordedDelays).isEmpty()
    }

    @Test
    fun `429 with Retry-After header is retried once and the eventual response is returned`() {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(429)
                .setHeader(ApiHeaders.Names.RETRY_AFTER, "2")
        )
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val response = execute()

        assertThat(response.code).isEqualTo(200)
        assertThat(mockWebServer.requestCount).isEqualTo(2)
        assertThat(recordedDelays).isEqualTo(listOf(2_000L))
    }

    @Test
    fun `429 without Retry-After header falls back to the default delay`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(429))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        execute()

        assertThat(recordedDelays).isEqualTo(listOf(RateLimitInterceptor.FALLBACK_DELAY_MS))
    }

    @Test
    fun `429 with a non-numeric Retry-After header falls back to the default delay`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader(ApiHeaders.Names.RETRY_AFTER, "not-a-number")
        )
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        execute()

        assertThat(recordedDelays).isEqualTo(listOf(RateLimitInterceptor.FALLBACK_DELAY_MS))
    }

    @Test
    fun `Retry-After beyond the max delay is clamped`() {
        val delayOverMax = RateLimitInterceptor.FALLBACK_DELAY_MS + 1000
        mockWebServer.enqueue(
            MockResponse().setResponseCode(429)
                .setHeader(ApiHeaders.Names.RETRY_AFTER, delayOverMax)
        )
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        execute()

        assertThat(recordedDelays).isEqualTo(listOf(RateLimitInterceptor.MAX_DELAY_MS))
    }

    @Test
    fun `second consecutive 429 is not retried again and is returned as-is`() {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(429)
                .setHeader(ApiHeaders.Names.RETRY_AFTER, "1")
        )
        mockWebServer.enqueue(
            MockResponse().setResponseCode(429)
                .setHeader(ApiHeaders.Names.RETRY_AFTER, "5")
        )

        val response = execute()

        assertThat(response.code).isEqualTo(429)
        assertThat(mockWebServer.requestCount).isEqualTo(2)
        assertThat(recordedDelays).isEqualTo(listOf(1_000L))
    }

    @Test
    fun `other error codes are not retried`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(503))

        val response = execute()

        assertThat(response.code).isEqualTo(503)
        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(recordedDelays).isEmpty()
    }
}
