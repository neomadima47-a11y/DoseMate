package com.example

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ui.MainAppLayout
import com.example.ui.MainViewModel
import com.example.ui.XmlUiController
import com.example.ui.theme.HealthBridgeTheme

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private var useXmlEngine by mutableStateOf(true) // Native XML Engine active by default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationAndAlarmPermissions()
        renderEngine()
    }

    private fun requestNotificationAndAlarmPermissions() {
        // Request POST_NOTIFICATIONS runtime permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Request SCHEDULE_EXACT_ALARM permission for Android 12+ (API 31+) if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }

    private fun renderEngine() {
        if (useXmlEngine) {
            setContentView(R.layout.activity_main)
            val rootLayout = findViewById<ViewGroup>(android.R.id.content)
            val xmlController = XmlUiController(
                activity = this,
                rootContainer = rootLayout,
                viewModel = mainViewModel,
                onToggleEngine = {
                    useXmlEngine = false
                    renderEngine()
                }
            )
            xmlController.start()
        } else {
            setContent {
                val userSession by mainViewModel.userSession.collectAsState()
                val darkTheme = userSession?.darkThemeEnabled ?: true

                HealthBridgeTheme(darkTheme = darkTheme) {
                    MainAppLayout(
                        viewModel = mainViewModel,
                        onSwitchToXmlEngine = {
                            useXmlEngine = true
                            renderEngine()
                        }
                    )
                }
            }
        }
    }
}
