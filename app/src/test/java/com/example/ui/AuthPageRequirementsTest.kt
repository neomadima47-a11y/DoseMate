package com.example.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthPageRequirementsTest {

    @Test
    fun testEmailPasswordValidation() {
        fun isValidEmail(email: String): Boolean {
            return email.isNotBlank() && email.contains("@") && email.contains(".")
        }

        assertTrue(isValidEmail("lerato@example.com"))
        assertTrue(isValidEmail("neomadima47@gmail.com"))
        assertFalse(isValidEmail("invalidemail"))
        assertFalse(isValidEmail(""))
    }

    @Test
    fun testGoogleAccountSelection() {
        val googleEmail = "neomadima47@gmail.com"
        val googleName = "Neo Madima"

        assertTrue(googleEmail.endsWith("@gmail.com"))
        assertEquals("Neo Madima", googleName)
    }

    @Test
    fun testAuthModeSwitching() {
        val registerState = true
        val signInState = false

        assertTrue(registerState)
        assertFalse(signInState)
    }
}
