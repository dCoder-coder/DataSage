package com.retailiq.datasage.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalErrorHandler @Inject constructor(
    private val tokenStore: TokenStore,
    private val authEventBus: AuthEventBus
) : Interceptor {

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: SocketTimeoutException) {
            Timber.w(e, "Connection timed out for ${request.url}")
            _snackbarEvents.tryEmit("Connection timed out.")
            throw e
        }

        when (response.code) {
            401 -> {
                // Skip if this is already the refresh endpoint to avoid loops
                if (!request.url.encodedPath.contains("/auth/refresh") &&
                    !request.url.encodedPath.contains("/auth/login")) {
                    Timber.w("401 received — clearing tokens and emitting Logout")
                    tokenStore.clearTokens()
                    authEventBus.emit(AuthEvent.Logout)
                }
            }
            429 -> {
                _snackbarEvents.tryEmit("Too many requests. Please wait a moment.")
            }
            in 500..599 -> {
                _snackbarEvents.tryEmit("Server error. Please try again.")
            }
        }

        return response
    }
}
