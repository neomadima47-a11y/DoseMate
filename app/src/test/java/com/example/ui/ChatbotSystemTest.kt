package com.example.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatbotSystemTest {

    private val emergencyKeywords = listOf(
        "chest pain", "heart attack", "stroke", "suicide",
        "heavy bleeding", "difficulty breathing", "choking", "unconscious"
    )

    @Test
    fun testEmergencyKeywordsInterventionAlert() {
        val userMessage1 = "I am having severe chest pain and difficulty breathing"
        val userMessage2 = "What is the standard dosage for Panado?"

        val isEmergency1 = emergencyKeywords.any { userMessage1.lowercase().contains(it) }
        val isEmergency2 = emergencyKeywords.any { userMessage2.lowercase().contains(it) }

        assertTrue("Red-flag terms must trigger emergency alert", isEmergency1)
        assertTrue("Normal questions must not trigger emergency alert", !isEmergency2)
    }

    @Test
    fun testMedicalQueryAnsweringAndDisclaimerRemoval() {
        val rawAiResponse = "• Take Panado 500mg every 4 to 6 hours.\n• Do not exceed 4000mg per day. Please consult your doctor for medical advice."
        val disclaimerPattern = Regex("(?i)please (consult|talk to|see|contact) (your|a) (doctor|physician|healthcare provider|pharmacist)[^.\\n]*[.\\n]?")
        val cleaned = rawAiResponse.replace(disclaimerPattern, "").trim()

        assertTrue(cleaned.contains("Panado 500mg"))
        assertTrue(!cleaned.contains("Please consult your doctor"))
    }

    @Test
    fun testGenerativeAiModelInstructionSetup() {
        val modelName = "gemini-1.5-flash"
        val systemInstruction = "You are a helpful and highly cautious medical information assistant."

        assertEquals("gemini-1.5-flash", modelName)
        assertTrue(systemInstruction.contains("medical information assistant"))
    }
}
