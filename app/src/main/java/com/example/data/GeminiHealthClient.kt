package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiHealthClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun getGeminiApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY" && key != "MY_NEW_API_KEY_DEFAULT_VALUE") key.trim() else ""
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * Helper to clean any residual doctor disclaimer phrases from AI text
     */
    private fun stripDoctorDisclaimers(text: String): String {
        var clean = text
        val disclaimerPatterns = listOf(
            Regex("(?i)please (consult|talk to|see|contact) (your|a) (doctor|physician|healthcare provider|pharmacist)[^.\\n]*[.\\n]?"),
            Regex("(?i)consult (your|a) (doctor|physician|healthcare provider|pharmacist)[^.\\n]*[.\\n]?"),
            Regex("(?i)contact (your|a) (doctor|physician|healthcare provider|pharmacist)[^.\\n]*[.\\n]?"),
            Regex("(?i)speak (to|with) (your|a) (doctor|physician|healthcare provider|pharmacist)[^.\\n]*[.\\n]?"),
            Regex("(?i)always (consult|talk to|speak to|see) (your|a) (doctor|physician|healthcare provider|pharmacist)[^.\\n]*[.\\n]?")
        )

        for (pattern in disclaimerPatterns) {
            clean = clean.replace(pattern, "").trim()
        }

        return clean.ifBlank { text }
    }

    /**
     * Queries Google Gemini AI API
     * to get deep clinical insights, specific physiological explanations,
     * and immediate practical steps to manage health vitals and symptoms.
     */
    suspend fun getHealthProblemInsights(
        problemType: String,
        readingValue: String,
        unit: String,
        classification: String,
        symptoms: String,
        language: String,
        timing: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val geminiKey = getGeminiApiKey()

        val prompt = """
        You are an expert, helpful medical assistant for DoseMate (HealthBridge).
        The patient logged: $problemType ($readingValue $unit) - $classification. Notes: ${symptoms.ifBlank { "None" }}.

        Provide a simple, direct, short, and straightforward 3-bullet answer in $language:
        • Direct health status
        • Simple practical action step
        • Recommended dosage or care tip
        """.trimIndent()

        if (geminiKey.isNotBlank()) {
            val geminiResult = queryGemini(geminiKey, prompt)
            if (!geminiResult.isNullOrBlank()) {
                return@withContext Result.success(stripDoctorDisclaimers(geminiResult))
            }
        }

        Result.success(
            "${generateLocalClinicalFallback(problemType, readingValue, unit, classification, symptoms, language)}\n\n*(Clinical decision support active)*"
        )
    }

    data class ChatMessagePayload(
        val role: String, // "system", "user", "assistant"
        val content: String
    )

    /**
     * Alias for backward compatibility
     */
    suspend fun chatWithGroq(
        history: List<ChatMessagePayload>,
        currentVitalsContext: String? = null,
        language: String = "English"
    ): Result<String> = chatWithGemini(history, currentVitalsContext, language)

    /**
     * Interactive conversational chatbot powered directly by Google Gemini AI API.
     * Strictly optimized for direct, practical, straightforward medication, health, & DoseMate app answers.
     */
    suspend fun chatWithGemini(
        history: List<ChatMessagePayload>,
        currentVitalsContext: String? = null,
        language: String = "English"
    ): Result<String> = withContext(Dispatchers.IO) {
        val geminiKey = getGeminiApiKey()

        val systemPrompt = buildString {
            append("You are an expert, friendly, and practical medical AI assistant for DoseMate (HealthBridge), a personal community health and medication companion app in South Africa. ")
            append("You have full knowledge about general medical, prescription, dosage, side effect, drug interaction, administration, and wellness topics, as well as the DoseMate app.\n\n")
            append("DOSEMATE APP FEATURES AND KNOWLEDGE:\n")
            append("• Purpose: DoseMate (HealthBridge) helps users track chronic conditions (Hypertension, Diabetes), schedule daily medications, receive exact high-priority alarm notifications, inspect calendar adherence history, and locate local health clinics in South Africa.\n")
            append("• Medication Schedule Page: Tap '+ Add Med' to schedule prescriptions, pick 1x, 2x, 3x, or 4x daily frequency, and set exact notification alarm times for every dose.\n")
            append("• Medication History Page: Features an interactive 7-column monthly calendar with Green ('✓ Taken') and Orange ('⏰ Not Taken') date highlights. Click any date cell to view all prescriptions taken on that date.\n")
            append("• Clinics Page: Interactive MapTiler map with OSRM turn-by-turn road navigation and Geocoding search for hospitals and health centers in Pretoria, Johannesburg, and South Africa.\n")
            append("• Settings Page: Profile management, South African language selection (English, isiZulu, Sesotho, Afrikaans, Sepedi, Setswana, isiXhosa), dark mode, emergency contacts, and automated Room & Firebase Cloud Firestore sync.\n\n")
            append("STRICT RULES FOR YOUR RESPONSES:\n")
            append("1. Answer EVERY medication or health question directly, simply, accurately, and immediately in clear plain language right at the start.\n")
            append("2. If asked about medication dosages (e.g. Panado, Paracetamol, Ibuprofen, Metformin, Amlodipine, Lisinopril, Aspirin, etc.), give the exact standard adult dosage numbers directly (e.g. Panado: 500mg-1000mg per dose every 4-6 hours, max 4000mg/4g per 24 hours).\n")
            append("3. Keep responses structured using 2 to 4 simple, short bullet points so they are easy to read and straightforward.\n")
            append("4. Do NOT use generic refusal phrases or unnecessary disclaimers. Provide the direct practical answer first.\n")
            append("5. Respond in $language.\n")
            if (!currentVitalsContext.isNullOrBlank()) {
                append("Patient Context on file: $currentVitalsContext. ")
            }
        }

        if (geminiKey.isNotBlank()) {
            val geminiResponse = queryGeminiWithMessages(geminiKey, systemPrompt, history)
            if (!geminiResponse.isNullOrBlank()) {
                return@withContext Result.success(stripDoctorDisclaimers(geminiResponse))
            }
        }

        val lastUserMsg = history.lastOrNull { it.role == "user" }?.content ?: ""
        Result.success(generateGeneralHealthAdviceFallback(lastUserMsg))
    }

    /**
     * Backward-compatible direct question query
     */
    suspend fun askGroqDirectQuestion(
        userQuestion: String,
        problemType: String,
        readingValue: String,
        unit: String,
        symptoms: String,
        language: String
    ): Result<String> {
        val vitalsContext = "$problemType: $readingValue $unit" + if (symptoms.isNotBlank()) ", Symptoms: $symptoms" else ""
        val history = listOf(ChatMessagePayload(role = "user", content = userQuestion))
        return chatWithGemini(history, vitalsContext, language)
    }

    private fun queryGemini(apiKey: String, prompt: String): String? {
        val models = listOf(
            "gemini-flash-latest",
            "gemini-3.8-flash",
            "gemini-2.5-flash-lite",
            "gemini-pro-latest"
        )
        for (model in models) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().put(JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                    }))
                }

                val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val json = JSONObject(body)
                        val candidates = json.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val text = parts.getJSONObject(0).optString("text", "")
                                if (text.isNotBlank()) return stripDoctorDisclaimers(text)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun queryGeminiWithMessages(apiKey: String, systemPrompt: String, history: List<ChatMessagePayload>): String? {
        val models = listOf(
            "gemini-flash-latest",
            "gemini-3.8-flash",
            "gemini-2.5-flash-lite",
            "gemini-pro-latest"
        )
        for (model in models) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

                val contentsArray = JSONArray()
                for (msg in history) {
                    val role = if (msg.role == "assistant") "model" else "user"
                    contentsArray.put(JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().put(JSONObject().put("text", msg.content)))
                    })
                }

                val requestJson = JSONObject().apply {
                    if (systemPrompt.isNotBlank()) {
                        put("system_instruction", JSONObject().apply {
                            put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                        })
                    }
                    put("contents", contentsArray)
                }

                val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val json = JSONObject(body)
                        val candidates = json.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val text = parts.getJSONObject(0).optString("text", "")
                                if (text.isNotBlank()) return stripDoctorDisclaimers(text)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun generateGeneralHealthAdviceFallback(question: String): String {
        val lower = question.lowercase()
        return when {
            lower.contains("app") || lower.contains("dosemate") || lower.contains("healthbridge") || lower.contains("what is this") ->
                "🩺 **DoseMate (HealthBridge) App Overview**:\n\n" +
                "• **Purpose**: DoseMate helps you track chronic health conditions, schedule daily prescriptions, and receive exact alarm notifications.\n" +
                "• **Medication Schedule**: Add medications with 1x, 2x, 3x, or 4x daily frequency and set exact alarm times for every dose.\n" +
                "• **Calendar History**: Inspect your daily taken medications on the interactive calendar highlighted in Green (`✓ Taken`) or Orange (`⏰ Not Taken`).\n" +
                "• **Clinics & Directions**: Locate healthcare centers in South Africa with turn-by-turn road navigation."

            lower.contains("headache") || lower.contains("panado") || lower.contains("paracetamol") ->
                "💊 **Panado / Headache Direct Answer**:\n\n" +
                "• **What to take**: Panado (Paracetamol 500mg) or Ibuprofen 400mg with a full glass of water.\n" +
                "• **Standard Panado Dosage**: 500mg to 1,000mg (1 or 2 tablets of 500mg) every 4 to 6 hours as needed.\n" +
                "• **Maximum Limit**: Do not take more than 4,000mg (4 grams / 8 tablets) in 24 hours."

            lower.contains("metformin") || lower.contains("sugar") || lower.contains("diabetes") ->
                "💊 **Metformin Quick Answer**:\n\n" +
                "• **What it does**: Lowers blood sugar in Type 2 Diabetes.\n" +
                "• **How to take**: Take with or right after meals to avoid stomach upset.\n" +
                "• **Missed dose**: Take as soon as you remember, unless it is time for your next dose."

            lower.contains("amlodipine") || lower.contains("lisinopril") || lower.contains("pressure") || lower.contains("hypertension") ->
                "❤️ **Blood Pressure Medicine Quick Answer**:\n\n" +
                "• **What it does**: Lowers high blood pressure to keep your heart healthy.\n" +
                "• **How to take**: Take once daily with water at the same time every day.\n" +
                "• **Precaution**: Avoid taking Ibuprofen concurrently if on blood pressure medication."

            lower.contains("miss") || lower.contains("forget") || lower.contains("forgot") ->
                "⏰ **Missed Dose Quick Answer**:\n\n" +
                "• Take the missed dose as soon as you remember.\n" +
                "• If it's almost time for your next dose, skip the missed one. Never double up doses."

            lower.contains("ibuprofen") || lower.contains("pain") ->
                "💊 **Ibuprofen Quick Answer**:\n\n" +
                "• **Dosage**: Standard adult dose is 200mg to 400mg every 6 to 8 hours.\n" +
                "• **How to take**: Always take with food or milk to protect your stomach."

            lower.contains("water") || lower.contains("hydrat") ->
                "💧 **Hydration Target**: Drink 2 to 2.5 liters of water daily to support circulation and kidney health."

            else ->
                "🩺 **DoseMate AI Health Guidance**:\n\n" +
                "• **App Purpose**: DoseMate manages your daily medication schedule, calendar history, and local clinic directions.\n" +
                "• **Medication Dosage**: Take Panado 500mg - 1000mg per dose (max 4000mg daily) with water."
        }
    }

    private fun generateLocalClinicalFallback(
        type: String,
        valStr: String,
        unit: String,
        classif: String,
        syms: String,
        lang: String
    ): String {
        return "🩺 **Simple Guidance**: $classif ($valStr $unit). Take prescribed medications with water at regular daily times."
    }
}
