package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent
import java.util.Locale
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.ClinicEntity
import com.example.data.DoseLogEntity
import com.example.data.GeminiHealthClient
import com.example.ui.adapters.ClinicXmlAdapter
import com.example.ui.adapters.HealthLogXmlAdapter
import com.example.ui.adapters.MedicationXmlAdapter
import com.example.util.ClinicalEvaluator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class XmlUiController(
    private val activity: Activity,
    private val rootContainer: ViewGroup,
    private val viewModel: MainViewModel,
    private val onToggleEngine: () -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var currentScreenLayout: View? = null
    private var activeNavIndex = 0 // -1: Auth, 0: Dashboard, 1: Log Reading, 2: Medications, 3: History, 4: Clinics, 5: Settings

    private var fragmentContainer: FrameLayout? = null
    private var bottomNavBar: LinearLayout? = null
    private var tvSyncBadge: TextView? = null

    // Adapters
    private val medicationAdapter = MedicationXmlAdapter(
        onMarkTaken = { doseId ->
            viewModel.markDoseTaken(doseId)
            Toast.makeText(activity, "Marked dose taken!", Toast.LENGTH_SHORT).show()
        }
    )

    private val healthLogAdapter = HealthLogXmlAdapter(
        onItemClick = { log ->
            Toast.makeText(activity, "${log.type}: ${log.value} (${log.notes})", Toast.LENGTH_SHORT).show()
        }
    )

    private val clinicAdapter = ClinicXmlAdapter(
        onCallClick = { clinic ->
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${clinic.phone.ifBlank { "911" }}"))
            activity.startActivity(intent)
        },
        onDirectionsClick = { clinic ->
            val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(clinic.name + " " + clinic.address)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            activity.startActivity(mapIntent)
        }
    )

    // Log Reading State
    private var activeTab = "PRESSURE" // "PRESSURE", "GLUCOSE", "SYMPTOM"
    private var systolicValue = 120
    private var diastolicValue = 80
    private var glucoseValue = 5.4
    private val selectedSymptoms = mutableSetOf<String>()

    fun start() {
        val activityMainView = LayoutInflater.from(activity).inflate(R.layout.activity_main, rootContainer, false)
        rootContainer.removeAllViews()
        rootContainer.addView(activityMainView)

        fragmentContainer = activityMainView.findViewById(R.id.fragment_container)
        bottomNavBar = activityMainView.findViewById(R.id.bottom_nav_bar)
        tvSyncBadge = activityMainView.findViewById(R.id.tv_sync_status_badge)

        setupBottomNavButtons(activityMainView)

        scope.launch {
            viewModel.userSession.collectLatest { session ->
                if (session != null && session.isLoggedIn && activeNavIndex == -1) {
                    showScreen(0) // Show Dashboard once logged in
                }
                if (session != null) {
                    applyThemeToXmlRoot(session.darkThemeEnabled)
                }
            }
        }

        scope.launch {
            kotlinx.coroutines.flow.combine(
                viewModel.isNetworkConnected,
                viewModel.pendingSyncCount
            ) { isConnected, pendingCount ->
                Pair(isConnected, pendingCount)
            }.collectLatest { (isConnected, pendingCount) ->
                if (!isConnected) {
                    tvSyncBadge?.text = "🟠 Offline (Saved)"
                    tvSyncBadge?.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
                    tvSyncBadge?.setBackgroundResource(R.drawable.bg_badge_amber)
                } else if (pendingCount > 0) {
                    tvSyncBadge?.text = "🔄 Syncing $pendingCount item(s)..."
                    tvSyncBadge?.setTextColor(android.graphics.Color.parseColor("#3B82F6"))
                    tvSyncBadge?.setBackgroundResource(R.drawable.bg_badge_teal)
                } else {
                    tvSyncBadge?.text = "🟢 Auto-Synced"
                    tvSyncBadge?.setTextColor(android.graphics.Color.parseColor("#10B981"))
                    tvSyncBadge?.setBackgroundResource(R.drawable.bg_badge_green)
                }
            }
        }

        showScreen(0)
    }

    private fun setupBottomNavButtons(mainView: View) {
        val navIds = listOf(
            R.id.nav_dash to 0,
            R.id.nav_log to 1,
            R.id.nav_meds to 2,
            R.id.nav_history to 3,
            R.id.nav_clinics to 4,
            R.id.nav_settings to 5
        )

        for ((id, index) in navIds) {
            mainView.findViewById<TextView>(id)?.setOnClickListener {
                showScreen(index)
            }
        }
    }

    private fun updateBottomNavHighlight(mainView: View) {
        val navIds = listOf(
            R.id.nav_dash to 0,
            R.id.nav_log to 1,
            R.id.nav_meds to 2,
            R.id.nav_history to 3,
            R.id.nav_clinics to 4,
            R.id.nav_settings to 5
        )

        for ((id, index) in navIds) {
            val tv = mainView.findViewById<TextView>(id)
            if (index == activeNavIndex) {
                tv?.setTextColor(android.graphics.Color.parseColor("#1D9E75"))
            } else {
                tv?.setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
            }
        }
    }

    fun showScreen(index: Int) {
        activeNavIndex = index
        val container = fragmentContainer ?: rootContainer
        container.removeAllViews()

        if (index == -1) {
            bottomNavBar?.visibility = View.GONE
        } else {
            bottomNavBar?.visibility = View.VISIBLE
            rootContainer.let { updateBottomNavHighlight(it) }
        }

        val layoutId = when (index) {
            -1 -> R.layout.fragment_auth
            0 -> R.layout.fragment_dashboard
            1 -> R.layout.fragment_log_reading
            2 -> R.layout.fragment_medications
            3 -> R.layout.fragment_health_history
            4 -> R.layout.fragment_clinic_locator
            5 -> R.layout.fragment_settings
            else -> R.layout.fragment_dashboard
        }

        val inflater = LayoutInflater.from(activity)
        val view = inflater.inflate(layoutId, container, false)
        currentScreenLayout = view
        container.addView(view)

        when (index) {
            -1 -> setupAuthView(view)
            0 -> setupDashboardView(view)
            1 -> setupLogReadingView(view)
            2 -> setupMedicationsView(view)
            3 -> setupHealthLogView(view)
            4 -> setupClinicsView(view)
            5 -> setupSettingsView(view)
        }

        val isDark = viewModel.userSession.value?.darkThemeEnabled ?: true
        applyThemeToXmlRoot(isDark)
    }

    // --- 1. Authentication View Setup ---
    fun setupAuthView(view: View) {
        val spinnerLang = view.findViewById<android.widget.Spinner>(R.id.spinner_auth_language)
        val tabSignIn = view.findViewById<TextView>(R.id.tab_auth_signin)
        val tabRegister = view.findViewById<TextView>(R.id.tab_auth_register)
        val layoutName = view.findViewById<LinearLayout>(R.id.layout_auth_name)
        val etName = view.findViewById<EditText>(R.id.et_auth_name)
        val etEmail = view.findViewById<EditText>(R.id.et_auth_email)
        val etPin = view.findViewById<EditText>(R.id.et_auth_pin)
        val btnForgotPin = view.findViewById<TextView>(R.id.btn_forgot_pin)
        val btnPrimary = view.findViewById<Button>(R.id.btn_auth_primary)
        val btnGoogle = view.findViewById<View>(R.id.btn_auth_google)
        val tvGoogleLabel = view.findViewById<TextView>(R.id.tv_auth_google_label)
        var isRegister = false

        // Setup Language Spinner
        val languages = listOf("English", "isiZulu", "Sesotho", "Afrikaans")
        val langAdapter = android.widget.ArrayAdapter(activity, android.R.layout.simple_spinner_item, languages)
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLang?.adapter = langAdapter

        // Populate existing session if available
        val currentUser = viewModel.userSession.value
        if (currentUser != null && currentUser.isLoggedIn && currentUser.email.isNotBlank()) {
            etEmail?.setText(currentUser.email)
            if (currentUser.fullName.isNotBlank()) {
                etName?.setText(currentUser.fullName)
            }
            val langIdx = languages.indexOf(currentUser.preferredLanguage)
            if (langIdx >= 0) {
                spinnerLang?.setSelection(langIdx)
            }
        }

        // Toggle between Sign In & Register
        fun updateTabs() {
            if (isRegister) {
                tabRegister?.setBackgroundResource(R.drawable.bg_tab_selected)
                tabRegister?.setTextColor(android.graphics.Color.WHITE)
                tabSignIn?.setBackgroundResource(0)
                tabSignIn?.setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
                layoutName?.visibility = View.VISIBLE
                btnPrimary?.text = "Create DoseMate Account"
                tvGoogleLabel?.text = "Sign up with Google"
            } else {
                tabSignIn?.setBackgroundResource(R.drawable.bg_tab_selected)
                tabSignIn?.setTextColor(android.graphics.Color.WHITE)
                tabRegister?.setBackgroundResource(0)
                tabRegister?.setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
                layoutName?.visibility = View.GONE
                btnPrimary?.text = "Sign In to DoseMate"
                tvGoogleLabel?.text = "Sign in with Google"
            }
        }

        tabSignIn?.setOnClickListener {
            isRegister = false
            updateTabs()
        }

        tabRegister?.setOnClickListener {
            isRegister = true
            updateTabs()
        }

        // Google Sign-In Chooser Dialog
        fun showGoogleAccountChooser() {
            val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_google_signin, null)
            val dialog = AlertDialog.Builder(activity)
                .setView(dialogView)
                .create()

            val accountNeo = dialogView.findViewById<View>(R.id.layout_google_account_neo)
            val accountCustom = dialogView.findViewById<View>(R.id.layout_google_account_custom)

            accountNeo?.setOnClickListener {
                dialog.dismiss()
                val selectedLang = spinnerLang?.selectedItem?.toString() ?: "English"
                viewModel.login("neomadima47@gmail.com", "Neo Madima", selectedLang)
                Toast.makeText(activity, "Signed in with Google as Neo Madima", Toast.LENGTH_SHORT).show()
                showScreen(0)
            }

            accountCustom?.setOnClickListener {
                dialog.dismiss()
                val customInput = EditText(activity).apply {
                    hint = "username@gmail.com"
                    inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    setTextColor(android.graphics.Color.WHITE)
                    setHintTextColor(android.graphics.Color.parseColor("#9CA3AF"))
                    setPadding(48, 36, 48, 36)
                }
                AlertDialog.Builder(activity)
                    .setTitle("Google Sign-In")
                    .setMessage("Enter your Google Account email address:")
                    .setView(customInput)
                    .setPositiveButton("Sign In") { _, _ ->
                        val email = customInput.text.toString().trim()
                        if (email.contains("@") && email.contains(".")) {
                            val derivedName = email.substringBefore('@')
                                .replace('.', ' ')
                                .replace('_', ' ')
                                .split(' ')
                                .joinToString(" ") { p ->
                                    p.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }
                                }
                            val selectedLang = spinnerLang?.selectedItem?.toString() ?: "English"
                            viewModel.login(email, derivedName.ifBlank { "Google User" }, selectedLang)
                            Toast.makeText(activity, "Signed in with Google as $derivedName", Toast.LENGTH_SHORT).show()
                            showScreen(0)
                        } else {
                            Toast.makeText(activity, "Please enter a valid Google email", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            dialog.show()
        }

        btnGoogle?.setOnClickListener {
            com.example.data.FirebaseAuthManager.signInWithGoogleFirebase(activity) { success, email, name ->
                if (success && !email.isNullOrBlank()) {
                    val selectedLang = spinnerLang?.selectedItem?.toString() ?: "English"
                    viewModel.login(email, name ?: "Google User", selectedLang)
                    Toast.makeText(activity, "Signed in with Google as ${name ?: email}", Toast.LENGTH_SHORT).show()
                    showScreen(0)
                } else {
                    showGoogleAccountChooser()
                }
            }
        }

        // Forgot PIN / Password Dialog Trigger
        btnForgotPin?.setOnClickListener {
            val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_forgot_password, null)
            val dialog = AlertDialog.Builder(activity)
                .setView(dialogView)
                .create()

            val etResetEmail = dialogView.findViewById<EditText>(R.id.et_reset_email)
            val etResetNewPin = dialogView.findViewById<EditText>(R.id.et_reset_new_pin)
            val etResetConfirmPin = dialogView.findViewById<EditText>(R.id.et_reset_confirm_pin)
            val btnResetCancel = dialogView.findViewById<Button>(R.id.btn_reset_cancel)
            val btnResetSave = dialogView.findViewById<Button>(R.id.btn_reset_save)
            val btnResetViaGoogle = dialogView.findViewById<View>(R.id.btn_reset_via_google)

            val prefillEmail = etEmail?.text?.toString()?.trim().orEmpty().ifBlank {
                currentUser?.email.orEmpty()
            }
            if (prefillEmail.isNotBlank()) {
                etResetEmail?.setText(prefillEmail)
            }

            btnResetCancel?.setOnClickListener { dialog.dismiss() }
            btnResetViaGoogle?.setOnClickListener {
                dialog.dismiss()
                showGoogleAccountChooser()
            }

            btnResetSave?.setOnClickListener {
                val email = etResetEmail?.text?.toString()?.trim() ?: ""

                if (email.isBlank() || !email.contains("@")) {
                    Toast.makeText(activity, "Please enter your registered email address", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                com.example.data.FirebaseAuthManager.sendFirebasePasswordReset(
                    email = email,
                    onSuccess = {
                        dialog.dismiss()
                        Toast.makeText(
                            activity,
                            "Password reset email sent to $email! Check your inbox to set a new password.",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onError = { error ->
                        Toast.makeText(activity, "Error: $error", Toast.LENGTH_LONG).show()
                    }
                )
            }

            dialog.show()
        }

        // Professional Sign In & Registration validation
        btnPrimary?.setOnClickListener {
            val email = etEmail?.text?.toString()?.trim() ?: ""
            val password = etPin?.text?.toString()?.trim() ?: ""
            val selectedLang = spinnerLang?.selectedItem?.toString() ?: "English"

            if (email.isBlank() || !email.contains("@") || !email.contains(".")) {
                Toast.makeText(activity, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                etEmail?.requestFocus()
                return@setOnClickListener
            }
            if (password.isBlank()) {
                Toast.makeText(activity, "Please enter your password", Toast.LENGTH_SHORT).show()
                etPin?.requestFocus()
                return@setOnClickListener
            }

            if (isRegister) {
                val inputName = etName?.text?.toString()?.trim() ?: ""
                if (inputName.isBlank()) {
                    Toast.makeText(activity, "Please enter your full name", Toast.LENGTH_SHORT).show()
                    etName?.requestFocus()
                    return@setOnClickListener
                }
                viewModel.register(email, inputName, selectedLang, password)
                Toast.makeText(activity, "Account created! Welcome, $inputName!", Toast.LENGTH_SHORT).show()
                showScreen(0)
            } else {
                val session = viewModel.userSession.value
                val derivedName = if (session != null && session.email.equals(email, ignoreCase = true) && session.fullName.isNotBlank()) {
                    session.fullName
                } else {
                    email.substringBefore('@').replace('.', ' ').replace('_', ' ')
                        .split(' ')
                        .joinToString(" ") { p ->
                            p.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }
                        }
                }

                viewModel.login(email, derivedName, selectedLang, password)
                Toast.makeText(activity, "Welcome back, $derivedName!", Toast.LENGTH_SHORT).show()
                showScreen(0)
            }
        }

        updateTabs()
    }

    // --- 2. Dashboard View Setup ---
    fun setupDashboardView(view: View) {
        val tvGreeting = view.findViewById<TextView>(R.id.tv_greeting)
        val tvSummary = view.findViewById<TextView>(R.id.tv_adherence_summary)
        val pbAdherence = view.findViewById<ProgressBar>(R.id.pb_adherence)
        val btnMarkAllTaken = view.findViewById<Button>(R.id.btn_mark_all_taken)
        val btnLogVital = view.findViewById<Button>(R.id.btn_log_vital)
        val btnAddMed = view.findViewById<Button>(R.id.btn_add_medication)
        val btnFindClinic = view.findViewById<Button>(R.id.btn_find_clinic)
        val btnHeaderNotifications = view.findViewById<TextView>(R.id.btn_header_notifications)
        val rvTodayMeds = view.findViewById<RecyclerView>(R.id.rv_today_medications)
        val tvMedsCount = view.findViewById<TextView>(R.id.tv_meds_count_badge)

        // User greeting
        val user = viewModel.userSession.value
        tvGreeting?.text = user?.fullName ?: "Lerato Mokoena"

        // Real-time calculation of adherence percentage
        val updateAdherence = {
            val doses = viewModel.doseLogs.value
            val takenCount = doses.count { it.status == "Taken" }
            val totalCount = doses.size
            val pct = if (totalCount > 0) (takenCount * 100) / totalCount else 100

            tvSummary?.text = if (totalCount > 0) {
                "Daily Adherence: $pct% ($takenCount of $totalCount doses taken)"
            } else {
                "All scheduled medications up to date"
            }
            pbAdherence?.progress = pct
        }

        updateAdherence()

        // Quick action listeners
        btnMarkAllTaken?.setOnClickListener {
            viewModel.markAllPendingDosesTaken()
            Toast.makeText(activity, "All pending doses marked taken!", Toast.LENGTH_SHORT).show()
        }

        btnLogVital?.setOnClickListener { showScreen(1) }
        btnAddMed?.setOnClickListener { showAddMedicationDialog() }
        btnFindClinic?.setOnClickListener { showScreen(4) }
        btnHeaderNotifications?.setOnClickListener { showScreen(5) }
        tvMedsCount?.setOnClickListener { showScreen(2) }

        // Today's Medications RecyclerView
        rvTodayMeds?.layoutManager = LinearLayoutManager(activity)
        rvTodayMeds?.adapter = medicationAdapter

        scope.launch {
            viewModel.doseLogs.collectLatest { list ->
                medicationAdapter.updateData(list)
                updateAdherence()
            }
        }
    }

    // --- 3. AI Medication & Health Assistant View Setup ---
    fun setupLogReadingView(view: View) {
        val etGroqInput = view.findViewById<EditText>(R.id.et_groq_chat_input)
        val btnSendGroq = view.findViewById<TextView>(R.id.btn_send_groq_chat)
        val layoutGroqReplying = view.findViewById<LinearLayout>(R.id.layout_groq_replying)
        val layoutChatContainer = view.findViewById<LinearLayout>(R.id.layout_chat_messages_container)
        val btnClearChat = view.findViewById<TextView>(R.id.btn_clear_chat)

        val chatHistory = mutableListOf<GeminiHealthClient.ChatMessagePayload>()

        fun addChatMessage(text: String, isUser: Boolean) {
            val msgView = TextView(activity).apply {
                this.text = text
                textSize = 12f
                setTextColor(if (isUser) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#F3F4F6"))
                setBackgroundResource(if (isUser) R.drawable.bg_chat_user else R.drawable.bg_chat_ai)
                setPadding(16, 12, 16, 12)
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 10
                gravity = if (isUser) android.view.Gravity.END else android.view.Gravity.START
            }
            msgView.layoutParams = params
            layoutChatContainer?.addView(msgView)
        }

        // Initial welcoming greeting from Google Gemini AI Assistant
        if (layoutChatContainer?.childCount == 0) {
            val initialGreeting = "Hello! I am your AI Medication & Health Assistant powered by Google Gemini AI. Ask me anything about your prescriptions, side effects, drug interactions, or wellness advice!"
            addChatMessage(initialGreeting, isUser = false)
            chatHistory.add(GeminiHealthClient.ChatMessagePayload(role = "assistant", content = initialGreeting))
        }

        fun sendUserMessage(query: String) {
            val q = query.trim()
            if (q.isBlank()) return

            addChatMessage(q, isUser = true)
            chatHistory.add(GeminiHealthClient.ChatMessagePayload(role = "user", content = q))
            etGroqInput?.setText("")
            layoutGroqReplying?.visibility = View.VISIBLE

            val activeMeds = viewModel.medications.value.map { "${it.name} (${it.dosage})" }.joinToString(", ")
            val currentVitalsContext = "Active Prescriptions on file: ${activeMeds.ifBlank { "Metformin 500mg, Amlodipine 5mg" }}"

            scope.launch {
                val result = GeminiHealthClient.chatWithGroq(
                    history = chatHistory.toList(),
                    currentVitalsContext = currentVitalsContext,
                    language = "English"
                )
                withContext(Dispatchers.Main) {
                    layoutGroqReplying?.visibility = View.GONE
                    val aiResponse = result.getOrElse { "I am available to answer any questions about your medications, side effects, or dosage." }
                    addChatMessage(aiResponse, isUser = false)
                    chatHistory.add(GeminiHealthClient.ChatMessagePayload(role = "assistant", content = aiResponse))
                }
            }
        }

        btnSendGroq?.setOnClickListener {
            val query = etGroqInput?.text?.toString() ?: ""
            sendUserMessage(query)
        }

        btnClearChat?.setOnClickListener {
            layoutChatContainer?.removeAllViews()
            chatHistory.clear()
            val resetGreeting = "Chat history cleared. Ask me any questions about your medications or health!"
            addChatMessage(resetGreeting, isUser = false)
            chatHistory.add(GeminiHealthClient.ChatMessagePayload(role = "assistant", content = resetGreeting))
        }

        // Quick Suggestion Inquiry Chips
        val chipSideEffects = view.findViewById<TextView>(R.id.chip_inquiry_side_effects)
        val chipInteractions = view.findViewById<TextView>(R.id.chip_inquiry_interactions)
        val chipMissedDose = view.findViewById<TextView>(R.id.chip_inquiry_missed_dose)
        val chipFoods = view.findViewById<TextView>(R.id.chip_inquiry_foods)
        val chipSleep = view.findViewById<TextView>(R.id.chip_inquiry_sleep)

        chipSideEffects?.setOnClickListener { sendUserMessage("What are common side effects of Metformin?") }
        chipInteractions?.setOnClickListener { sendUserMessage("Is it safe to take Ibuprofen with Lisinopril?") }
        chipMissedDose?.setOnClickListener { sendUserMessage("What should I do if I forget to take a dose of my prescription?") }
        chipFoods?.setOnClickListener { sendUserMessage("What foods are best for managing high blood pressure?") }
        chipSleep?.setOnClickListener { sendUserMessage("Give me sleep tips to manage stress and heart health.") }
    }

    // --- 4. Medications View Setup ---
    fun setupMedicationsView(view: View) {
        val rvList = view.findViewById<RecyclerView>(R.id.rv_medications_list)
        val layoutEmpty = view.findViewById<LinearLayout>(R.id.layout_empty_medications)
        val btnHeaderAdd = view.findViewById<Button>(R.id.btn_header_add_med)
        val btnTestAlert = view.findViewById<Button>(R.id.btn_test_reminder_alert)
        val tvScheduleCount = view.findViewById<TextView>(R.id.tv_med_schedule_count)

        // Filter Tabs
        val tabAll = view.findViewById<TextView>(R.id.tab_med_filter_all)
        val tabDue = view.findViewById<TextView>(R.id.tab_med_filter_due)
        val tabCompleted = view.findViewById<TextView>(R.id.tab_med_filter_completed)

        var currentFilter = "All"

        fun applyFilter(filter: String, doseList: List<DoseLogEntity> = viewModel.doseLogs.value) {
            currentFilter = filter

            val activeMeds = viewModel.medications.value.filter { it.status == "Active" }

            val displayDoses = mutableListOf<DoseLogEntity>()

            for (med in activeMeds) {
                val times = med.reminderTimes.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("08:00 AM") }
                val displayName = if (med.dosage.isNotBlank() && med.dosage != "1 dose" && med.dosage != "Standard dose") "${med.name} (${med.dosage})" else med.name

                for (timeStr in times) {
                    val existingLog = doseList.find { log ->
                        (log.medicationId == med.id || log.medicationName.contains(med.name, ignoreCase = true)) &&
                                (log.scheduledTime.equals(timeStr, ignoreCase = true) || times.size == 1)
                    }

                    if (existingLog != null) {
                        displayDoses.add(existingLog)
                    } else {
                        displayDoses.add(
                            DoseLogEntity(
                                medicationId = med.id,
                                medicationName = displayName,
                                scheduledTime = timeStr,
                                status = "Pending",
                                date = "Today"
                            )
                        )
                    }
                }
            }

            val finalDosesList = (if (displayDoses.isNotEmpty()) displayDoses else doseList)
                .distinctBy { "${it.medicationId}_${it.scheduledTime}" }

            val filtered = when (filter) {
                "Due" -> finalDosesList.filter { it.status != "Taken" }
                "Completed" -> finalDosesList.filter { it.status == "Taken" }
                else -> finalDosesList
            }

            medicationAdapter.updateData(filtered)
            layoutEmpty?.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            rvList?.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE

            val tabs = listOfNotNull(tabAll, tabDue, tabCompleted)
            tabs.forEach { tab ->
                val isSelected = (tab == tabAll && filter == "All") ||
                                 (tab == tabDue && filter == "Due") ||
                                 (tab == tabCompleted && filter == "Completed")
                tab.setBackgroundResource(if (isSelected) R.drawable.bg_button_teal else 0)
                tab.setTextColor(android.graphics.Color.parseColor(if (isSelected) "#FFFFFF" else "#9CA3AF"))
            }

            tvScheduleCount?.text = "${filtered.size} prescriptions & doses shown"
        }

        rvList?.layoutManager = LinearLayoutManager(activity)
        rvList?.adapter = medicationAdapter

        tabAll?.setOnClickListener { applyFilter("All") }
        tabDue?.setOnClickListener { applyFilter("Due") }
        tabCompleted?.setOnClickListener { applyFilter("Completed") }

        btnHeaderAdd?.setOnClickListener { showAddMedicationDialog() }

        btnTestAlert?.setOnClickListener {
            com.example.notifications.MedicationNotificationHelper.showMedicationReminder(
                context = activity,
                medicationId = System.currentTimeMillis() % 10000,
                medicationName = "Metformin 500mg",
                dosage = "500mg with meal",
                scheduledTime = "Now"
            )
            Toast.makeText(activity, "Dispatched dose reminder alert to system tray!", Toast.LENGTH_SHORT).show()
        }

        scope.launch {
            kotlinx.coroutines.flow.combine(
                viewModel.doseLogs,
                viewModel.medications
            ) { doses, _ ->
                applyFilter(currentFilter, doses)
            }.collectLatest {}
        }

        applyFilter("All")
    }

    fun showAddMedicationDialog(prefilledName: String = "") {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_add_medication, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_dialog_med_name)
        val tvStartDateVal = dialogView.findViewById<TextView>(R.id.tv_dialog_start_date_val)
        val btnPickStartDate = dialogView.findViewById<Button>(R.id.btn_dialog_pick_start_date)
        val spinnerFreq = dialogView.findViewById<android.widget.Spinner>(R.id.spinner_dialog_frequency)

        val row1 = dialogView.findViewById<View>(R.id.layout_time_row_1)
        val row2 = dialogView.findViewById<View>(R.id.layout_time_row_2)
        val row3 = dialogView.findViewById<View>(R.id.layout_time_row_3)
        val row4 = dialogView.findViewById<View>(R.id.layout_time_row_4)

        val tvTime1 = dialogView.findViewById<TextView>(R.id.tv_time_val_1)
        val tvTime2 = dialogView.findViewById<TextView>(R.id.tv_time_val_2)
        val tvTime3 = dialogView.findViewById<TextView>(R.id.tv_time_val_3)
        val tvTime4 = dialogView.findViewById<TextView>(R.id.tv_time_val_4)

        val btnPick1 = dialogView.findViewById<Button>(R.id.btn_pick_time_1)
        val btnPick2 = dialogView.findViewById<Button>(R.id.btn_pick_time_2)
        val btnPick3 = dialogView.findViewById<Button>(R.id.btn_pick_time_3)
        val btnPick4 = dialogView.findViewById<Button>(R.id.btn_pick_time_4)

        if (prefilledName.isNotBlank()) {
            etName?.setText(prefilledName)
        }

        val selectedStartCal = java.util.Calendar.getInstance()
        val startDateDisplayFormat = java.text.SimpleDateFormat("EEEE, MMM d, yyyy", java.util.Locale.US)
        val startDateIsoFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)

        tvStartDateVal?.text = "Today (${startDateDisplayFormat.format(selectedStartCal.time)})"

        btnPickStartDate?.setOnClickListener {
            val yr = selectedStartCal.get(java.util.Calendar.YEAR)
            val mo = selectedStartCal.get(java.util.Calendar.MONTH)
            val dy = selectedStartCal.get(java.util.Calendar.DAY_OF_MONTH)

            android.app.DatePickerDialog(activity, { _, selectedYear, selectedMonth, selectedDay ->
                selectedStartCal.set(java.util.Calendar.YEAR, selectedYear)
                selectedStartCal.set(java.util.Calendar.MONTH, selectedMonth)
                selectedStartCal.set(java.util.Calendar.DAY_OF_MONTH, selectedDay)

                val formattedDate = startDateDisplayFormat.format(selectedStartCal.time)
                tvStartDateVal?.text = formattedDate
            }, yr, mo, dy).apply {
                datePicker.minDate = System.currentTimeMillis() - 1000
            }.show()
        }

        // Frequency options
        val freqOptions = arrayOf("1x Daily (Once a day)", "2x Daily (Twice a day)", "3x Daily (Thrice a day)", "4x Daily (4 times a day)")
        val freqAdapter = android.widget.ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, freqOptions)
        spinnerFreq?.adapter = freqAdapter

        fun updateRowVisibilities(count: Int) {
            row1?.visibility = if (count >= 1) View.VISIBLE else View.GONE
            row2?.visibility = if (count >= 2) View.VISIBLE else View.GONE
            row3?.visibility = if (count >= 3) View.VISIBLE else View.GONE
            row4?.visibility = if (count >= 4) View.VISIBLE else View.GONE
        }

        spinnerFreq?.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateRowVisibilities(position + 1)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        fun showTimePickerDialog(targetTv: TextView?) {
            val cal = java.util.Calendar.getInstance()
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = cal.get(java.util.Calendar.MINUTE)

            android.app.TimePickerDialog(activity, { _, selectedHour, selectedMinute ->
                val formatCal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, selectedHour)
                    set(java.util.Calendar.MINUTE, selectedMinute)
                }
                val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
                targetTv?.text = sdf.format(formatCal.time)
            }, hour, minute, false).show()
        }

        btnPick1?.setOnClickListener { showTimePickerDialog(tvTime1) }
        btnPick2?.setOnClickListener { showTimePickerDialog(tvTime2) }
        btnPick3?.setOnClickListener { showTimePickerDialog(tvTime3) }
        btnPick4?.setOnClickListener { showTimePickerDialog(tvTime4) }

        AlertDialog.Builder(activity)
            .setView(dialogView)
            .setPositiveButton("Save Prescription") { _, _ ->
                val name = etName?.text.toString().trim()
                if (name.isBlank()) {
                    Toast.makeText(activity, "Please enter a medication name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val selectedFreqPos = spinnerFreq?.selectedItemPosition ?: 0
                val doseCount = selectedFreqPos + 1
                val freqText = when (doseCount) {
                    1 -> "Once daily"
                    2 -> "Twice daily"
                    3 -> "3x daily"
                    else -> "4x daily"
                }

                val timesList = mutableListOf<String>()
                if (doseCount >= 1) timesList.add(tvTime1?.text?.toString() ?: "08:00 AM")
                if (doseCount >= 2) timesList.add(tvTime2?.text?.toString() ?: "02:00 PM")
                if (doseCount >= 3) timesList.add(tvTime3?.text?.toString() ?: "08:00 PM")
                if (doseCount >= 4) timesList.add(tvTime4?.text?.toString() ?: "10:00 PM")

                val timesFormatted = timesList.joinToString(", ")
                val defaultDosage = "1 dose"
                val startDateIso = startDateIsoFormat.format(selectedStartCal.time)

                viewModel.addMedication(name, defaultDosage, freqText, timesFormatted, startDateIso)
                Toast.makeText(activity, "Scheduled $name ($freqText at $timesFormatted starting $startDateIso)", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- 5. Health Log View Setup ---
    fun setupHealthLogView(view: View) {
        val tvSubtitle = view.findViewById<TextView>(R.id.tv_history_subtitle)
        val tvAdherenceBadge = view.findViewById<TextView>(R.id.tv_history_adherence_badge)

        val btnPrevMonth = view.findViewById<TextView>(R.id.btn_prev_month)
        val btnNextMonth = view.findViewById<TextView>(R.id.btn_next_month)
        val tvMonthYear = view.findViewById<TextView>(R.id.tv_calendar_month_year)
        val rvCalendarGrid = view.findViewById<RecyclerView>(R.id.rv_calendar_grid)

        val tvSelectedDateLabel = view.findViewById<TextView>(R.id.tv_selected_date_label)
        val tvDateAdherenceDesc = view.findViewById<TextView>(R.id.tv_date_adherence_desc)
        val tvDateAdherenceStatus = view.findViewById<TextView>(R.id.tv_date_adherence_status)

        val rvLogs = view.findViewById<RecyclerView>(R.id.rv_history_logs)
        val layoutEmpty = view.findViewById<LinearLayout>(R.id.layout_empty_history)

        rvLogs?.layoutManager = LinearLayoutManager(activity)
        rvLogs?.adapter = medicationAdapter

        val fullDateFormat = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.US)
        val isoDateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val monthYearFormat = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.US)

        var displayCalendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        var selectedCalendar = java.util.Calendar.getInstance()

        fun isSameDay(cal1: java.util.Calendar, cal2: java.util.Calendar): Boolean {
            return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                    cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
        }

        fun getDosesForCalendarDate(targetCal: java.util.Calendar): List<DoseLogEntity> {
            val targetIso = isoDateFormat.format(targetCal.time)
            val targetFull = fullDateFormat.format(targetCal.time)

            val nowCal = java.util.Calendar.getInstance()
            val isToday = isSameDay(targetCal, nowCal)

            val diffMs = nowCal.timeInMillis - targetCal.timeInMillis
            val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()

            val allDoses = viewModel.doseLogs.value
            val activeMeds = viewModel.medications.value.filter { it.status == "Active" }

            val matchedLogs = allDoses.filter { log ->
                log.date == targetIso ||
                log.date.equals(targetFull, ignoreCase = true) ||
                (isToday && (log.date == "Today" || log.date == targetIso)) ||
                (diffDays == 1 && (log.date == "Yesterday" || log.date == targetIso)) ||
                (diffDays == 2 && (log.date == "2 days ago" || log.date == targetIso)) ||
                (diffDays in 3..30 && log.date.contains("$diffDays days ago", ignoreCase = true))
            }.distinctBy { "${it.medicationId}_${it.scheduledTime}" }

            if (matchedLogs.isNotEmpty()) {
                return matchedLogs
            }

            if (isToday && activeMeds.isNotEmpty()) {
                return activeMeds.flatMap { med ->
                    val times = med.reminderTimes.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("08:00 AM") }
                    times.map { timeStr ->
                        DoseLogEntity(
                            medicationId = med.id,
                            medicationName = if (med.dosage.isNotBlank() && med.dosage != "1 dose") "${med.name} (${med.dosage})" else med.name,
                            scheduledTime = timeStr,
                            status = "Pending",
                            date = "Today"
                        )
                    }
                }.distinctBy { "${it.medicationId}_${it.scheduledTime}" }
            }

            return emptyList()
        }

        fun updateSelectedDateDetails(cal: java.util.Calendar) {
            val dateFormatted = fullDateFormat.format(cal.time)
            tvSelectedDateLabel?.text = dateFormatted

            val dateDoses = getDosesForCalendarDate(cal)
            medicationAdapter.updateData(dateDoses)

            layoutEmpty?.visibility = if (dateDoses.isEmpty()) View.VISIBLE else View.GONE
            rvLogs?.visibility = if (dateDoses.isEmpty()) View.GONE else View.VISIBLE

            val takenCount = dateDoses.count { it.status == "Taken" }
            val totalCount = dateDoses.size
            val pct = if (totalCount > 0) (takenCount * 100) / totalCount else 0

            val nowCal = java.util.Calendar.getInstance()
            nowCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            nowCal.set(java.util.Calendar.MINUTE, 0)
            nowCal.set(java.util.Calendar.SECOND, 0)
            nowCal.set(java.util.Calendar.MILLISECOND, 0)

            val checkCal = cal.clone() as java.util.Calendar
            checkCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            checkCal.set(java.util.Calendar.MINUTE, 0)
            checkCal.set(java.util.Calendar.SECOND, 0)
            checkCal.set(java.util.Calendar.MILLISECOND, 0)

            val isFuture = checkCal.after(nowCal)

            if (totalCount == 0) {
                tvDateAdherenceStatus?.text = "No Prescriptions"
                tvDateAdherenceStatus?.setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
                tvDateAdherenceStatus?.setBackgroundResource(R.drawable.bg_badge_teal)
                tvDateAdherenceDesc?.text = "No prescriptions scheduled for this date"
            } else if (isFuture) {
                tvDateAdherenceStatus?.text = "⏰ Scheduled ($totalCount Doses)"
                tvDateAdherenceStatus?.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
                tvDateAdherenceStatus?.setBackgroundResource(R.drawable.bg_badge_amber)
                tvDateAdherenceDesc?.text = "Future scheduled doses on $dateFormatted"
            } else if (pct == 100) {
                tvDateAdherenceStatus?.text = "✓ 100% Taken"
                tvDateAdherenceStatus?.setTextColor(android.graphics.Color.parseColor("#10B981"))
                tvDateAdherenceStatus?.setBackgroundResource(R.drawable.bg_badge_green)
                tvDateAdherenceDesc?.text = "All $totalCount scheduled doses marked as taken!"
            } else {
                tvDateAdherenceStatus?.text = "⏰ $takenCount of $totalCount Taken"
                tvDateAdherenceStatus?.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
                tvDateAdherenceStatus?.setBackgroundResource(R.drawable.bg_badge_amber)
                tvDateAdherenceDesc?.text = "${totalCount - takenCount} dose(s) not taken on $dateFormatted"
            }

            tvSubtitle?.text = "${dateDoses.size} prescription logs on $dateFormatted"
        }

        fun renderCalendarGrid() {
            tvMonthYear?.text = monthYearFormat.format(displayCalendar.time)

            val monthCal = displayCalendar.clone() as java.util.Calendar
            monthCal.set(java.util.Calendar.DAY_OF_MONTH, 1)
            val firstDayOfWeek = monthCal.get(java.util.Calendar.DAY_OF_WEEK) - 1 // 0-based offset for Sun
            val daysInMonth = monthCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

            val dayItems = mutableListOf<com.example.ui.adapters.CalendarDayItem>()

            // Add blank leading padding cells
            for (i in 0 until firstDayOfWeek) {
                dayItems.add(
                    com.example.ui.adapters.CalendarDayItem(
                        dayNumber = 0,
                        fullDateString = "",
                        isoDateString = "",
                        calendar = null,
                        isCurrentMonth = false,
                        status = com.example.ui.adapters.DayAdherenceStatus.NO_DOSES,
                        isSelected = false
                    )
                )
            }

            val selectedIso = isoDateFormat.format(selectedCalendar.time)

            val nowCal = java.util.Calendar.getInstance()
            nowCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            nowCal.set(java.util.Calendar.MINUTE, 0)
            nowCal.set(java.util.Calendar.SECOND, 0)
            nowCal.set(java.util.Calendar.MILLISECOND, 0)

            // Populate all days in current month
            for (day in 1..daysInMonth) {
                val dayCal = displayCalendar.clone() as java.util.Calendar
                dayCal.set(java.util.Calendar.DAY_OF_MONTH, day)

                val dayDateFormatted = fullDateFormat.format(dayCal.time)
                val dayIsoFormatted = isoDateFormat.format(dayCal.time)

                val isSelected = dayIsoFormatted == selectedIso

                val dateDoses = getDosesForCalendarDate(dayCal)
                val takenCount = dateDoses.count { it.status == "Taken" }
                val totalCount = dateDoses.size

                val checkCal = dayCal.clone() as java.util.Calendar
                checkCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                checkCal.set(java.util.Calendar.MINUTE, 0)
                checkCal.set(java.util.Calendar.SECOND, 0)
                checkCal.set(java.util.Calendar.MILLISECOND, 0)

                val isFuture = checkCal.after(nowCal)

                val status = when {
                    totalCount == 0 -> com.example.ui.adapters.DayAdherenceStatus.NO_DOSES
                    isFuture -> com.example.ui.adapters.DayAdherenceStatus.UPCOMING_SCHEDULED
                    takenCount > 0 && takenCount == totalCount -> com.example.ui.adapters.DayAdherenceStatus.ALL_TAKEN
                    else -> com.example.ui.adapters.DayAdherenceStatus.NOT_TAKEN_DUE
                }

                dayItems.add(
                    com.example.ui.adapters.CalendarDayItem(
                        dayNumber = day,
                        fullDateString = dayDateFormatted,
                        isoDateString = dayIsoFormatted,
                        calendar = dayCal,
                        isCurrentMonth = true,
                        status = status,
                        isSelected = isSelected
                    )
                )
            }

            val calendarAdapter = com.example.ui.adapters.CalendarGridXmlAdapter(dayItems) { clickedDay ->
                if (clickedDay.calendar != null) {
                    selectedCalendar = clickedDay.calendar
                    renderCalendarGrid()
                    updateSelectedDateDetails(selectedCalendar)
                }
            }

            rvCalendarGrid?.layoutManager = GridLayoutManager(activity, 7)
            rvCalendarGrid?.adapter = calendarAdapter
        }

        btnPrevMonth?.setOnClickListener {
            displayCalendar.add(java.util.Calendar.MONTH, -1)
            renderCalendarGrid()
        }

        btnNextMonth?.setOnClickListener {
            displayCalendar.add(java.util.Calendar.MONTH, 1)
            renderCalendarGrid()
        }

        scope.launch {
            kotlinx.coroutines.flow.combine(
                viewModel.doseLogs,
                viewModel.medications
            ) { _, _ ->
                renderCalendarGrid()
                updateSelectedDateDetails(selectedCalendar)
            }.collectLatest {}
        }

        renderCalendarGrid()
        updateSelectedDateDetails(selectedCalendar)
    }

    // Active Clinic View references
    private var activeClinicWebView: android.webkit.WebView? = null
    private var activeRouteCard: View? = null
    private var activeTvRouteClinicName: TextView? = null
    private var activeTvRouteDetails: TextView? = null
    private var activeTvRouteAddress: TextView? = null
    private var activeBtnRouteCall: View? = null
    private var activeClinicScrollView: View? = null

    private fun routeToClinicInApp(clinic: ClinicEntity) {
        val webView = activeClinicWebView
        val routeCard = activeRouteCard
        val tvName = activeTvRouteClinicName
        val tvDetails = activeTvRouteDetails
        val tvAddress = activeTvRouteAddress
        val btnCall = activeBtnRouteCall

        // Smoothly scroll up so the user sees the map and route
        (activeClinicScrollView as? android.widget.ScrollView)?.smoothScrollTo(0, 0)

        if (routeCard != null && tvName != null && tvDetails != null && tvAddress != null) {
            val driveMins = maxOf(2, (clinic.distanceKm * 2.8).toInt())
            val walkMins = maxOf(5, (clinic.distanceKm * 14.0).toInt())

            tvName.text = clinic.name
            tvDetails.text = "Distance: ${clinic.distanceKm} km • In-App Route: ~$driveMins mins drive / ~$walkMins mins walk"
            tvAddress.text = clinic.address
            routeCard.visibility = View.VISIBLE

            btnCall?.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${clinic.phone.ifBlank { "911" }}"))
                activity.startActivity(intent)
            }
        }

        if (webView != null) {
            val clinicNameEscaped = clinic.name.replace("'", "\\'")
            val lat = clinic.latitude ?: (-25.7479 + clinic.distanceKm * 0.008)
            val lng = clinic.longitude ?: (28.2293 + clinic.distanceKm * 0.008)
            webView.evaluateJavascript("routeTo($lat, $lng, '$clinicNameEscaped')", null)
        }
    }

    private fun performClinicSearch(query: String, hintTv: TextView?) {
        val q = query.trim()
        if (q.isBlank()) {
            Toast.makeText(activity, "Please enter a clinic or hospital name", Toast.LENGTH_SHORT).show()
            return
        }

        val allClinics = viewModel.clinics.value
        val localMatch = allClinics.firstOrNull {
            it.name.contains(q, ignoreCase = true) || it.address.contains(q, ignoreCase = true)
        }
        if (localMatch != null) {
            routeToClinicInApp(localMatch)
            Toast.makeText(activity, "Routing to ${localMatch.name}", Toast.LENGTH_SHORT).show()
            return
        }

        hintTv?.text = "Searching MapTiler for '$q'..."
        scope.launch(Dispatchers.IO) {
            try {
                val encodedQuery = java.net.URLEncoder.encode(q, "UTF-8")
                val apiKey = "JhXKARTgXfWBUYsXkPWK"
                val urlStr = "https://api.maptiler.com/geocoding/$encodedQuery.json?key=$apiKey&proximity=28.2293,-25.7479&country=za"
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = okhttp3.Request.Builder().url(urlStr).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful && !body.isNullOrBlank()) {
                    val json = org.json.JSONObject(body)
                    val features = json.optJSONArray("features")
                    if (features != null && features.length() > 0) {
                        val first = features.getJSONObject(0)
                        val placeName = first.optString("place_name", q)
                        val geom = first.optJSONObject("geometry")
                        val coords = geom?.optJSONArray("coordinates")
                        val lng = coords?.optDouble(0) ?: 28.2293
                        val lat = coords?.optDouble(1) ?: -25.7479

                        val dist = com.example.data.calculateDistanceKm(-25.7479, 28.2293, lat, lng)
                        val cleanTitle = if (first.has("text")) first.getString("text") else q

                        val newClinic = ClinicEntity(
                            id = System.currentTimeMillis() % 100000,
                            name = cleanTitle,
                            distanceKm = String.format(java.util.Locale.US, "%.1f", dist).toDouble(),
                            openHours = "Emergency 24/7",
                            address = placeName,
                            phone = "+27 12 354 1000",
                            latitude = lat,
                            longitude = lng
                        )

                        viewModel.addCustomClinic(
                            name = newClinic.name,
                            address = newClinic.address,
                            phone = newClinic.phone,
                            openHours = newClinic.openHours,
                            distanceKm = newClinic.distanceKm,
                            latitude = lat,
                            longitude = lng
                        )

                        withContext(Dispatchers.Main) {
                            hintTv?.text = "Facility located! Routing on MapTiler..."
                            routeToClinicInApp(newClinic)
                            Toast.makeText(activity, "Directions ready for ${newClinic.name}!", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                }
            } catch (_: Throwable) {}

            withContext(Dispatchers.Main) {
                viewModel.addCustomClinic(q, "Gauteng / Pretoria Region", "+27 12 354 1000", "24/7 Service")
                Toast.makeText(activity, "Added & routing to $q", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun showAddCustomClinicDialog() {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_add_clinic, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_dialog_clinic_name)
        val etAddress = dialogView.findViewById<EditText>(R.id.et_dialog_clinic_address)
        val etPhone = dialogView.findViewById<EditText>(R.id.et_dialog_clinic_phone)
        val etHours = dialogView.findViewById<EditText>(R.id.et_dialog_clinic_hours)

        AlertDialog.Builder(activity)
            .setView(dialogView)
            .setPositiveButton("Save Facility") { _, _ ->
                val name = etName?.text.toString().trim()
                val address = etAddress?.text.toString().trim().ifBlank { "Pretoria / Gauteng" }
                val phone = etPhone?.text.toString().trim().ifBlank { "+27 12 354 1000" }
                val hours = etHours?.text.toString().trim().ifBlank { "Emergency 24/7" }

                if (name.isNotBlank()) {
                    viewModel.addCustomClinic(name, address, phone, hours)
                    Toast.makeText(activity, "Saved $name to directory", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, "Please enter a facility name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private var gpsLocationCallback: com.google.android.gms.location.LocationCallback? = null
    private var gpsFusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient? = null

    @SuppressLint("MissingPermission")
    private fun startGpsTrackingForMap(webView: android.webkit.WebView) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                102
            )
            return
        }

        try {
            if (gpsFusedLocationClient == null) {
                gpsFusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(activity)
            }

            gpsLocationCallback?.let { gpsFusedLocationClient?.removeLocationUpdates(it) }

            val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 1500L
            ).setMinUpdateIntervalMillis(1000L).build()

            gpsLocationCallback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    val location = result.lastLocation ?: return
                    val lat = location.latitude
                    val lng = location.longitude
                    webView.post {
                        webView.evaluateJavascript("updateUserLocation($lat, $lng)", null)
                    }
                }
            }

            gpsFusedLocationClient?.requestLocationUpdates(
                locationRequest,
                gpsLocationCallback!!,
                android.os.Looper.getMainLooper()
            )
        } catch (e: Throwable) {
            android.util.Log.e("GpsTracker", "Error starting GPS tracking", e)
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 202 && resultCode == Activity.RESULT_OK) {
            val spokenText = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                val etSearch = currentScreenLayout?.findViewById<EditText>(R.id.et_search_clinics)
                val btnSearch = currentScreenLayout?.findViewById<Button>(R.id.btn_search_online_clinic)
                etSearch?.setText(spokenText)
                btnSearch?.performClick()
            }
        }
    }

    // --- 6. Clinics View Setup ---
    fun setupClinicsView(view: View) {
        val etSearch = view.findViewById<EditText>(R.id.et_search_clinics)
        val btnVoice = view.findViewById<TextView>(R.id.btn_voice_search_clinic)
        val btnSearch = view.findViewById<Button>(R.id.btn_search_online_clinic)
        val webView = view.findViewById<android.webkit.WebView>(R.id.webview_clinic_map)

        val btnRecenterGps = view.findViewById<TextView>(R.id.btn_recenter_gps)

        val chipHospitals = view.findViewById<TextView>(R.id.chip_search_hospitals)
        val chipClinics = view.findViewById<TextView>(R.id.chip_search_clinics)
        val chipPrivate = view.findViewById<TextView>(R.id.chip_search_private_hospitals)
        val chipPharmacies = view.findViewById<TextView>(R.id.chip_search_pharmacies)
        val chipEmergency = view.findViewById<TextView>(R.id.chip_search_emergency)

        if (webView != null) {
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.setGeolocationEnabled(true)
            webView.webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: android.webkit.GeolocationPermissions.Callback?
                ) {
                    callback?.invoke(origin, true, false)
                }
            }
            webView.loadUrl("file:///android_asset/google_map.html")
            startGpsTrackingForMap(webView)
        }

        btnVoice?.setOnClickListener {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak facility or clinic name...")
                }
                activity.startActivityForResult(intent, 202)
            } catch (_: Exception) {
                Toast.makeText(activity, "Voice search is not supported on this device", Toast.LENGTH_SHORT).show()
            }
        }

        fun performSearch(query: String) {
            val q = query.trim()
            if (q.isBlank()) return

            scope.launch(Dispatchers.IO) {
                try {
                    val encodedQuery = java.net.URLEncoder.encode(q, "UTF-8")
                    val urlStr = "https://api.maptiler.com/geocoding/$encodedQuery.json?key=JhXKARTgXfWBUYsXkPWK&proximity=28.2293,-25.7479&country=za"
                    val client = okhttp3.OkHttpClient.Builder()
                        .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                    val request = okhttp3.Request.Builder().url(urlStr).build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string()

                    if (response.isSuccessful && !body.isNullOrBlank()) {
                        val json = org.json.JSONObject(body)
                        val features = json.optJSONArray("features")
                        if (features != null && features.length() > 0) {
                            var bestFeature = features.getJSONObject(0)
                            var bestDist = Double.MAX_VALUE

                            // Find the closest facility relative to user location
                            for (i in 0 until features.length()) {
                                val feat = features.getJSONObject(i)
                                val geom = feat.optJSONObject("geometry")
                                val coords = geom?.optJSONArray("coordinates")
                                if (coords != null) {
                                    val lng = coords.optDouble(0)
                                    val lat = coords.optDouble(1)
                                    val dist = com.example.data.calculateDistanceKm(-25.7479, 28.2293, lat, lng)
                                    if (dist < bestDist) {
                                        bestDist = dist
                                        bestFeature = feat
                                    }
                                }
                            }

                            val placeName = bestFeature.optString("place_name", q)
                            val geom = bestFeature.optJSONObject("geometry")
                            val coords = geom?.optJSONArray("coordinates")
                            val lng = coords?.optDouble(0) ?: 28.2293
                            val lat = coords?.optDouble(1) ?: -25.7479

                            withContext(Dispatchers.Main) {
                                val escapedName = placeName.replace("'", "\\'")
                                etSearch?.setText(placeName)
                                webView?.evaluateJavascript("searchLocation($lat, $lng, '$escapedName')", null)
                                Toast.makeText(activity, "Closest Facility: $placeName", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }
                    }
                } catch (_: Throwable) {}

                withContext(Dispatchers.Main) {
                    val escapedQuery = q.replace("'", "\\'")
                    webView?.evaluateJavascript("searchLocation(-25.7479, 28.2293, '$escapedQuery')", null)
                }
            }
        }

        btnSearch?.setOnClickListener {
            val q = etSearch?.text?.toString() ?: ""
            performSearch(q)
        }

        chipHospitals?.setOnClickListener {
            etSearch?.setText("Hospital")
            performSearch("Hospital")
        }

        chipClinics?.setOnClickListener {
            etSearch?.setText("Public Clinic")
            performSearch("Public Clinic")
        }

        chipPrivate?.setOnClickListener {
            etSearch?.setText("Private Hospital")
            performSearch("Private Hospital")
        }

        chipPharmacies?.setOnClickListener {
            etSearch?.setText("Pharmacy")
            performSearch("Pharmacy")
        }

        chipEmergency?.setOnClickListener {
            etSearch?.setText("Emergency Medical Care")
            performSearch("Emergency Medical Care")
        }

        btnRecenterGps?.setOnClickListener {
            webView?.evaluateJavascript("recenterUserLocation()", null)
            Toast.makeText(activity, "Centered on live location", Toast.LENGTH_SHORT).show()
        }
    }

    // --- 7. Settings View Setup ---
    fun setupSettingsView(view: View) {
        val tvAvatar = view.findViewById<TextView>(R.id.tv_settings_avatar)
        val tvName = view.findViewById<TextView>(R.id.tv_settings_name)
        val tvEmail = view.findViewById<TextView>(R.id.tv_settings_email)

        val spinnerLang = view.findViewById<android.widget.Spinner>(R.id.spinner_language)
        val tvActiveLang = view.findViewById<TextView>(R.id.tv_active_language)

        val switchReminders = view.findViewById<android.widget.Switch>(R.id.switch_reminders)

        val btnCallEmergency = view.findViewById<Button>(R.id.btn_call_emergency_contact)
        val btnCallDoctor = view.findViewById<Button>(R.id.btn_call_doctor)
        val btnSignOut = view.findViewById<Button>(R.id.btn_sign_out)

        // Observe user session
        scope.launch {
            viewModel.userSession.collectLatest { session ->
                if (session == null) return@collectLatest
                val fullName = session.fullName
                val email = session.email
                val lang = session.preferredLanguage

                tvName?.text = fullName
                tvEmail?.text = email
                tvActiveLang?.text = "$lang (South Africa)"

                val initials = fullName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
                tvAvatar?.text = initials.ifBlank { "LM" }

                if (switchReminders?.isChecked != session.medicationRemindersEnabled) {
                    switchReminders?.isChecked = session.medicationRemindersEnabled
                }

                applyThemeToXmlRoot(true)
            }
        }

        // Language Selection Spinner Setup
        val languages = arrayOf("English", "Sepedi", "Sesotho", "Setswana", "isiZulu", "Afrikaans", "isiXhosa")
        val adapter = android.widget.ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, languages)
        spinnerLang?.adapter = adapter

        spinnerLang?.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLang = languages[position]
                if (selectedLang != (viewModel.userSession.value?.preferredLanguage ?: "English")) {
                    viewModel.updateLanguage(selectedLang)
                    tvActiveLang?.text = "$selectedLang (South Africa)"
                    Toast.makeText(activity, "Language updated to $selectedLang", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        switchReminders?.setOnCheckedChangeListener { _, isChecked ->
            val currentReminders = viewModel.userSession.value?.medicationRemindersEnabled ?: true
            if (isChecked != currentReminders) {
                viewModel.toggleReminders(isChecked)
            }
        }

        btnCallEmergency?.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+27123456789"))
            activity.startActivity(intent)
        }

        btnCallDoctor?.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+27129876543"))
            activity.startActivity(intent)
        }

        btnSignOut?.setOnClickListener {
            viewModel.logout()
            showScreen(-1) // Show Auth Screen
            Toast.makeText(activity, "Signed out successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyThemeToXmlRoot(isDark: Boolean = true) {
        val rootBg = android.graphics.Color.parseColor("#111818")
        val topBottomBg = android.graphics.Color.parseColor("#1A2322")
        val textColor = android.graphics.Color.parseColor("#F3F4F6")
        val textSecColor = android.graphics.Color.parseColor("#9CA3AF")
        val cardBg = android.graphics.Color.parseColor("#1A2322")

        rootContainer.setBackgroundColor(rootBg)
        fragmentContainer?.setBackgroundColor(rootBg)
        currentScreenLayout?.setBackgroundColor(rootBg)

        val topBar = rootContainer.findViewById<View>(R.id.app_top_bar)
        topBar?.setBackgroundColor(topBottomBg)

        val tvTitle = rootContainer.findViewById<TextView>(R.id.tv_app_title)
        tvTitle?.setTextColor(textColor)

        bottomNavBar?.setBackgroundColor(topBottomBg)

        currentScreenLayout?.let { root ->
            updateViewColorsRecursively(root, isDark, rootBg, cardBg, textColor, textSecColor)
        }
    }

    private fun updateViewColorsRecursively(
        view: View,
        isDark: Boolean,
        bgCol: Int,
        cardCol: Int,
        textPrimaryCol: Int,
        textSecCol: Int
    ) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                updateViewColorsRecursively(view.getChildAt(i), isDark, bgCol, cardCol, textPrimaryCol, textSecCol)
            }
        }

        if (view is TextView) {
            val currentHex = String.format("#%06X", (0xFFFFFF and view.currentTextColor))
            if (currentHex.equals("#F3F4F6", ignoreCase = true) || currentHex.equals("#0F172A", ignoreCase = true) || currentHex.equals("#F9FAFB", ignoreCase = true) || currentHex.equals("#111827", ignoreCase = true)) {
                view.setTextColor(textPrimaryCol)
            } else if (currentHex.equals("#9CA3AF", ignoreCase = true) || currentHex.equals("#475569", ignoreCase = true) || currentHex.equals("#6B7280", ignoreCase = true)) {
                view.setTextColor(textSecCol)
            }
        }

        if (view is EditText) {
            view.setTextColor(textPrimaryCol)
            view.setHintTextColor(textSecCol)
            val editBg = if (isDark) android.graphics.Color.parseColor("#24302E") else android.graphics.Color.parseColor("#F1F5F9")
            view.setBackgroundColor(editBg)
        }
    }
}
