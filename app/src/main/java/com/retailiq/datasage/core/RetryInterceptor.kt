package com.retailiq.datasage.core

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.net.SocketException
import java.net.UnknownHostException

/**
 * OkHttp Interceptor that retries once on transient network errors
 * (SocketException, UnknownHostException). Does NOT retry on HTTP 4xx/5xx.
 */
class RetryInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        return try {
            chain.proceed(request)
        } catch (e: SocketException) {
            Timber.w(e, "SocketException — retrying once: ${request.url}")
            chain.proceed(request)
        } catch (e: UnknownHostException) {
            Timber.w(e, "UnknownHostException — retrying once: ${request.url}")
            chain.proceed(request)
        }
    }
}
