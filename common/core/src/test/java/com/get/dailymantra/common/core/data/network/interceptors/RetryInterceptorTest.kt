package com.get.dailymantra.common.core.data.network.interceptors

import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class RetryInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        client = OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor(maxRetries = 3, baseDelayMs = 1))
            .build()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun execute() = client.newCall(
        Request.Builder().url(mockWebServer.url("/")).get().build()
    ).execute()

    @Test
    fun `successful response is returned without retrying`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val response = execute()

        assertThat(response.code).isEqualTo(200)
        assertThat(mockWebServer.requestCount).isEqualTo(1)
    }

    @Test
    fun `502 is retried and the eventual success response is returned`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(502))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val response = execute()

        assertThat(response.code).isEqualTo(200)
        assertThat(mockWebServer.requestCount).isEqualTo(2)
    }

    @Test
    fun `503 is retried and the eventual success response is returned`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(503))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val response = execute()

        assertThat(response.code).isEqualTo(200)
        assertThat(mockWebServer.requestCount).isEqualTo(2)
    }

    @Test
    fun `504 is retried and the eventual success response is returned`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(504))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val response = execute()

        assertThat(response.code).isEqualTo(200)
        assertThat(mockWebServer.requestCount).isEqualTo(2)
    }

    @Test
    fun `500 is not retried`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val response = execute()

        assertThat(response.code).isEqualTo(500)
        assertThat(mockWebServer.requestCount).isEqualTo(1)
    }

    @Test
    fun `429 is not retried since RateLimitInterceptor owns it`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(429))

        val response = execute()

        assertThat(response.code).isEqualTo(429)
        assertThat(mockWebServer.requestCount).isEqualTo(1)
    }

    @Test
    fun `retries are capped at maxRetries and the last retryable response is returned`() {
        repeat(3) { mockWebServer.enqueue(MockResponse().setResponseCode(502)) }

        val response = execute()

        assertThat(response.code).isEqualTo(502)
        assertThat(mockWebServer.requestCount).isEqualTo(3)
    }

    @Test
    fun `delay doubles on each successive retry`() {
        val recordedDelays = mutableListOf<Long>()
        val baseDelayMs = 500L
        val backoffClient = OkHttpClient.Builder()
            .addInterceptor(
                RetryInterceptor(
                    maxRetries = 3,
                    baseDelayMs = baseDelayMs,
                    sleeper = { recordedDelays.add(it) }
                )
            )
            .build()

        repeat(3) { mockWebServer.enqueue(MockResponse().setResponseCode(502)) }

        backoffClient.newCall(Request.Builder().url(mockWebServer.url("/")).get().build()).execute()

        assertThat(recordedDelays).isEqualTo(listOf(baseDelayMs, baseDelayMs * 2))
    }

    @Test
    fun `no sleep occurs when the first response succeeds`() {
        val recordedDelays = mutableListOf<Long>()
        val backoffClient = OkHttpClient.Builder()
            .addInterceptor(
                RetryInterceptor(maxRetries = 3, baseDelayMs = 500, sleeper = { recordedDelays.add(it) })
            )
            .build()
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        backoffClient.newCall(Request.Builder().url(mockWebServer.url("/")).get().build()).execute()

        assertThat(recordedDelays).isEmpty()
    }

    @Test
    fun `no sleep occurs after the last attempt is exhausted`() {
        val recordedDelays = mutableListOf<Long>()
        val maxRetries = 3
        val backoffClient = OkHttpClient.Builder()
            .addInterceptor(
                RetryInterceptor(maxRetries = maxRetries, baseDelayMs = 500, sleeper = { recordedDelays.add(it) })
            )
            .build()
        repeat(maxRetries) { mockWebServer.enqueue(MockResponse().setResponseCode(502)) }

        backoffClient.newCall(Request.Builder().url(mockWebServer.url("/")).get().build()).execute()

        assertThat(recordedDelays).hasSize(maxRetries - 1)
    }
}
