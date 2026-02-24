package com.retailiq.datasage.core

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthManagerIntegrationTest {
    @Test
    fun saveReadAndClearTokens_workAsExpected() {
        val manager = AuthManager(ApplicationProvider.getApplicationContext())

        manager.saveTokens("access-token", "refresh-token")
        assertEquals("access-token", manager.getAccessToken())
        assertEquals("refresh-token", manager.getRefreshToken())

        manager.clearTokens()
        assertNull(manager.getAccessToken())
        assertNull(manager.getRefreshToken())
    }
}
