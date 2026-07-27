package com.get.dailymantra.common.core.data.network.interceptors

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.common.truth.Truth.assertThat
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID

class IdempotencyInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        client = OkHttpClient.Builder()
            .addInterceptor(IdempotencyInterceptor())
            .build()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun jsonBody() = "{}".toRequestBody("application/json".toMediaType())

    private fun execute(request: Request) {
        mockWebServer.enqueue(MockResponse().setBody(""))
        client.newCall(request).execute().close()
    }

    @Test
    fun `POST without existing header gets a Request-Id assigned`() {
        val request = Request.Builder()
            .url(mockWebServer.url("/"))
            .post(jsonBody())
            .build()

        execute(request)

        val requestId = mockWebServer.takeRequest().headers[ApiHeaders.Names.REQUEST_ID]
        assertThat(requestId).isNotNull()
        UUID.fromString(requestId) // throws if not a valid UUID
    }

    @Test
    fun `PATCH without existing header gets a Request-Id assigned`() {
        val request = Request.Builder()
            .url(mockWebServer.url("/"))
            .patch(jsonBody())
            .build()

        execute(request)

        val requestId = mockWebServer.takeRequest().headers[ApiHeaders.Names.REQUEST_ID]
        assertThat(requestId).isNotNull()
        UUID.fromString(requestId)
    }

    @Test
    fun `GET requests are not assigned a Request-Id`() {
        val request = Request.Builder()
            .url(mockWebServer.url("/"))
            .get()
            .build()

        execute(request)

        val requestId = mockWebServer.takeRequest().headers[ApiHeaders.Names.REQUEST_ID]
        assertThat(requestId).isNull()
    }

    @Test
    fun `DELETE requests are not assigned a Request-Id`() {
        val request = Request.Builder()
            .url(mockWebServer.url("/"))
            .delete()
            .build()

        execute(request)

        val requestId = mockWebServer.takeRequest().headers[ApiHeaders.Names.REQUEST_ID]
        assertThat(requestId).isNull()
    }

    @Test
    fun `POST with caller-provided Request-Id is left untouched`() {
        val callerRequestId = "caller-supplied-id"
        val request = Request.Builder()
            .url(mockWebServer.url("/"))
            .header(ApiHeaders.Names.REQUEST_ID, callerRequestId)
            .post(jsonBody())
            .build()

        execute(request)

        val requestId = mockWebServer.takeRequest().headers[ApiHeaders.Names.REQUEST_ID]
        assertThat(requestId).isEqualTo(callerRequestId)
    }

    @Test
    fun `PATCH with caller-provided Request-Id is left untouched`() {
        val callerRequestId = "caller-supplied-id"
        val request = Request.Builder()
            .url(mockWebServer.url("/"))
            .header(ApiHeaders.Names.REQUEST_ID, callerRequestId)
            .patch(jsonBody())
            .build()

        execute(request)

        val requestId = mockWebServer.takeRequest().headers[ApiHeaders.Names.REQUEST_ID]
        assertThat(requestId).isEqualTo(callerRequestId)
    }

    @Test
    fun `each POST without a caller header gets a unique Request-Id`() {
        val first = Request.Builder().url(mockWebServer.url("/")).post(jsonBody()).build()
        val second = Request.Builder().url(mockWebServer.url("/")).post(jsonBody()).build()

        execute(first)
        execute(second)

        val firstRequestId = mockWebServer.takeRequest().headers[ApiHeaders.Names.REQUEST_ID]
        val secondRequestId = mockWebServer.takeRequest().headers[ApiHeaders.Names.REQUEST_ID]
        assertThat(firstRequestId).isNotEqualTo(secondRequestId)

        UUID.fromString(firstRequestId)
        UUID.fromString(secondRequestId)
    }
}
