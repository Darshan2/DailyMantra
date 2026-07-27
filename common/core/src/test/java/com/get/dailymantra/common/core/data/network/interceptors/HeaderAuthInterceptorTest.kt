package com.get.dailymantra.common.core.data.network.interceptors

import com.get.dailymantra.common.core.data.network.FakeTokenProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header

private interface TestApi {
    @NoAuthorization
    @GET("/public")
    suspend fun skipAuthEndpoint(): Response<Unit>

    @GET("/private")
    suspend fun normalApiEndPoint(): Response<Unit>

    @GET("/private")
    suspend fun endpointWithCallerHeader(@Header(ApiHeaders.Names.AUTHORIZATION) authorization: String): Response<Unit>
}

class HeaderAuthInterceptorTest {

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

    private fun client(accessToken: String?): TestApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(HeaderAuthInterceptor(FakeTokenProvider(accessToken)))
            .build()

        return Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(client)
            .build()
            .create(TestApi::class.java)
    }

    @Test
    fun `request without NoAuthorization gets a Bearer Authorization header from the token provider`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(""))

        client("abc123").normalApiEndPoint()

        val header = mockWebServer.takeRequest().headers[ApiHeaders.Names.AUTHORIZATION]
        assertThat(header).isEqualTo("${ApiHeaders.Values.BEARER_PREFIX}abc123")
    }

    @Test
    fun `request without NoAuthorization and no access token throws UnauthenticatedException`() = runTest {
        var thrown: UnauthenticatedException? = null

        try {
            client(accessToken = null).normalApiEndPoint()
        } catch (e: UnauthenticatedException) {
            thrown = e
        }

        assertThat(thrown).isNotNull()
    }

    @Test
    fun `request tagged with a NoAuthorization method skips token lookup and Authorization header`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(""))

        client(accessToken = null).skipAuthEndpoint()

        val header = mockWebServer.takeRequest().headers[ApiHeaders.Names.AUTHORIZATION]
        assertThat(header).isNull()
    }

    @Test
    fun `caller-supplied Authorization header at the call site is left untouched and the token provider is not consulted`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(""))

        client(accessToken = null).endpointWithCallerHeader("Bearer caller-supplied")

        val headers = mockWebServer.takeRequest().headers.values(ApiHeaders.Names.AUTHORIZATION)
        assertThat(headers).containsExactly("Bearer caller-supplied")
    }
}
