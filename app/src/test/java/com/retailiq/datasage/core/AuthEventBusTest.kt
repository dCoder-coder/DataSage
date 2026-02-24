package com.retailiq.datasage.core

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthEventBusTest {
    @Test
    fun emitsSessionExpiredEvent() = runBlocking {
        val bus = AuthEventBus()
        val observed = async { bus.events.first() }

        bus.emit(AuthEvent.SessionExpired)

        assertEquals(AuthEvent.SessionExpired, observed.await())
    }
}
