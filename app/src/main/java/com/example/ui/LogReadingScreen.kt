package com.example.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GeminiHealthClient
import com.example.ui.theme.HealthAmber
import com.example.ui.theme.HealthBlue
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.HealthRed
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealSecondary
import com.example.util.ClinicalEvaluation
import com.example.util.ClinicalEvaluator
import com.example.util.HealthSeverity
import com.example.util.Strings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatBotMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: String = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
)

@Composable
fun LogReadingScreen(
    language: String,
    onSaveReading: (type: String, value: String, unit: String, whenTaken: String, notes: String) -> Unit,
    onReadingSavedNav: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToMedications: () -> Unit = {},
    onNavigateToClinics: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Active Selection State
    var selectedType by remember { mutableStateOf("Blood pressure") } // "Blood pressure", "Blood sugar", "Symptom"

    // BP Specific State
    var systolicValue by remember { mutableStateOf(120) }
    var diastolicValue by remember { mutableStateOf(80) }

    // Blood Sugar Specific State
    var glucoseValue by remember { mutableStateOf(5.4) }

    // Symptom Specific State
    var symptomTextValue by remember { mutableStateOf("") }
    var selectedSymptoms by remember { mutableStateOf(setOf<String>()) }

    // Groq Conversational AI Chatbot State (Powered by Groq Llama-3.3-70B)
    var chatMessages by remember {
        mutableStateOf(
            listOf(
                ChatBotMessage(
                    isUser = false,
                    text = "Hello! I'm your Groq Clinical AI Assistant (powered by Llama 3.3 70B). I'm here to chat about anything—whether you want to discuss your readings on screen, or ask general questions about nutrition, sleep, exercise, medications, mental wellness, or any other health topic. How can I help you today?"
                )
            )
        )
    }
    var userChatInput by remember { mutableStateOf("") }
    var isGroqReplying by remember { mutableStateOf(false) }

    // Save feedback state for Health Log
    var isSavedConfirmationVisible by remember { mutableStateOf(false) }
    var savedReadingSummary by remember { mutableStateOf<String?>(null) }
    var autoSaveStatus by remember { mutableStateOf<String?>(null) }
    var lastSavedFingerprint by remember { mutableStateOf("") }

    // Compute formatted value string for each mode
    val currentFormattedValue by remember(selectedType, systolicValue, diastolicValue, glucoseValue, symptomTextValue) {
        derivedStateOf {
            when (selectedType) {
                "Blood pressure" -> "$systolicValue/$diastolicValue"
                "Blood sugar" -> String.format(java.util.Locale.US, "%.1f", glucoseValue)
                else -> symptomTextValue.ifBlank { "Unspecified Symptom" }
            }
        }
    }

    val unit = when (selectedType) {
        "Blood pressure" -> "mmHg"
        "Blood sugar" -> "mmol/L"
        else -> ""
    }

    val symptomsSummary by remember(selectedSymptoms) {
        derivedStateOf {
            if (selectedSymptoms.isEmpty()) "" else selectedSymptoms.joinToString(", ")
        }
    }

    // Dynamic Clinical Evaluation
    val evaluation: ClinicalEvaluation? by remember(selectedType, currentFormattedValue, unit, symptomsSummary) {
        derivedStateOf {
            when (selectedType) {
                "Blood pressure" -> {
                    ClinicalEvaluator.evaluateBloodPressure(
                        systolic = systolicValue.toDouble(),
                        diastolic = diastolicValue.toDouble(),
                        symptoms = symptomsSummary,
                        timing = "Routine"
                    )
                }
                "Blood sugar" -> {
                    ClinicalEvaluator.evaluateBloodSugar(
                        value = glucoseValue,
                        unit = unit,
                        symptoms = symptomsSummary,
                        timing = "Routine"
                    )
                }
                else -> {
                    ClinicalEvaluation(
                        classification = symptomTextValue.ifBlank { "Health Symptom" },
                        badgeIcon = "🩺",
                        feedbackMessage = "Analyzing description via medical intelligence to determine cause, practical relief, and warning signs.",
                        severity = HealthSeverity.INFO
                    )
                }
            }
        }
    }

    // Function to send a message to the Groq conversational chatbot
    fun sendMessageToChatbot(promptText: String? = null) {
        val question = (promptText ?: userChatInput).trim()
        if (question.isBlank() || isGroqReplying) return

        val userMessage = ChatBotMessage(isUser = true, text = question)
        chatMessages = chatMessages + userMessage
        userChatInput = ""
        isGroqReplying = true

        coroutineScope.launch {
            val vitalsContext = "$selectedType: $currentFormattedValue $unit" +
                    if (symptomsSummary.isNotBlank()) " (Symptoms: $symptomsSummary)" else ""

            val historyPayload = chatMessages.map { msg ->
                GeminiHealthClient.ChatMessagePayload(
                    role = if (msg.isUser) "user" else "assistant",
                    content = msg.text
                )
            }

            val result = GeminiHealthClient.chatWithGroq(
                history = historyPayload,
                currentVitalsContext = vitalsContext,
                language = language
            )

            isGroqReplying = false
            result.onSuccess { reply ->
                chatMessages = chatMessages + ChatBotMessage(isUser = false, text = reply)
            }.onFailure { err ->
                chatMessages = chatMessages + ChatBotMessage(
                    isUser = false,
                    text = "Could not reach Groq server: ${err.message ?: "Network timeout"}. Please verify connection and try again."
                )
            }
        }
    }

    // Explicit function to save reading directly to Health Log
    fun performSaveReading(navigateImmediately: Boolean = false) {
        val classificationContext = evaluation?.classification ?: "Routine"
        val notes = if (symptomsSummary.isNotBlank()) "Symptoms: $symptomsSummary" else "Health Log"

        onSaveReading(selectedType, currentFormattedValue, unit, classificationContext, notes)
        savedReadingSummary = "$selectedType: $currentFormattedValue $unit"
        isSavedConfirmationVisible = true
        autoSaveStatus = "✓ Saved to Health Log"

        if (navigateImmediately) {
            onReadingSavedNav()
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Professional Clinical Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(HealthGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Clinical Health Monitor",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealSecondary,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Record Vitals & Symptoms",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Groq AI Status Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(TealPrimary.copy(alpha = 0.12f))
                    .border(1.dp, TealPrimary.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                    .clickable { onReadingSavedNav() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("header_history_badge")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "History",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Segmented Vitals Selector Tabs (Professional Medical Cards)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val vitalsTabs = listOf(
                Triple("Blood pressure", "Blood Pressure", Icons.Default.Favorite),
                Triple("Blood sugar", "Blood Glucose", Icons.Default.WaterDrop),
                Triple("Symptom", "Symptom Log", Icons.Default.Psychology)
            )

            vitalsTabs.forEach { (typeKey, title, icon) ->
                val isSelected = selectedType == typeKey
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            selectedType = typeKey
                        }
                        .testTag("vital_tab_$typeKey"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) TealPrimary else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else TealSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Clinical Measurement Entry Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("measurement_entry_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (selectedType) {
                    "Blood pressure" -> {
                        // Blood Pressure Dual Dial (Systolic & Diastolic)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Systolic Control
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "SYSTOLIC",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (systolicValue > 70) systolicValue -= 2 },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(TealPrimary.copy(alpha = 0.1f), CircleShape)
                                            .testTag("bp_systolic_minus")
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease Systolic", tint = TealPrimary, modifier = Modifier.size(18.dp))
                                    }

                                    Text(
                                        text = "$systolicValue",
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    )

                                    IconButton(
                                        onClick = { if (systolicValue < 250) systolicValue += 2 },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(TealPrimary.copy(alpha = 0.1f), CircleShape)
                                            .testTag("bp_systolic_plus")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase Systolic", tint = TealPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Text(
                                    text = "Upper (mmHg)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .height(50.dp)
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            )

                            // Diastolic Control
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "DIASTOLIC",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (diastolicValue > 40) diastolicValue -= 2 },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(TealPrimary.copy(alpha = 0.1f), CircleShape)
                                            .testTag("bp_diastolic_minus")
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease Diastolic", tint = TealPrimary, modifier = Modifier.size(18.dp))
                                    }

                                    Text(
                                        text = "$diastolicValue",
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    )

                                    IconButton(
                                        onClick = { if (diastolicValue < 150) diastolicValue += 2 },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(TealPrimary.copy(alpha = 0.1f), CircleShape)
                                            .testTag("bp_diastolic_plus")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase Diastolic", tint = TealPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Text(
                                    text = "Lower (mmHg)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Fast Preset Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                115 to 75,
                                120 to 80,
                                130 to 85,
                                142 to 92,
                                160 to 100
                            ).forEach { (sys, dia) ->
                                val isMatched = (systolicValue == sys && diastolicValue == dia)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isMatched) TealPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable {
                                            systolicValue = sys
                                            diastolicValue = dia
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$sys/$dia",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMatched) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    "Blood sugar" -> {
                        // Blood Glucose Dial
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "BLOOD GLUCOSE CONCENTRATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        if (glucoseValue > 2.0) {
                                            glucoseValue = Math.round((glucoseValue - 0.2) * 10.0) / 10.0
                                        }
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(TealPrimary.copy(alpha = 0.1f), CircleShape)
                                        .testTag("glucose_minus")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease Glucose", tint = TealPrimary)
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.1f", glucoseValue),
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "mmol/L",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                IconButton(
                                    onClick = {
                                        if (glucoseValue < 30.0) {
                                            glucoseValue = Math.round((glucoseValue + 0.2) * 10.0) / 10.0
                                        }
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(TealPrimary.copy(alpha = 0.1f), CircleShape)
                                        .testTag("glucose_plus")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase Glucose", tint = TealPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Quick Glucose Presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(4.2, 5.4, 6.8, 8.5, 11.5).forEach { preset ->
                                    val isMatch = (glucoseValue == preset)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isMatch) TealPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .clickable { glucoseValue = preset }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$preset",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMatch) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        // Symptom Tracker Entry
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "DESCRIBE YOUR HEALTH SYMPTOM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = symptomTextValue,
                                onValueChange = { symptomTextValue = it },
                                placeholder = {
                                    Text(
                                        text = "e.g. Throbbing left-side migraine, feeling faint when standing, shortness of breath...",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("symptom_text_input"),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Save to Health Log Action Button
                Button(
                    onClick = { performSaveReading(navigateImmediately = false) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_to_health_log_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Reading to Health Log",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Interactive Confirmation Card when saved
                AnimatedVisibility(visible = isSavedConfirmationVisible) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(HealthGreen.copy(alpha = 0.12f))
                            .border(1.dp, HealthGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = HealthGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Successfully Logged to Health Log!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HealthGreen
                                )
                            }
                            IconButton(
                                onClick = { isSavedConfirmationVisible = false },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = HealthGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        if (savedReadingSummary != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Recorded: ${savedReadingSummary!!} · ${evaluation?.classification ?: "Routine"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onReadingSavedNav() },
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "View in Health Log",
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { isSavedConfirmationVisible = false },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Log Another", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Visual Clinical Gauge & Severity Card
        if (evaluation != null && selectedType != "Symptom") {
            val eval = evaluation!!
            val sevColor = when (eval.severity) {
                HealthSeverity.NORMAL -> HealthGreen
                HealthSeverity.ELEVATED -> HealthAmber
                HealthSeverity.WARNING -> Color(0xFFF97316)
                HealthSeverity.CRITICAL -> HealthRed
                HealthSeverity.INFO -> HealthBlue
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("clinical_gauge_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header with category badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = eval.badgeIcon, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = eval.classification,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = sevColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(sevColor.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when (eval.severity) {
                                    HealthSeverity.NORMAL -> "Optimal Range"
                                    HealthSeverity.ELEVATED -> "Caution / Elevated"
                                    HealthSeverity.WARNING -> "Stage 1 / Alert"
                                    HealthSeverity.CRITICAL -> "Action Required"
                                    HealthSeverity.INFO -> "Information"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = sevColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Multi-zone Color Gauge Meter
                    val gaugeProgress = when (selectedType) {
                        "Blood pressure" -> {
                            // Map systolic (90 - 200) to 0.0f - 1.0f
                            ((systolicValue - 90).toFloat() / 110f).coerceIn(0.05f, 0.98f)
                        }
                        "Blood sugar" -> {
                            // Map glucose (3.0 - 15.0) to 0.0f - 1.0f
                            ((glucoseValue - 3.0).toFloat() / 12f).coerceIn(0.05f, 0.98f)
                        }
                        else -> 0.5f
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            HealthBlue,
                                            HealthGreen,
                                            HealthAmber,
                                            Color(0xFFF97316),
                                            HealthRed
                                        )
                                    )
                                )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Low", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Normal", fontSize = 10.sp, color = HealthGreen, fontWeight = FontWeight.Bold)
                            Text("Elevated", fontSize = 10.sp, color = HealthAmber)
                            Text("High", fontSize = 10.sp, color = HealthRed, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = eval.feedbackMessage,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )

                    // Urgent Red Flag Warning
                    if (eval.redFlagWarning != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(HealthRed.copy(alpha = 0.12f))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = HealthRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = eval.redFlagWarning!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HealthRed,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Accompanying Symptoms Chips
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Accompanying Symptoms (Select all that apply)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                val symptomsList = listOf(
                    "Severe headache",
                    "Blurry vision",
                    "Dizziness / Shakiness",
                    "Chest tightness",
                    "Fatigue",
                    "Nausea",
                    "Palpitations",
                    "Sweating"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    symptomsList.take(4).forEach { sym ->
                        val isChecked = selectedSymptoms.contains(sym)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isChecked) TealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(1.dp, if (isChecked) TealPrimary else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedSymptoms = if (isChecked) selectedSymptoms - sym else selectedSymptoms + sym
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sym,
                                fontSize = 10.sp,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                color = if (isChecked) TealPrimary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    symptomsList.drop(4).forEach { sym ->
                        val isChecked = selectedSymptoms.contains(sym)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isChecked) TealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(1.dp, if (isChecked) TealPrimary else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedSymptoms = if (isChecked) selectedSymptoms - sym else selectedSymptoms + sym
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sym,
                                fontSize = 10.sp,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                color = if (isChecked) TealPrimary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =========================================================================
        // GROQ AI MEDICAL INTELLIGENCE CHATBOT (Powered by Groq Llama-3.3-70B API)
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("groq_ai_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header: Groq AI Assistant with Clear button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(TealPrimary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Groq Clinical AI Assistant",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Llama 3.3 70B • Conversational Medical & Health AI",
                                fontSize = 11.sp,
                                color = TealSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Reset / Clear Chat Button
                    IconButton(
                        onClick = {
                            chatMessages = listOf(
                                ChatBotMessage(
                                    isUser = false,
                                    text = "Hello! I'm your Groq Clinical AI Assistant (powered by Llama 3.3 70B). Ask me any questions about health, nutrition, sleep, medications, fitness, or your readings on screen!"
                                )
                            )
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear Chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Question / Topic Chips (Scrollable row)
                Text(
                    text = "Suggested Inquiries (General & Vitals):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                val quickInquiries = listOf(
                    "🥗 Healthy heart foods",
                    "💧 Daily hydration target",
                    "😴 Tips for deeper sleep",
                    "🧘 Lowering stress naturally",
                    "❤️ Safe blood pressure range",
                    "🩸 How to avoid sugar spikes",
                    "🏃 Safe exercise routine",
                    "💊 What if I missed my medication?"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickInquiries.forEach { promptText ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(0.5.dp, TealPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable {
                                    sendMessageToChatbot(promptText)
                                }
                                .padding(vertical = 5.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = promptText,
                                fontSize = 10.sp,
                                color = TealPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Chat Messages Transcript Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp, max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chatMessages.forEach { message ->
                            if (message.isUser) {
                                // User Bubble (Right Aligned)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 14.dp,
                                                    topEnd = 14.dp,
                                                    bottomStart = 14.dp,
                                                    bottomEnd = 2.dp
                                                )
                                            )
                                            .background(TealPrimary)
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = message.text,
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            lineHeight = 17.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = message.timestamp,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            } else {
                                // AI Assistant Bubble (Left Aligned)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 2.dp,
                                                    topEnd = 14.dp,
                                                    bottomStart = 14.dp,
                                                    bottomEnd = 14.dp
                                                )
                                            )
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, TealPrimary.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                                            .padding(horizontal = 12.dp, vertical = 9.dp)
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Psychology,
                                                    contentDescription = null,
                                                    tint = TealPrimary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Groq AI Medical Assistant",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TealPrimary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = message.text,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = message.timestamp,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        // Groq Replying Indicator
                        if (isGroqReplying) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, TealPrimary.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    color = TealPrimary,
                                    strokeWidth = 1.5.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Groq Llama 3.3 is typing medical guidance...",
                                    fontSize = 11.sp,
                                    color = TealSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Chat Input Field & Send Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = userChatInput,
                        onValueChange = { userChatInput = it },
                        placeholder = {
                            Text(
                                text = "Ask about health, vitals, nutrition, sleep...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            focusManager.clearFocus()
                            sendMessageToChatbot()
                        }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ask_groq_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            sendMessageToChatbot()
                        },
                        enabled = userChatInput.isNotBlank() && !isGroqReplying,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (userChatInput.isNotBlank() && !isGroqReplying) TealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .testTag("ask_groq_btn")
                    ) {
                        if (isGroqReplying) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Message",
                                tint = if (userChatInput.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Share & Clinical Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Share with Doctor / Caregiver
            OutlinedButton(
                onClick = {
                    val shareText = "Clinical Health Record:\n" +
                            "• Type: $selectedType\n" +
                            "• Reading: $currentFormattedValue $unit\n" +
                            "• Symptoms: ${if (symptomsSummary.isNotBlank()) symptomsSummary else "None"}\n" +
                            "• Status: ${evaluation?.classification ?: "Recorded"}\n" +
                            "• Logged via HealthBridge"
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Clinical Reading"))
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp), tint = TealSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Vitals", fontSize = 12.sp, color = TealSecondary, fontWeight = FontWeight.SemiBold)
                }
            }

            // Correlate with Medications
            OutlinedButton(
                onClick = onNavigateToMedications,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(15.dp), tint = TealPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Medications", fontSize = 12.sp, color = TealPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Navigation Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateToDashboard,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dashboard", fontSize = 13.sp)
                }
            }

            Button(
                onClick = { performSaveReading(navigateImmediately = true) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & View Log", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
