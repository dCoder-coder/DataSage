package com.retailiq.datasage.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleGuardTest {
    @Test
    fun ownerRolePredicate_behaviour() {
        fun isAllowed(role: String, required: String) = role.equals(required, ignoreCase = true)
        assertTrue(isAllowed("owner", "owner"))
        assertTrue(isAllowed("OWNER", "owner"))
        assertFalse(isAllowed("staff", "owner"))
    }
}
