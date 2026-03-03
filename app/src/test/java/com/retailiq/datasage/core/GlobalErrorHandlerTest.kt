package com.retailiq.datasage.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalErrorHandlerTest {

    private lateinit var tokenStore: TokenStore
    private lateinit var authEventBus: AuthEventBus
    private lateinit var handler: GlobalErrorHandler

    @Before
    fun setup() {
        tokenStore = mock()
        authEventBus = AuthEventBus()
        handler = GlobalErrorHandler(tokenStore, authEventBus)
    }

    private fun mockChainWithCode(code: Int): Interceptor.Chain {
        val chain = mock<Interceptor.Chain>()
        val request = Request.Builder().url("https://api.test.com/api/v1/test").build()
        whenever(chain.request()).thenReturn(request)
        val response = Response.Builder()
            .code(code)
            .message("Test")
            .protocol(Protocol.HTTP_1_1)
            .request(request)
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()
        whenever(chain.proceed(any())).thenReturn(response)
        return chain
    }

    @Test
    fun `on 401 emits Logout event and clears tokens`() = runTest {
        val chain = mockChainWithCode(401)
        var capturedEvent: AuthEvent? = null

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            capturedEvent = authEventBus.events.first()
        }

        handler.intercept(chain)

        assertTrue("Expected AuthEvent.Logout", capturedEvent is AuthEvent.Logout)
        verify(tokenStore).clearTokens()
        job.cancel()
    }

    @Test
    fun `on 429 emits rate-limit snackbar event`() = runTest {
        val chain = mockChainWithCode(429)
        var capturedMessage: String? = null

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            capturedMessage = handler.snackbarEvents.first()
        }

        handler.intercept(chain)

        assertEquals("Too many requests. Please wait a moment.", capturedMessage)
        job.cancel()
    }

    @Test
    fun `on 503 emits server error snackbar event`() = runTest {
        val chain = mockChainWithCode(503)
        var capturedMessage: String? = null

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            capturedMessage = handler.snackbarEvents.first()
        }

        handler.intercept(chain)

        assertEquals("Server error. Please try again.", capturedMessage)
        job.cancel()
    }
}
