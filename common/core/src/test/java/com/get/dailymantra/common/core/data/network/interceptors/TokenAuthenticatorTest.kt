package com.get.dailymantra.common.core.data.network.interceptors

import com.get.dailymantra.common.core.data.network.FakeAuthApi
import com.get.dailymantra.common.core.data.network.FakeTokenProvider
import com.get.dailymantra.common.core.data.network.RefreshTokenResponse
import com.get.dailymantra.common.core.data.security.AppEvent
import com.get.dailymantra.common.core.data.security.AppEventBus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class TokenAuthenticatorTest {

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

    private fun client(authenticator: TokenAuthenticator): OkHttpClient = OkHttpClient.Builder()
        .authenticator(authenticator)
        .build()

    private fun requestWithToken(token: String) = Request.Builder()
        .url(mockWebServer.url("/"))
        .header(ApiHeaders.Names.AUTHORIZATION, "${ApiHeaders.Values.BEARER_PREFIX}$token")
        .build()

    @Test
    fun `401 triggers a refresh and the retried request carries the new access token`() {
        val tokenProvider = FakeTokenProvider(accessToken = "old-access", refreshToken = "old-refresh")
        val authApi = FakeAuthApi(Result.success(RefreshTokenResponse("new-access", "new-refresh")))
        val authenticator = TokenAuthenticator(tokenProvider, authApi, AppEventBus())
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val response = client(authenticator).newCall(requestWithToken("old-access")).execute()

        assertThat(response.code).isEqualTo(200)
        mockWebServer.takeRequest() // the original, failing request
        val retriedRequest = mockWebServer.takeRequest()
        assertThat(retriedRequest.headers[ApiHeaders.Names.AUTHORIZATION]).isEqualTo("${ApiHeaders.Values.BEARER_PREFIX}new-access")
        assertThat(authApi.lastRequest?.refreshToken).isEqualTo("old-refresh")
    }

    @Test
    fun `refreshed tokens are persisted via tokenProvider`() = runTest {
        val tokenProvider = FakeTokenProvider(accessToken = "old-access", refreshToken = "old-refresh")
        val authApi = FakeAuthApi(Result.success(RefreshTokenResponse("new-access", "new-refresh")))
        val authenticator = TokenAuthenticator(tokenProvider, authApi, AppEventBus())
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        client(authenticator).newCall(requestWithToken("old-access")).execute()

        assertThat(tokenProvider.saveTokensCalls).isEqualTo(1)
        assertThat(tokenProvider.getRefreshToken()).isEqualTo("new-refresh")
        assertThat(tokenProvider.getAccessToken()).isEqualTo("new-access")
    }

    @Test
    fun `refresh failure clears tokens, notifies session expired, and does not retry`() = runTest {
        val tokenProvider = FakeTokenProvider(accessToken = "old-access", refreshToken = "old-refresh")
        val authApi = FakeAuthApi(Result.failure(IllegalStateException("refresh endpoint down")))
        val appEventBus = AppEventBus()
        val authenticator = TokenAuthenticator(tokenProvider, authApi, appEventBus)
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        val response = client(authenticator).newCall(requestWithToken("old-access")).execute()

        assertThat(response.code).isEqualTo(401)
        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(tokenProvider.clearTokensCalls).isEqualTo(1)
        assertThat(tokenProvider.getRefreshToken()).isNull()
        assertThat(tokenProvider.getAccessToken()).isNull()
        assertThat(appEventBus.events.first()).isEqualTo(AppEvent.SessionExpired)
    }

    @Test
    fun `missing refresh token gives up immediately without calling the refresh endpoint`() {
        val tokenProvider = FakeTokenProvider(accessToken = "old-access", refreshToken = null)
        val authApi = FakeAuthApi()
        val authenticator = TokenAuthenticator(tokenProvider, authApi, AppEventBus())
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        val response = client(authenticator).newCall(requestWithToken("old-access")).execute()

        assertThat(response.code).isEqualTo(401)
        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(authApi.lastRequest).isNull()
        assertThat(tokenProvider.clearTokensCalls).isEqualTo(1)
    }

    @Test
    fun `retries are capped at MAX_ATTEMPTS when the server keeps returning 401`() {
        val tokenProvider = FakeTokenProvider(accessToken = "old-access", refreshToken = "old-refresh")
        val authApi = FakeAuthApi(Result.success(RefreshTokenResponse("new-access", "new-refresh")))
        val authenticator = TokenAuthenticator(tokenProvider, authApi, AppEventBus())
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        val response = client(authenticator).newCall(requestWithToken("old-access")).execute()

        assertThat(response.code).isEqualTo(401)
        assertThat(mockWebServer.requestCount).isEqualTo(2)
    }

    @Test
    fun `a token already refreshed by a concurrent request is reused without calling the refresh endpoint`() {
        val tokenProvider = FakeTokenProvider(accessToken = "refreshed-by-other-call", refreshToken = "old-refresh")
        val authApi = FakeAuthApi()
        val authenticator = TokenAuthenticator(tokenProvider, authApi, AppEventBus())
        val failedResponse = Response.Builder()
            .request(requestWithToken("old-access"))
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()

        val retriedRequest = authenticator.authenticate(route = null, response = failedResponse)

        assertThat(retriedRequest?.header(ApiHeaders.Names.AUTHORIZATION)).isEqualTo("Bearer refreshed-by-other-call")
        assertThat(authApi.lastRequest).isNull()
    }
}
