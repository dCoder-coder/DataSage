package com.retailiq.datasage.ui.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {
    @Test
    fun validMobile_acceptsIndian10DigitRange() {
        assertTrue(AuthValidation.isValidMobile("9876543210"))
        assertFalse(AuthValidation.isValidMobile("5876543210"))
        assertFalse(AuthValidation.isValidMobile("987654321"))
    }

    @Test
    fun strongPassword_requiresLengthAndDigit() {
        assertTrue(AuthValidation.isStrongPassword("password1"))
        assertFalse(AuthValidation.isStrongPassword("pass"))
        assertFalse(AuthValidation.isStrongPassword("password"))
    }
}
