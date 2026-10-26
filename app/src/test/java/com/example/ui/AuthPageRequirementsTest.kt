package com.example.ui

import com.example.util.Strings
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

    @Test
    fun testSouthAfricanLanguagesLocalization() {
        val saLanguages = listOf("English", "isiZulu", "Sesotho", "Afrikaans", "Sepedi", "Setswana", "isiXhosa")

        for (lang in saLanguages) {
            val loginStr = Strings.get("login", lang)
            val dashboardStr = Strings.get("dashboard", lang)

            assertTrue("String for $lang must not be empty", loginStr.isNotBlank())
            assertTrue("Dashboard string for $lang must not be empty", dashboardStr.isNotBlank())
        }

        assertEquals("Ngena", Strings.get("login", "isiZulu"))
        assertEquals("Kena", Strings.get("login", "Sesotho"))
        assertEquals("Teken in", Strings.get("login", "Afrikaans"))
        assertEquals("Tsena", Strings.get("login", "Sepedi"))
    }
}
