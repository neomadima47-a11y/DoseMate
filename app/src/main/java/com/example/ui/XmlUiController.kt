package com.example.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.ClinicEntity
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
                if (session == null || !session.isLoggedIn) {
                    showScreen(-1) // Show Auth
                } else if (activeNavIndex == -1) {
                    showScreen(0) // Show Dashboard once logged in
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
        val btnReturnDashboard = view.findViewById<Button>(R.id.btn_auth_return_dashboard)

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
                btnPrimary?.text = "Create HealthBridge Account"
                tvGoogleLabel?.text = "Sign up with Google"
            } else {
                tabSignIn?.setBackgroundResource(R.drawable.bg_tab_selected)
                tabSignIn?.setTextColor(android.graphics.Color.WHITE)
                tabRegister?.setBackgroundResource(0)
                tabRegister?.setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
                layoutName?.visibility = View.GONE
                btnPrimary?.text = "Sign In to HealthBridge"
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

        btnReturnDashboard?.setOnClickListener {
            showScreen(0)
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
            val pin = etPin?.text?.toString()?.trim() ?: ""
            val selectedLang = spinnerLang?.selectedItem?.toString() ?: "English"

            if (email.isBlank() || !email.contains("@") || !email.contains(".")) {
                Toast.makeText(activity, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                etEmail?.requestFocus()
                return@setOnClickListener
            }
            if (pin.isBlank()) {
                Toast.makeText(activity, "Please enter your PIN / password", Toast.LENGTH_SHORT).show()
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
                if (pin.length < 4) {
                    Toast.makeText(activity, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                    etPin?.requestFocus()
                    return@setOnClickListener
                }
                viewModel.register(email, inputName, selectedLang, pin)
                Toast.makeText(activity, "Account created! Welcome, $inputName!", Toast.LENGTH_SHORT).show()
                showScreen(0)
            } else {
                val session = viewModel.userSession.value
                if (session != null && session.email.isNotBlank() && session.email.equals(email, ignoreCase = true)) {
                    if (session.securityPin.isNotBlank() && session.securityPin != pin) {
                        Toast.makeText(activity, "Incorrect PIN. Tap 'Forgot PIN / Password?' to reset.", Toast.LENGTH_LONG).show()
                        etPin?.requestFocus()
                        return@setOnClickListener
                    }
                }

                val name = if (session != null && session.email.equals(email, ignoreCase = true) && session.fullName.isNotBlank()) {
                    session.fullName
                } else {
                    email.substringBefore('@').replace('.', ' ').replace('_', ' ')
                }

                viewModel.login(email, name, selectedLang, pin)
                Toast.makeText(activity, "Welcome back, $name!", Toast.LENGTH_SHORT).show()
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
        val rvRecentLogs = view.findViewById<RecyclerView>(R.id.rv_recent_logs)
        val tvMedsCount = view.findViewById<TextView>(R.id.tv_meds_count_badge)
        val tvVitalsCount = view.findViewById<TextView>(R.id.tv_vitals_count_badge)

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
        tvVitalsCount?.setOnClickListener { showScreen(3) }

        // Today's Medications RecyclerView
        rvTodayMeds?.layoutManager = LinearLayoutManager(activity)
        rvTodayMeds?.adapter = medicationAdapter

        // Recent Vitals RecyclerView
        rvRecentLogs?.layoutManager = LinearLayoutManager(activity)
        rvRecentLogs?.adapter = healthLogAdapter

        scope.launch {
            viewModel.doseLogs.collectLatest { list ->
                medicationAdapter.updateData(list)
                updateAdherence()
            }
        }

        scope.launch {
            viewModel.healthReadings.collectLatest { list ->
                healthLogAdapter.updateData(list.take(5))
            }
        }
    }

    // --- 3. Log Reading View Setup ---
    fun setupLogReadingView(view: View) {
        // Header history navigation
        view.findViewById<LinearLayout>(R.id.btn_header_history)?.setOnClickListener {
            showScreen(3)
        }

        // State Variables
        var selectedType = "Blood Pressure"
        var systolic = 120
        var diastolic = 80
        var glucose = 5.4
        val selectedSymptoms = mutableSetOf<String>()

        // Tabs & Containers
        val tabBp = view.findViewById<LinearLayout>(R.id.tab_blood_pressure)
        val tabSugar = view.findViewById<LinearLayout>(R.id.tab_blood_sugar)
        val tabSymptom = view.findViewById<LinearLayout>(R.id.tab_symptom)
        val tabBpTitle = view.findViewById<TextView>(R.id.tab_bp_title)
        val tabSugarTitle = view.findViewById<TextView>(R.id.tab_sugar_title)
        val tabSymptomTitle = view.findViewById<TextView>(R.id.tab_symptom_title)

        val layoutBp = view.findViewById<LinearLayout>(R.id.layout_bp_container)
        val layoutSugar = view.findViewById<LinearLayout>(R.id.layout_sugar_container)
        val layoutSymptom = view.findViewById<LinearLayout>(R.id.layout_symptom_container)

        // BP & Sugar Controls
        val tvSysVal = view.findViewById<TextView>(R.id.tv_systolic_value)
        val tvDiaVal = view.findViewById<TextView>(R.id.tv_diastolic_value)
        val tvGlucoseVal = view.findViewById<TextView>(R.id.tv_glucose_value)
        val etSymptomInput = view.findViewById<EditText>(R.id.et_symptom_input)

        // Clinical Gauge Elements
        val tvGaugeBadgeIcon = view.findViewById<TextView>(R.id.tv_gauge_badge_icon)
        val tvGaugeClassification = view.findViewById<TextView>(R.id.tv_gauge_classification)
        val tvGaugeSeverityTag = view.findViewById<TextView>(R.id.tv_gauge_severity_tag)
        val tvGaugeFeedbackMessage = view.findViewById<TextView>(R.id.tv_gauge_feedback_message)
        val layoutRedFlag = view.findViewById<LinearLayout>(R.id.layout_red_flag_warning)
        val tvRedFlag = view.findViewById<TextView>(R.id.tv_red_flag_warning)

        // Dynamic Clinical Evaluation Routine
        fun updateEvaluation() {
            val symptomNotes = etSymptomInput?.text?.toString() ?: ""
            val evaluation = if (selectedType == "Blood Pressure") {
                ClinicalEvaluator.evaluateBloodPressure(systolic.toDouble(), diastolic.toDouble(), symptomNotes, "Resting")
            } else {
                ClinicalEvaluator.evaluateBloodSugar(glucose, "mmol/L", symptomNotes, "Fasting")
            }

            tvGaugeClassification?.text = evaluation.classification
            tvGaugeFeedbackMessage?.text = evaluation.feedbackMessage

            val (tagColor, tagBg, severityName) = when (evaluation.severity) {
                com.example.util.HealthSeverity.NORMAL -> Triple(android.graphics.Color.parseColor("#10B981"), R.drawable.bg_badge_green, "Optimal")
                com.example.util.HealthSeverity.ELEVATED -> Triple(android.graphics.Color.parseColor("#F59E0B"), R.drawable.bg_badge_amber, "Elevated")
                com.example.util.HealthSeverity.WARNING -> Triple(android.graphics.Color.parseColor("#F59E0B"), R.drawable.bg_badge_amber, "Stage 1")
                com.example.util.HealthSeverity.CRITICAL -> Triple(android.graphics.Color.parseColor("#EF4444"), R.drawable.bg_badge_red, "Crisis")
                else -> Triple(android.graphics.Color.parseColor("#3B82F6"), R.drawable.bg_badge_teal, "Info")
            }

            tvGaugeBadgeIcon?.setTextColor(tagColor)
            tvGaugeSeverityTag?.text = severityName
            tvGaugeSeverityTag?.setTextColor(tagColor)
            tvGaugeSeverityTag?.setBackgroundResource(tagBg)

            if (evaluation.severity == com.example.util.HealthSeverity.CRITICAL) {
                layoutRedFlag?.visibility = View.VISIBLE
                tvRedFlag?.text = "Urgent: Clinical threshold exceeded. Seek clinical advice promptly."
            } else {
                layoutRedFlag?.visibility = View.GONE
            }
        }

        // Segment Switcher Routine
        fun switchTab(type: String) {
            selectedType = type
            tabBp?.setBackgroundResource(if (type == "Blood Pressure") R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected)
            tabBpTitle?.setTextColor(if (type == "Blood Pressure") android.graphics.Color.WHITE else android.graphics.Color.parseColor("#F3F4F6"))

            tabSugar?.setBackgroundResource(if (type == "Blood Glucose") R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected)
            tabSugarTitle?.setTextColor(if (type == "Blood Glucose") android.graphics.Color.WHITE else android.graphics.Color.parseColor("#F3F4F6"))

            tabSymptom?.setBackgroundResource(if (type == "Symptom") R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected)
            tabSymptomTitle?.setTextColor(if (type == "Symptom") android.graphics.Color.WHITE else android.graphics.Color.parseColor("#F3F4F6"))

            layoutBp?.visibility = if (type == "Blood Pressure") View.VISIBLE else View.GONE
            layoutSugar?.visibility = if (type == "Blood Glucose") View.VISIBLE else View.GONE
            layoutSymptom?.visibility = if (type == "Symptom") View.VISIBLE else View.GONE

            updateEvaluation()
        }

        tabBp?.setOnClickListener { switchTab("Blood Pressure") }
        tabSugar?.setOnClickListener { switchTab("Blood Glucose") }
        tabSymptom?.setOnClickListener { switchTab("Symptom") }

        // Stepper Handlers
        view.findViewById<TextView>(R.id.btn_systolic_minus)?.setOnClickListener {
            if (systolic > 70) { systolic -= 2; tvSysVal?.text = "$systolic"; updateEvaluation() }
        }
        view.findViewById<TextView>(R.id.btn_systolic_plus)?.setOnClickListener {
            if (systolic < 240) { systolic += 2; tvSysVal?.text = "$systolic"; updateEvaluation() }
        }
        view.findViewById<TextView>(R.id.btn_diastolic_minus)?.setOnClickListener {
            if (diastolic > 40) { diastolic -= 2; tvDiaVal?.text = "$diastolic"; updateEvaluation() }
        }
        view.findViewById<TextView>(R.id.btn_diastolic_plus)?.setOnClickListener {
            if (diastolic < 150) { diastolic += 2; tvDiaVal?.text = "$diastolic"; updateEvaluation() }
        }

        view.findViewById<TextView>(R.id.btn_glucose_minus)?.setOnClickListener {
            if (glucose > 2.0) {
                glucose = Math.round((glucose - 0.2) * 10.0) / 10.0
                tvGlucoseVal?.text = String.format(java.util.Locale.US, "%.1f", glucose)
                updateEvaluation()
            }
        }
        view.findViewById<TextView>(R.id.btn_glucose_plus)?.setOnClickListener {
            if (glucose < 25.0) {
                glucose = Math.round((glucose + 0.2) * 10.0) / 10.0
                tvGlucoseVal?.text = String.format(java.util.Locale.US, "%.1f", glucose)
                updateEvaluation()
            }
        }

        // Quick Presets
        view.findViewById<TextView>(R.id.chip_bp_115_75)?.setOnClickListener {
            systolic = 115; diastolic = 75; tvSysVal?.text = "115"; tvDiaVal?.text = "75"; updateEvaluation()
        }
        view.findViewById<TextView>(R.id.chip_bp_120_80)?.setOnClickListener {
            systolic = 120; diastolic = 80; tvSysVal?.text = "120"; tvDiaVal?.text = "80"; updateEvaluation()
        }
        view.findViewById<TextView>(R.id.chip_bp_130_85)?.setOnClickListener {
            systolic = 130; diastolic = 85; tvSysVal?.text = "130"; tvDiaVal?.text = "85"; updateEvaluation()
        }
        view.findViewById<TextView>(R.id.chip_bp_142_92)?.setOnClickListener {
            systolic = 142; diastolic = 92; tvSysVal?.text = "142"; tvDiaVal?.text = "92"; updateEvaluation()
        }
        view.findViewById<TextView>(R.id.chip_bp_160_100)?.setOnClickListener {
            systolic = 160; diastolic = 100; tvSysVal?.text = "160"; tvDiaVal?.text = "100"; updateEvaluation()
        }

        view.findViewById<TextView>(R.id.chip_glucose_42)?.setOnClickListener {
            glucose = 4.2; tvGlucoseVal?.text = "4.2"; updateEvaluation()
        }
        view.findViewById<TextView>(R.id.chip_glucose_54)?.setOnClickListener {
            glucose = 5.4; tvGlucoseVal?.text = "5.4"; updateEvaluation()
        }
        view.findViewById<TextView>(R.id.chip_glucose_68)?.setOnClickListener {
            glucose = 6.8; tvGlucoseVal?.text = "6.8"; updateEvaluation()
        }
        view.findViewById<TextView>(R.id.chip_glucose_85)?.setOnClickListener {
            glucose = 8.5; tvGlucoseVal?.text = "8.5"; updateEvaluation()
        }
        view.findViewById<TextView>(R.id.chip_glucose_115)?.setOnClickListener {
            glucose = 11.5; tvGlucoseVal?.text = "11.5"; updateEvaluation()
        }

        // Groq AI Chatbot
        val etGroqInput = view.findViewById<EditText>(R.id.et_groq_chat_input)
        val btnSendGroq = view.findViewById<TextView>(R.id.btn_send_groq_chat)
        val layoutGroqReplying = view.findViewById<LinearLayout>(R.id.layout_groq_replying)
        val layoutChatContainer = view.findViewById<LinearLayout>(R.id.layout_chat_messages_container)
        val chatHistory = mutableListOf<GeminiHealthClient.ChatMessagePayload>()

        fun addChatMessage(text: String, isUser: Boolean) {
            val msgView = TextView(activity).apply {
                this.text = text
                textSize = 12f
                setTextColor(if (isUser) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#F3F4F6"))
                setBackgroundResource(if (isUser) R.drawable.bg_chat_user else R.drawable.bg_chat_ai)
                setPadding(12, 10, 12, 10)
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
                gravity = if (isUser) android.view.Gravity.END else android.view.Gravity.START
            }
            msgView.layoutParams = params
            layoutChatContainer?.addView(msgView)
        }

        if (layoutChatContainer?.childCount == 0) {
            val initialGreeting = "Hello! I am your Groq Llama 3.3 AI clinical assistant. How can I help you understand your health readings, symptoms, diet, or lifestyle today?"
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

            val currentVitalsContext = "Current Selection: $selectedType, Reading: ${if (selectedType == "Blood Pressure") "$systolic/$diastolic mmHg" else "$glucose mmol/L"}, Classification: ${tvGaugeClassification?.text}"

            scope.launch {
                val result = GeminiHealthClient.chatWithGroq(
                    history = chatHistory.toList(),
                    currentVitalsContext = currentVitalsContext,
                    language = "English"
                )
                withContext(Dispatchers.Main) {
                    layoutGroqReplying?.visibility = View.GONE
                    val aiResponse = result.getOrElse { "I'm available to answer any health, diet, or vital questions you have." }
                    addChatMessage(aiResponse, isUser = false)
                    chatHistory.add(GeminiHealthClient.ChatMessagePayload(role = "assistant", content = aiResponse))
                }
            }
        }

        btnSendGroq?.setOnClickListener {
            val query = etGroqInput?.text.toString()
            sendUserMessage(query)
        }

        view.findViewById<TextView>(R.id.btn_clear_chat)?.setOnClickListener {
            layoutChatContainer?.removeAllViews()
            chatHistory.clear()
            val resetGreeting = "Chat reset. Ask me any medical, wellness, diet, or prescription questions!"
            addChatMessage(resetGreeting, isUser = false)
            chatHistory.add(GeminiHealthClient.ChatMessagePayload(role = "assistant", content = resetGreeting))
        }

        val inquiryMap = mapOf(
            R.id.chip_inquiry_foods to "What foods are best for heart health and blood pressure?",
            R.id.chip_inquiry_hydration to "How much water should I drink daily for optimal health?",
            R.id.chip_inquiry_sleep to "Give me sleep tips to manage stress and blood pressure.",
            R.id.chip_inquiry_bp to "What is a normal healthy blood pressure range?"
        )

        for ((chipId, inquiry) in inquiryMap) {
            view.findViewById<TextView>(chipId)?.setOnClickListener {
                sendUserMessage(inquiry)
            }
        }

        // Save Action
        val layoutSaved = view.findViewById<LinearLayout>(R.id.layout_saved_confirmation)
        val tvSavedSummary = view.findViewById<TextView>(R.id.tv_saved_summary)

        view.findViewById<Button>(R.id.btn_save_to_health_log)?.setOnClickListener {
            val (valStr, unitStr) = when (selectedType) {
                "Blood Pressure" -> Pair("$systolic/$diastolic", "mmHg")
                "Blood Glucose" -> Pair(String.format(java.util.Locale.US, "%.1f", glucose), "mmol/L")
                else -> Pair("Logged", "Symptom")
            }
            val notes = etSymptomInput?.text?.toString() ?: ""
            viewModel.addHealthReading(selectedType, valStr, unitStr, "Today", notes)

            layoutSaved?.visibility = View.VISIBLE
            tvSavedSummary?.text = "$selectedType: $valStr $unitStr logged successfully."
            Toast.makeText(activity, "Saved to Health Log!", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<TextView>(R.id.btn_dismiss_confirmation)?.setOnClickListener {
            layoutSaved?.visibility = View.GONE
        }
        view.findViewById<Button>(R.id.btn_view_in_health_log)?.setOnClickListener {
            showScreen(3)
        }
        view.findViewById<Button>(R.id.btn_log_another)?.setOnClickListener {
            layoutSaved?.visibility = View.GONE
        }

        // Quick Action Nav Row
        view.findViewById<Button>(R.id.btn_share_vitals_row)?.setOnClickListener {
            val text = "DoseMate Clinical Vitals: $selectedType -> ${if (selectedType == "Blood Pressure") "$systolic/$diastolic mmHg" else "$glucose mmol/L"} (${tvGaugeClassification?.text})"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            activity.startActivity(Intent.createChooser(intent, "Share Clinical Vitals"))
        }

        view.findViewById<Button>(R.id.btn_nav_meds_row)?.setOnClickListener { showScreen(2) }
        view.findViewById<Button>(R.id.btn_nav_dashboard_row)?.setOnClickListener { showScreen(0) }

        // Initial evaluation run
        updateEvaluation()
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

        fun applyFilter(filter: String) {
            currentFilter = filter
            val doses = viewModel.doseLogs.value
            val filtered = when (filter) {
                "Due" -> doses.filter { it.status != "Taken" }
                "Completed" -> doses.filter { it.status == "Taken" }
                else -> doses
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

            tvScheduleCount?.text = "${filtered.size} doses shown"
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
            viewModel.doseLogs.collectLatest {
                applyFilter(currentFilter)
            }
        }

        applyFilter("All")
    }

    fun showAddMedicationDialog(prefilledName: String = "") {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_add_medication, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_dialog_med_name)
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

                viewModel.addMedication(name, defaultDosage, freqText, timesFormatted)
                Toast.makeText(activity, "Scheduled $name ($freqText at $timesFormatted)", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- 5. Health Log View Setup ---
    fun setupHealthLogView(view: View) {
        val tvSubtitle = view.findViewById<TextView>(R.id.tv_history_subtitle)
        val btnLogNew = view.findViewById<Button>(R.id.btn_history_log_new)
        val bannerCritical = view.findViewById<LinearLayout>(R.id.layout_critical_vitals_banner)
        val btnBannerFindClinic = view.findViewById<TextView>(R.id.btn_banner_find_clinic)
        val chipAll = view.findViewById<TextView>(R.id.chip_filter_all)
        val chipSugar = view.findViewById<TextView>(R.id.chip_filter_sugar)
        val chipPressure = view.findViewById<TextView>(R.id.chip_filter_pressure)
        val chipSymptoms = view.findViewById<TextView>(R.id.chip_filter_symptoms)
        val tvTargetBadge = view.findViewById<TextView>(R.id.tv_trend_target_badge)
        val tvTrendDesc = view.findViewById<TextView>(R.id.tv_trend_card_desc)
        val rvLogs = view.findViewById<RecyclerView>(R.id.rv_history_logs)
        val layoutEmpty = view.findViewById<LinearLayout>(R.id.layout_empty_history)

        val fullHistoryAdapter = HealthLogXmlAdapter()
        rvLogs?.layoutManager = LinearLayoutManager(activity)
        rvLogs?.adapter = fullHistoryAdapter

        val allReadings = viewModel.healthReadings.value

        // Dynamic Filter Handler (All, Sugar, Pressure, Symptoms)
        fun applyFilter(filter: String) {
            val filtered = when (filter) {
                "Sugar" -> allReadings.filter { 
                    it.type.contains("sugar", ignoreCase = true) || it.type.contains("glucose", ignoreCase = true) 
                }
                "Pressure" -> allReadings.filter { 
                    it.type.contains("pressure", ignoreCase = true) 
                }
                "Symptoms" -> allReadings.filter { 
                    it.type.contains("symptom", ignoreCase = true) 
                }
                else -> allReadings
            }

            fullHistoryAdapter.updateData(filtered)
            layoutEmpty?.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            rvLogs?.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE

            // Toggle Visual Tab State
            val chips = listOfNotNull(chipAll, chipSugar, chipPressure, chipSymptoms)
            chips.forEach { chip ->
                val isSelected = (chip == chipAll && filter == "All") ||
                                 (chip == chipSugar && filter == "Sugar") ||
                                 (chip == chipPressure && filter == "Pressure") ||
                                 (chip == chipSymptoms && filter == "Symptoms")
                chip.setBackgroundResource(if (isSelected) R.drawable.bg_button_teal else 0)
                chip.setTextColor(android.graphics.Color.parseColor(if (isSelected) "#FFFFFF" else "#9CA3AF"))
            }

            // Update target badge and guidance description based on category
            when (filter) {
                "Pressure" -> {
                    tvTargetBadge?.text = "Target < 120/80 mmHg"
                    tvTrendDesc?.text = "Systolic and diastolic pressures evaluated for chronic hypertension control."
                }
                "Sugar" -> {
                    tvTargetBadge?.text = "Target: 4.0 - 10.0 mmol/L"
                    tvTrendDesc?.text = "Blood glucose readings tracked for diabetes and glycaemic health."
                }
                else -> {
                    tvTargetBadge?.text = "Target: Normal"
                    tvTrendDesc?.text = "All physiological vital signs and symptoms recorded on-device and synchronized."
                }
            }
        }

        // Check for critical readings to display the alert banner
        val hasCritical = allReadings.take(5).any { item ->
            if (item.type.contains("pressure", ignoreCase = true)) {
                val sys = item.value.split("/").firstOrNull()?.trim()?.toIntOrNull() ?: 120
                sys >= 140
            } else if (item.type.contains("sugar", ignoreCase = true) || item.type.contains("glucose", ignoreCase = true)) {
                val s = item.value.toDoubleOrNull() ?: 5.5
                s >= 11.1 || s < 3.9
            } else false
        }
        bannerCritical?.visibility = if (hasCritical) View.VISIBLE else View.GONE
        btnBannerFindClinic?.setOnClickListener { showScreen(4) }

        tvSubtitle?.text = "${allReadings.size} entries recorded · syncs with all pages"
        btnLogNew?.setOnClickListener { showScreen(1) }

        chipAll?.setOnClickListener { applyFilter("All") }
        chipSugar?.setOnClickListener { applyFilter("Sugar") }
        chipPressure?.setOnClickListener { applyFilter("Pressure") }
        chipSymptoms?.setOnClickListener { applyFilter("Symptoms") }

        scope.launch {
            viewModel.healthReadings.collectLatest { list ->
                fullHistoryAdapter.updateData(list)
                applyFilter("All")
            }
        }

        // Start with All logs
        applyFilter("All")
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

    // --- 6. Clinics View Setup ---
    fun setupClinicsView(view: View) {
        val etSearch = view.findViewById<EditText>(R.id.et_search_clinics)
        val btnSearch = view.findViewById<Button>(R.id.btn_search_online_clinic)
        val tvHint = view.findViewById<TextView>(R.id.tv_search_status_hint)
        val btnAddCustom = view.findViewById<Button>(R.id.btn_add_custom_clinic)
        val scrollClinic = view.findViewById<View>(R.id.scroll_clinic_locator)
        val btnCenterUser = view.findViewById<Button>(R.id.btn_map_center_user)
        val webView = view.findViewById<android.webkit.WebView>(R.id.webview_clinic_map)
        val layoutLoading = view.findViewById<LinearLayout>(R.id.layout_map_loading)

        val routeCard = view.findViewById<LinearLayout>(R.id.layout_in_app_route_card)
        val tvRouteName = view.findViewById<TextView>(R.id.tv_route_clinic_name)
        val btnClearRoute = view.findViewById<TextView>(R.id.btn_clear_map_route)
        val tvRouteDetails = view.findViewById<TextView>(R.id.tv_route_details)
        val tvRouteAddress = view.findViewById<TextView>(R.id.tv_route_address)
        val btnRouteCall = view.findViewById<Button>(R.id.btn_route_call_direct)

        val tvCount = view.findViewById<TextView>(R.id.tv_clinics_count_badge)
        val rvClinics = view.findViewById<RecyclerView>(R.id.rv_clinics_list)

        // Store active references for in-app routing
        activeClinicWebView = webView
        activeRouteCard = routeCard
        activeTvRouteClinicName = tvRouteName
        activeTvRouteDetails = tvRouteDetails
        activeTvRouteAddress = tvRouteAddress
        activeBtnRouteCall = btnRouteCall
        activeClinicScrollView = scrollClinic

        // Setup Adapter with In-App Route Click
        val adapter = ClinicXmlAdapter(
            onCallClick = { clinic ->
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${clinic.phone.ifBlank { "911" }}"))
                activity.startActivity(intent)
            },
            onDirectionsClick = { clinic ->
                routeToClinicInApp(clinic)
            }
        )

        rvClinics?.layoutManager = LinearLayoutManager(activity)
        rvClinics?.adapter = adapter

        // Setup MapTiler WebView
        if (webView != null) {
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true

            webView.addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun onMapReady() {
                    activity.runOnUiThread {
                        layoutLoading?.visibility = View.GONE
                        sendClinicsToMap(webView, viewModel.clinics.value)
                    }
                }

                @android.webkit.JavascriptInterface
                fun onClinicClicked(id: Long, name: String, lat: Double, lng: Double) {
                    activity.runOnUiThread {
                        val match = viewModel.clinics.value.firstOrNull { it.id == id }
                        if (match != null) {
                            routeToClinicInApp(match)
                        }
                    }
                }

                @android.webkit.JavascriptInterface
                fun onClinicRouted(id: Long, name: String) {
                    activity.runOnUiThread {
                        val match = viewModel.clinics.value.firstOrNull { it.id == id }
                        if (match != null) {
                            routeToClinicInApp(match)
                        }
                    }
                }
            }, "AndroidBridge")

            webView.loadUrl("file:///android_asset/maptiler_map.html")
        }

        btnCenterUser?.setOnClickListener {
            webView?.evaluateJavascript("centerUser()", null)
        }

        btnClearRoute?.setOnClickListener {
            routeCard?.visibility = View.GONE
            webView?.evaluateJavascript("clearRoute()", null)
        }

        btnSearch?.setOnClickListener {
            etSearch?.let { performClinicSearch(it.text.toString(), tvHint) }
        }

        btnAddCustom?.setOnClickListener {
            showAddCustomClinicDialog()
        }

        scope.launch {
            viewModel.clinics.collectLatest { list ->
                adapter.updateData(list)
                tvCount?.text = "${list.size} Facilities Available"
                if (webView != null) {
                    sendClinicsToMap(webView, list)
                }
            }
        }
    }

    private fun sendClinicsToMap(webView: android.webkit.WebView, clinics: List<ClinicEntity>) {
        val array = org.json.JSONArray()
        clinics.forEach { c ->
            val obj = org.json.JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("address", c.address)
            obj.put("distance", String.format(java.util.Locale.US, "%.1f", c.distanceKm))
            obj.put("hours", c.openHours)
            obj.put("lat", c.latitude ?: (-25.7479 + (c.distanceKm * 0.008)))
            obj.put("lng", c.longitude ?: (28.2293 + (c.distanceKm * 0.008)))
            array.put(obj)
        }
        val jsonStr = array.toString().replace("'", "\\'")
        webView.evaluateJavascript("loadClinics('$jsonStr')", null)
    }

    // --- 7. Settings View Setup ---
    fun setupSettingsView(view: View) {
        val tvAvatar = view.findViewById<TextView>(R.id.tv_settings_avatar)
        val tvName = view.findViewById<TextView>(R.id.tv_settings_name)
        val tvEmail = view.findViewById<TextView>(R.id.tv_settings_email)

        val spinnerLang = view.findViewById<android.widget.Spinner>(R.id.spinner_language)
        val tvActiveLang = view.findViewById<TextView>(R.id.tv_active_language)

        val switchDarkMode = view.findViewById<android.widget.Switch>(R.id.switch_dark_mode)
        val switchReminders = view.findViewById<android.widget.Switch>(R.id.switch_reminders)

        val btnCallEmergency = view.findViewById<Button>(R.id.btn_call_emergency_contact)
        val btnCallDoctor = view.findViewById<Button>(R.id.btn_call_doctor)
        val btnSignOut = view.findViewById<Button>(R.id.btn_sign_out)

        // Observe user session
        scope.launch {
            viewModel.userSession.collectLatest { session ->
                val fullName = session?.fullName ?: "Lerato Mokoena"
                val email = session?.email ?: "lerato.mokoena@dosemate.app"
                val lang = session?.preferredLanguage ?: "English"

                tvName?.text = fullName
                tvEmail?.text = email
                tvActiveLang?.text = "$lang (South Africa)"

                val initials = fullName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
                tvAvatar?.text = initials.ifBlank { "LM" }

                switchDarkMode?.isChecked = session?.darkThemeEnabled ?: true
                switchReminders?.isChecked = session?.medicationRemindersEnabled ?: true
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

        switchDarkMode?.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleDarkTheme(isChecked)
        }

        switchReminders?.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleReminders(isChecked)
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
}
