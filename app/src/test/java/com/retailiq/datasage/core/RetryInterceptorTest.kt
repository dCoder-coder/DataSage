package com.retailiq.datasage.core

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.SocketException

class RetryInterceptorTest {

    private val interceptor = RetryInterceptor()

    @Test
    fun `retries once on SocketException and succeeds`() {
        val chain = mock<Interceptor.Chain>()
        val request = Request.Builder().url("https://api.test.com/api/v1/test").build()
        whenever(chain.request()).thenReturn(request)

        val successResponse = Response.Builder()
            .code(200)
            .message("OK")
            .protocol(Protocol.HTTP_1_1)
            .request(request)
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()

        // First call throws SocketException, second returns 200
        var callCount = 0
        whenever(chain.proceed(any())).thenAnswer {
            callCount++
            if (callCount == 1) throw SocketException("Connection reset")
            successResponse
        }

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
        assertEquals(2, callCount)
    }

    @Test
    fun `does not retry on normal response`() {
        val chain = mock<Interceptor.Chain>()
        val request = Request.Builder().url("https://api.test.com/api/v1/test").build()
        whenever(chain.request()).thenReturn(request)

        val response = Response.Builder()
            .code(200)
            .message("OK")
            .protocol(Protocol.HTTP_1_1)
            .request(request)
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()

        var callCount = 0
        whenever(chain.proceed(any())).thenAnswer {
            callCount++
            response
        }

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
        assertEquals(1, callCount)
    }
}
