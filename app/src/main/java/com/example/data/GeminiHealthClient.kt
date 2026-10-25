package com.example.data

import com.example.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
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

    // Red-flag terms that trigger an immediate local override for user safety
    private val emergencyKeywords = listOf(
        "chest pain", "heart attack", "stroke", "suicide",
        "heavy bleeding", "difficulty breathing", "choking", "unconscious"
    )

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
     * Local reference database to ground the AI's answers with verified medical clinical data
     */
    private fun getVerifiedMedicalContext(userQuery: String): String {
        val query = userQuery.lowercase()
        return when {
            query.contains("diabetes") || query.contains("sugar") || query.contains("glucose") || query.contains("metformin") -> {
                "Context: Diabetes occurs when blood glucose is too high. Type 1 is autoimmune (no insulin production). " +
                "Type 2 is lifestyle/genetic (insulin resistance). Common symptoms include increased thirst, frequent urination, " +
                "and fatigue. Management involves glucose tracking, metformin (500mg-1000mg with meals), insulin therapy, diet, and exercise."
            }
            query.contains("hypertension") || query.contains("blood pressure") || query.contains("amlodipine") || query.contains("lisinopril") -> {
                "Context: Hypertension (high blood pressure) is often a silent condition with few visible symptoms. " +
                "Long-term risks include heart attack, stroke, and kidney damage. Standard treatments include lifestyle changes " +
                "(low-sodium diet, exercise) and medications like ACE inhibitors, beta-blockers, or calcium channel blockers (Amlodipine 5mg/10mg daily)."
            }
            query.contains("asthma") || query.contains("wheez") || query.contains("inhaler") -> {
                "Context: Asthma is a chronic condition causing inflammation and narrowing of the airways. " +
                "Symptoms include wheezing, shortness of breath, chest tightness, and coughing. Triggers include allergens, cold air, " +
                "and exercise. Managed using quick-relief rescue inhalers (albuterol) and long-term control inhalers (corticosteroids)."
            }
            query.contains("headache") || query.contains("panado") || query.contains("paracetamol") || query.contains("fever") -> {
                "Context: Panado / Paracetamol is a first-line analgesic and antipyretic. " +
                "Standard adult dosage: 500mg to 1,000mg (1 or 2 tablets of 500mg) every 4 to 6 hours as needed. " +
                "Maximum daily limit is 4,000mg (4 grams / 8 tablets) per 24 hours. Always drink water."
            }
            query.contains("ibuprofen") || query.contains("inflammation") || query.contains("pain") -> {
                "Context: Ibuprofen is a nonsteroidal anti-inflammatory drug (NSAID). " +
                "Standard adult dosage is 200mg to 400mg every 6 to 8 hours with food or milk to protect stomach lining. " +
                "Do not exceed 1,200mg daily without doctor supervision."
            }
            else -> ""
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

        // Check Emergency Keywords first
        val combinedText = "$problemType $classification $symptoms".lowercase()
        if (emergencyKeywords.any { combinedText.contains(it) }) {
            return@withContext Result.success(
                "⚠️ EMERGENCY ALERT:\nThe symptoms you entered may require immediate medical attention. Please stop using this chat app and contact emergency services or proceed to the nearest hospital right away."
            )
        }

        val prompt = """
        You are an expert, helpful medical assistant for DoseMate (HealthBridge).
        The patient logged: $problemType ($readingValue $unit) - $classification. Notes: ${symptoms.ifBlank { "None" }}.

        Provide a simple, direct, short, and straightforward 3-bullet answer in $language:
        • Direct health status
        • Simple practical action step
        • Recommended dosage or care tip
        """.trimIndent()

        if (geminiKey.isNotBlank()) {
            val sdkResult = queryGenerativeSdk(geminiKey, "You are a helpful medical information assistant.", prompt)
            if (!sdkResult.isNullOrBlank()) {
                return@withContext Result.success(stripDoctorDisclaimers(sdkResult))
            }

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
     * Interactive conversational chatbot powered directly by Official Google Generative AI SDK & Gemini API.
     * Unrestricted to answer ANY medical, health, dosage, symptom, treatment, or wellness question asked by the user.
     */
    suspend fun chatWithGemini(
        history: List<ChatMessagePayload>,
        currentVitalsContext: String? = null,
        language: String = "English"
    ): Result<String> = withContext(Dispatchers.IO) {
        val geminiKey = getGeminiApiKey()

        val lastUserMsg = history.lastOrNull { it.role == "user" }?.content ?: ""
        val lowercaseMessage = lastUserMsg.lowercase()

        // 1. Red-flag terms check: immediate local override for user safety
        if (emergencyKeywords.any { lowercaseMessage.contains(it) }) {
            return@withContext Result.success(
                "⚠️ EMERGENCY ALERT:\nThe symptoms you entered may require immediate medical attention. Please stop using this chat app and contact emergency services or proceed to the nearest hospital right away."
            )
        }

        val cleanQuery = lastUserMsg.trim()
        val verifiedContext = getVerifiedMedicalContext(cleanQuery)

        val groundedPrompt = if (verifiedContext.isNotEmpty()) {
            "Use the following verified clinical data to answer the user's query clearly, directly, and professionally in 2 to 4 bullet points in $language.\n" +
            "$verifiedContext\n\n" +
            "User Query: $cleanQuery"
        } else {
            "Provide general educational medical information regarding the following query clearly and directly in 2 to 4 bullet points in $language: $cleanQuery"
        }

        val systemPrompt = buildString {
            append("You are an all-purpose medical and wellness assistant for DoseMate (HealthBridge) with deep expertise in health, wellness, medications, symptoms, and lifestyle habits.\n\n")
            append("INSTRUCTIONS:\n")
            append("1. You must answer ANY general, casual, technical, medical, health, medication, or lifestyle question the user asks directly in $language.\n")
            append("2. When answering health or lifestyle questions (diet, nutrition, workouts, sleep, pain, stress), provide actionable, science-backed advice in 2 to 4 bullet points.\n")
            append("3. Never refuse to answer or output generic 'call your doctor' statements.\n")
            append("4. If symptoms describe a life-threatening emergency (like chest pain, choking, severe bleeding), instruct them immediately to visit emergency services.\n")
            if (!currentVitalsContext.isNullOrBlank()) {
                append("Patient Vitals Context: $currentVitalsContext. ")
            }
        }

        if (geminiKey.isNotBlank()) {
            // Try official Google Generative AI SDK (gemini-1.5-flash) with custom SafetySettings
            val sdkResponse = queryGenerativeSdk(geminiKey, systemPrompt, groundedPrompt)
            if (!sdkResponse.isNullOrBlank()) {
                return@withContext Result.success(stripDoctorDisclaimers(sdkResponse))
            }

            // Fallback to REST endpoint
            val geminiResponse = queryGeminiWithMessages(geminiKey, systemPrompt, history)
            if (!geminiResponse.isNullOrBlank()) {
                return@withContext Result.success(stripDoctorDisclaimers(geminiResponse))
            }
        }

        Result.success(generateGeneralHealthAdviceFallback(cleanQuery))
    }

    /**
     * Official Google Generative AI SDK Integration (com.google.ai.client.generativeai.GenerativeModel)
     * Configured with BlockThreshold.NONE for dangerous content so lifestyle & health questions are never blocked.
     */
    private suspend fun queryGenerativeSdk(apiKey: String, systemPrompt: String, userMessage: String): String? {
        return try {
            val safetySettings = listOf(
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
            )

            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey,
                safetySettings = safetySettings,
                systemInstruction = content {
                    text(systemPrompt)
                }
            )
            val response = generativeModel.generateContent(userMessage)
            response.text?.trim()
        } catch (_: Exception) {
            null
        }
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
            "gemini-1.5-flash",
            "gemini-flash-latest",
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
            "gemini-1.5-flash",
            "gemini-flash-latest",
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

            lower.contains("skin") || lower.contains("rash") || lower.contains("itch") || lower.contains("eczema") ->
                "🩺 **Skin Care & Rash Guidance**:\n\n" +
                "• **Overview**: Skin irritation or rashes can result from allergies, eczema, or contact dermatitis.\n" +
                "• **Management**: Wash gently with mild fragrance-free soap, keep skin hydrated, and apply over-the-counter hydrocortisone cream if itchy."

            lower.contains("stomach") || lower.contains("nausea") || lower.contains("acid") || lower.contains("ulcer") ->
                "🩺 **Stomach & Digestive Care Guidance**:\n\n" +
                "• **Overview**: Stomach discomfort or acid reflux can stem from diet, gastritis, or digestion.\n" +
                "• **Management**: Drink warm water or antacids (such as Gaviscon), eat small frequent meals, and avoid spicy foods."

            lower.contains("eye") || lower.contains("vision") ->
                "🩺 **Eye Health Guidance**:\n\n" +
                "• **Overview**: Eye irritation can be caused by dry eyes, strain, or conjunctivitis.\n" +
                "• **Management**: Use lubricating eye drops, rest your eyes from digital screens, and avoid rubbing."

            lower.contains("cough") || lower.contains("flu") || lower.contains("cold") || lower.contains("fever") ->
                "🩺 **Cold, Cough & Fever Guidance**:\n\n" +
                "• **Overview**: Viral upper respiratory infections cause fever, sore throat, and cough.\n" +
                "• **Management**: Rest, stay hydrated with fluids, and take Panado 500mg every 4-6 hours for fever relief."

            lower.contains("vitamin") || lower.contains("supplement") ->
                "💊 **Vitamins & Supplements Guidance**:\n\n" +
                "• **Overview**: Essential vitamins support immunity, bone density, and energy levels.\n" +
                "• **Recommendation**: Take Vitamin D 1000 IU daily and Vitamin C 500mg with meals."

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

            else ->
                "🩺 **DoseMate AI Health Guidance**:\n\n" +
                "• **Overview**: $question.\n" +
                "• **Practical Step**: Drink water, rest, and follow recommended health guidelines for symptom relief."
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
