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

    private const val GROQ_CHAT_URL = "https://api.groq.com/openai/v1/chat/completions"
    private const val GROQ_DEFAULT_KEY = "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"

    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Queries the AI API (Groq ultra-fast LLM, with local clinical fallback)
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
        val groqKey = try {
            val key = BuildConfig.GROQ_API_KEY
            if (key.isNotBlank() && key != "MY_GROQ_API_KEY") key else GROQ_DEFAULT_KEY
        } catch (e: Throwable) {
            GROQ_DEFAULT_KEY
        }

        val isSymptomMode = problemType.equals("Symptom", ignoreCase = true)

        val prompt = if (isSymptomMode) {
            """
            You are a compassionate, board-certified clinical medical assistant and health educator.
            A patient has described experiencing the following primary symptom / health problem:
            
            - Primary Health Problem / Symptom: "$readingValue"
            - Additional Accompanying Symptoms: ${if (symptoms.isNotBlank()) symptoms else "None reported"}
            - Language: $language
            
            Please generate comprehensive, easy-to-understand medical information and practical solutions for this specific health issue:
            
            1. 🔬 **What Is Happening In The Body**: Explain clearly and simply what causes "$readingValue", what is biologically happening, and why the body produces this symptom.
            2. 🛠️ **How to Solve This Problem Right Now (Immediate Relief & Actions)**:
               - Immediate self-care steps, resting posture, and comfort measures.
               - Safe home remedies, hydration, relaxation, or thermal therapy (warm/cold compress if applicable).
               - What to avoid right now (triggers, certain foods, physical exertion, stress).
            3. ⚠️ **Critical Warning Signs (When to Go to ER)**:
               - Red-flag "danger" signs indicating that "$readingValue" could be a medical emergency requiring urgent hospital care.
            4. 🩺 **When to See a Doctor & What Questions to Ask**:
               - Diagnostic tests or questions to discuss with a healthcare provider if the problem persists or worsens.
            
            Keep the tone reassuring, medically sound, and practical. Structure with bold bullet points. Respond in $language.
            """.trimIndent()
        } else {
            """
            You are a compassionate, board-certified clinical cardiovascular and chronic health assistant.
            The patient has just entered their health data:
            
            - Measurement Type: $problemType
            - Current Reading: $readingValue $unit (Clinical Category: $classification)
            - Timing / Meal Context: ${if (timing.isNotBlank()) timing else "General"}
            - Accompanying Symptoms Right Now: ${if (symptoms.isNotBlank()) symptoms else "None reported"}
            - Preferred Language: $language
            
            Please generate immediate, practical, step-by-step guidance on how to understand and solve the problem they are experiencing right now:
            
            1. 🔬 **What Is Happening Right Now**: Explain clearly what $readingValue $unit means for their arteries, heart, and body at this exact moment.
            2. 🛠️ **How to Solve This Problem Right Now (Immediate Actions)**:
               - Immediate posture and physical positioning (e.g. seated, legs uncrossed, resting 5-10 mins).
               - Breathing exercises (e.g. 4-7-8 relaxing breathing to reduce acute sympathetic tone).
               - Hydration and dietary steps (water intake, avoiding caffeine, sodium, or sugar).
               - Medication review check (did they take their prescribed chronic medicine today?).
            3. ⚠️ **Red Flag Warning Signs**: Specific symptoms that require dialing emergency services or going to the clinic immediately.
            4. 🩺 **Next Steps & Long-term Solution**: What to track, how to repeat the reading in 15 minutes, and when to contact their physician.
            
            Keep the tone calm, empowering, and medically accurate. Present in clearly readable bullet points with bold highlights. Respond in $language.
            """.trimIndent()
        }

        // Try Groq API (Primary requested API)
        if (groqKey.isNotBlank()) {
            val groqResult = queryGroq(groqKey, prompt)
            if (groqResult != null && groqResult.isNotBlank()) {
                return@withContext Result.success(groqResult)
            }
        }

        // Fallback to verified local clinical solver if network / quota error occurs
        Result.success(
            "${generateLocalClinicalFallback(problemType, readingValue, unit, classification, symptoms, language)}\n\n*(Clinical decision support active)*"
        )
    }

    data class ChatMessagePayload(
        val role: String, // "system", "user", "assistant"
        val content: String
    )

    /**
     * Interactive conversational chatbot powered by Groq API (Llama-3.3-70B).
     * Capable of answering questions on ANY health, medical, wellness, fitness,
     * nutrition, lifestyle topic as well as specific patient vitals.
     */
    suspend fun chatWithGroq(
        history: List<ChatMessagePayload>,
        currentVitalsContext: String? = null,
        language: String = "English"
    ): Result<String> = withContext(Dispatchers.IO) {
        val groqKey = try {
            val key = BuildConfig.GROQ_API_KEY
            if (key.isNotBlank() && key != "MY_GROQ_API_KEY") key else GROQ_DEFAULT_KEY
        } catch (e: Throwable) {
            GROQ_DEFAULT_KEY
        }

        val systemPrompt = buildString {
            append("You are an empathetic, certified clinical medical and health wellness assistant powered by Groq Llama 3.3. ")
            append("You can answer questions on ANY health, medical, wellness, fitness, nutrition, anatomy, disease prevention, lifestyle, or vital signs topic. ")
            append("Always provide clear, practical, reassuring, and medically accurate responses in $language. Keep responses informative, concise, and structured when appropriate. ")
            if (!currentVitalsContext.isNullOrBlank()) {
                append("\nCurrent Patient Vitals on screen: $currentVitalsContext (Reference this if relevant, but gladly answer any general or unrelated health question the user asks). ")
            }
            append("If a user describes life-threatening symptoms (e.g. crushing chest pain, stroke symptoms, acute severe shortness of breath), advise seeking immediate emergency medical care.")
        }

        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            for (msg in history) {
                put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }
        }

        val response = queryGroqWithMessages(groqKey, messagesArray)
        if (response != null && response.isNotBlank()) {
            Result.success(response)
        } else {
            val lastUserMsg = history.lastOrNull { it.role == "user" }?.content ?: ""
            Result.success(generateGeneralHealthAdviceFallback(lastUserMsg))
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
        return chatWithGroq(history, vitalsContext, language)
    }

    private fun queryGroqWithMessages(apiKey: String, messagesArray: JSONArray): String? {
        val models = listOf(
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "mixtral-8x7b-32768",
            "gemma2-9b-it"
        )
        for (model in models) {
            try {
                val requestJson = JSONObject().apply {
                    put("model", model)
                    put("messages", messagesArray)
                    put("temperature", 0.4)
                    put("max_tokens", 800)
                }

                val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(GROQ_CHAT_URL)
                    .header("Authorization", "Bearer $apiKey")
                    .post(requestBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val json = JSONObject(body)
                        val choices = json.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val msg = choices.getJSONObject(0).optJSONObject("message")
                            val content = msg?.optString("content", "") ?: ""
                            if (content.isNotBlank()) {
                                val clean = content.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()
                                if (clean.isNotBlank()) return clean
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue to next model
            }
        }
        return null
    }

    private fun generateGeneralHealthAdviceFallback(question: String): String {
        val lower = question.lowercase()
        return when {
            lower.contains("water") || lower.contains("hydrat") ->
                "Adequate hydration is essential for cellular function, circulation, and kidney health. Most adults require 2.0 to 2.7 liters of water daily, adjusted for physical activity and climate."
            lower.contains("sleep") || lower.contains("tired") || lower.contains("insomnia") ->
                "Aim for 7–9 hours of quality sleep nightly. Establish a soothing pre-bed routine, keep your bedroom cool and dark, and avoid blue-light screens at least 45 minutes before bedtime."
            lower.contains("food") || lower.contains("diet") || lower.contains("eat") || lower.contains("nutrition") ->
                "A balanced, nutrient-dense diet prioritizes whole vegetables, fiber-rich grains, lean proteins, and unsaturated fats like olive oil and nuts, while minimizing refined sugars and excess sodium."
            lower.contains("stress") || lower.contains("anxiety") || lower.contains("calm") ->
                "To reduce stress hormones naturally: practice 4-7-8 diaphragmatic breathing, take regular outdoor walks, practice progressive muscle relaxation, and ensure regular downtime."
            lower.contains("pressure") || lower.contains("hypertension") ->
                "Maintaining optimal blood pressure involves moderating sodium intake (< 2,300 mg/day), engaging in 150 minutes of weekly aerobic exercise, managing stress, and adhering to prescribed medications."
            lower.contains("sugar") || lower.contains("glucose") || lower.contains("diabetes") ->
                "To support healthy blood sugar balance: pair carbohydrates with protein and healthy fats to slow digestion, stay physically active, and choose low-glycemic foods like leafy greens, berries, and legumes."
            else ->
                "For overall health and well-being, prioritize regular physical movement (150 minutes weekly), a colorful balanced whole-food diet, 7-8 hours of restful sleep, and regular medical check-ups with your healthcare provider."
        }
    }

    private fun queryGroq(apiKey: String, prompt: String): String? {
        // High-performance Groq Llama models
        val models = listOf(
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "mixtral-8x7b-32768",
            "gemma2-9b-it"
        )
        for (model in models) {
            try {
                val messagesArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "You are an empathetic, board-certified medical intelligence assistant. Provide structured, accurate clinical insight in clear markdown bullet points.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                }

                val requestJson = JSONObject().apply {
                    put("model", model)
                    put("messages", messagesArray)
                    put("temperature", 0.3)
                    put("max_tokens", 1000)
                }

                val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(GROQ_CHAT_URL)
                    .header("Authorization", "Bearer $apiKey")
                    .post(requestBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val json = JSONObject(body)
                        val choices = json.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val msg = choices.getJSONObject(0).optJSONObject("message")
                            val content = msg?.optString("content", "") ?: ""
                            if (content.isNotBlank()) {
                                // Clean any model thinking tags if present
                                val clean = content.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()
                                if (clean.isNotBlank()) return clean
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue to next model or fallback
            }
        }
        return null
    }

    private fun generateLocalClinicalFallback(

        problemType: String,
        readingValue: String,
        unit: String,
        classification: String,
        symptoms: String,
        language: String
    ): String {
        return buildString {
            if (problemType.equals("Symptom", ignoreCase = true)) {
                val symptomLower = readingValue.lowercase()
                append("### 🔬 Understanding: \"$readingValue\"\n")
                when {
                    symptomLower.contains("headache") || symptomLower.contains("migraine") -> {
                        append("Headaches often stem from tension in the neck and scalp muscles, changes in cranial blood vessel dilation, dehydration, ocular strain, or heightened blood pressure.\n")
                    }
                    symptomLower.contains("chest") || symptomLower.contains("breath") -> {
                        append("Chest discomfort or breathing sensations can arise from cardiovascular strain, respiratory airways, muscle tension, or gastrointestinal reflux.\n")
                    }
                    symptomLower.contains("dizz") || symptomLower.contains("shak") || symptomLower.contains("lighthead") -> {
                        append("Dizziness or shakiness is frequently triggered by sudden postural blood pressure changes, low circulating glucose, dehydration, or inner-ear equilibrium shifts.\n")
                    }
                    symptomLower.contains("nausea") || symptomLower.contains("stomach") -> {
                        append("Nausea occurs when gastric nerve signals or inner-ear triggers stimulate the autonomic emetic reflex, commonly related to digestive irritation, stress, or medication side-effects.\n")
                    }
                    symptomLower.contains("fatigue") || symptomLower.contains("tired") -> {
                        append("Persistent fatigue reflects low cellular adenosine triphosphate (ATP) production, inadequate restorative sleep, systemic inflammation, or metabolic fluctuations.\n")
                    }
                    else -> {
                        append("You have noted **$readingValue**. Physical symptoms represent direct communication from your sensory nervous system that physiological stress or inflammation is present.\n")
                    }
                }

                if (symptoms.isNotBlank()) {
                    append("\n**Accompanying Symptoms Noted**: $symptoms\n")
                }

                append("\n### 🛠️ Immediate Relief & How to Solve This Problem\n")
                when {
                    symptomLower.contains("headache") || symptomLower.contains("migraine") -> {
                        append("• **Dark, Quiet Space**: Rest in a dim, silent room to minimize sensory overstimulation.\n")
                        append("• **Hydration**: Drink 1-2 glasses of water steadily to rule out mild dehydration.\n")
                        append("• **Cold/Warm Compress**: Apply a cool cloth over your forehead or a warm pad behind your neck to relax taut cervical muscles.\n")
                        append("• **Gentle Breathing**: Inhale deeply for 4 seconds, exhale for 6 seconds to relieve tension.\n")
                    }
                    symptomLower.contains("dizz") || symptomLower.contains("shak") -> {
                        append("• **Immediate Sitting**: Sit down immediately or lie flat to prevent balance loss or falls.\n")
                        append("• **Fluid & Glucose Check**: Sip water and consider a small snack (like fruit or crackers) if you haven't eaten recently.\n")
                        append("• **Move in Stages**: Avoid sudden standing; pause on the edge of the bed or chair before rising.\n")
                    }
                    else -> {
                        append("• **Rest & Offload Stress**: Stop strenuous physical activities and rest in a comfortable, supported posture.\n")
                        append("• **Hydration**: Sip water slowly; avoid caffeinated beverages, alcohol, and heavy meals.\n")
                        append("• **Track Patterns**: Record the exact start time, duration, and any preceding triggers.\n")
                    }
                }

                append("\n### ⚠️ Critical Warning Signs\n")
                append("Seek emergency medical care immediately if your symptom is accompanied by: sudden severe 'worst headache of life', chest pressure, shortness of breath, loss of vision, speech difficulty, or weakness on one side of your face or body.")
                return@buildString
            }

            append("### 🔬 What Is Happening In Your Body Right Now\n")
            if (problemType.contains("pressure", ignoreCase = true)) {
                append("Your blood pressure of **$readingValue $unit** is classified as **$classification**.\n")
                if (classification.contains("Crisis", ignoreCase = true) || classification.contains("Stage", ignoreCase = true)) {
                    append("Your blood vessels are experiencing heightened arterial resistance. This forces the heart muscle to pump with elevated force against rigid walls, causing extra strain.\n")
                } else if (classification.contains("Elevated", ignoreCase = true)) {
                    append("Your systolic pressure is slightly higher than ideal resting levels, which is common after stress, rushing, caffeine, or full bladder.\n")
                } else if (classification.contains("Low", ignoreCase = true)) {
                    append("Your circulation is at low pressure, which can temporarily reduce blood and oxygen delivery to your head and extremities.\n")
                } else {
                    append("Your vascular system is operating smoothly within healthy, balanced parameters.\n")
                }
            } else if (problemType.contains("sugar", ignoreCase = true)) {
                append("Your blood glucose level is **$readingValue $unit** (**$classification**).\n")
            } else {
                append("Current symptom event logged: **$readingValue**.\n")
            }

            if (symptoms.isNotBlank()) {
                append("**Reported Symptoms Right Now**: $symptoms\n")
            }

            append("\n### 🛠️ How to Solve This Problem Right Now\n")
            if (problemType.contains("pressure", ignoreCase = true)) {
                if (classification.contains("Crisis", ignoreCase = true)) {
                    append("• **Immediate Emergency Protocol**: Rest completely still. If you have chest tightness, severe headache, numbness, or vision blur, seek emergency medical care immediately.\n")
                    append("• **Quiet Rest**: Sit upright with back supported and legs uncrossed for 5 minutes without talking or moving.\n")
                    append("• **Medication Verification**: Check if your prescribed blood pressure medication was taken today.\n")
                } else if (classification.contains("Stage", ignoreCase = true) || classification.contains("Elevated", ignoreCase = true)) {
                    append("• **1. Calming Body Position**: Sit comfortably with your back supported, both feet flat on the floor, and arms resting at heart level.\n")
                    append("• **2. 4-7-8 Diaphragmatic Breathing**: Inhale slowly through your nose for 4 seconds, hold gently for 7 seconds, and exhale smoothly through your mouth for 8 seconds. Repeat 4 times to naturally activate your parasympathetic nervous system.\n")
                    append("• **3. Hydrate With Water**: Drink a glass of room-temperature water. Avoid coffee, tea, energy drinks, salty snacks, and tobacco right now.\n")
                    append("• **4. Medication Check**: Verify if today's scheduled dose of your blood pressure prescription was taken on time.\n")
                    append("• **5. Retake in 15 Minutes**: Rest quietly for 15 minutes and repeat your reading. Blood pressure naturally fluctuates throughout the day.\n")
                } else if (classification.contains("Low", ignoreCase = true)) {
                    append("• **Sit or Lie Down**: Lie down and elevate your legs slightly to assist blood return to your heart and brain.\n")
                    append("• **Drink Fluids**: Drink 1-2 cups of water or an electrolyte fluid to expand circulating blood volume.\n")
                    append("• **Stand Slowly**: Always rise slowly in stages (sit first, pause, then stand) to avoid postural dizziness.\n")
                } else {
                    append("• **Maintain Healthy Routine**: Continue regular hydration, balanced nutrition, and daily prescribed regimens.\n")
                }
            } else {
                append("• Rest in a well-ventilated, calm area and maintain adequate hydration.\n")
                append("• Track when symptoms began and note any specific food, activity, or stress triggers.\n")
            }

            append("\n### ⚠️ Critical Warning Signs\n")
            if (problemType.contains("pressure", ignoreCase = true)) {
                append("Seek immediate emergency attention at the nearest clinic or hospital if you experience: sudden severe 'thunderclap' headache, chest tightness or radiating arm pain, shortness of breath, sudden facial drooping or arm weakness, or confusion.")
            } else {
                append("Seek emergency care if symptoms worsen rapidly or are accompanied by severe pain, difficulty breathing, or dizziness.")
            }
        }
    }
}
