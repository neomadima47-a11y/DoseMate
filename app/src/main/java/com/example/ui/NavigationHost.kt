package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TealPrimary
import com.example.util.Strings

enum class ScreenRoute {
    AUTH,
    DASHBOARD,
    MEDICATIONS,
    LOG_READING,
    HEALTH_LOG,
    CLINIC_LOCATOR,
    SETTINGS
}

@Composable
fun MainAppLayout(viewModel: MainViewModel) {
    val userSession by viewModel.userSession.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val doseLogs by viewModel.doseLogs.collectAsState()
    val healthReadings by viewModel.healthReadings.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncSuccessMessage by viewModel.syncSuccessMessage.collectAsState()
    val clinics by viewModel.clinics.collectAsState()
    val liveClinics by viewModel.liveClinics.collectAsState()
    val isSearchingClinics by viewModel.isSearchingClinics.collectAsState()
    val clinicSearchError by viewModel.clinicSearchError.collectAsState()
    val serpApiKey by viewModel.serpApiKey.collectAsState()
    val activeSearchQuery by viewModel.activeSearchQuery.collectAsState()
    val isVerifyingDrug by viewModel.isVerifyingDrug.collectAsState()
    val drugVerification by viewModel.drugVerification.collectAsState()
    val drugInteractions by viewModel.drugInteractions.collectAsState()
    val healthConnectStatus by viewModel.healthConnectStatus.collectAsState()
    val healthConnectSyncMsg by viewModel.healthConnectSyncMsg.collectAsState()

    val isLoggedIn = userSession?.isLoggedIn ?: true
    val currentLanguage = userSession?.preferredLanguage ?: "English"

    var currentScreen by remember { mutableStateOf(if (isLoggedIn) ScreenRoute.DASHBOARD else ScreenRoute.AUTH) }

    if (!isLoggedIn || currentScreen == ScreenRoute.AUTH) {
        AuthScreen(
            currentLanguage = currentLanguage,
            onLoginSuccess = { email, name, language ->
                viewModel.login(email, name, language)
                currentScreen = ScreenRoute.DASHBOARD
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val navItems = listOf(
                        Triple(ScreenRoute.DASHBOARD, Icons.Default.Home, "dashboard"),
                        Triple(ScreenRoute.MEDICATIONS, Icons.Default.MedicalServices, "medications"),
                        Triple(ScreenRoute.LOG_READING, Icons.Default.AddCircle, "log_reading"),
                        Triple(ScreenRoute.HEALTH_LOG, Icons.AutoMirrored.Filled.ShowChart, "health_log"),
                        Triple(ScreenRoute.CLINIC_LOCATOR, Icons.Default.LocationOn, "find_clinic"),
                        Triple(ScreenRoute.SETTINGS, Icons.Default.Settings, "settings")
                    )

                    navItems.forEach { (route, icon, labelKey) ->
                        val isSelected = (currentScreen == route)
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = route },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = labelKey,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = Strings.get(labelKey, currentLanguage),
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TealPrimary,
                                selectedTextColor = TealPrimary,
                                indicatorColor = TealPrimary.copy(alpha = 0.15f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_item_${labelKey}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    ScreenRoute.DASHBOARD -> DashboardScreen(
                        userName = userSession?.fullName ?: "Lerato Mokoena",
                        language = currentLanguage,
                        doseLogs = doseLogs,
                        healthReadings = healthReadings,
                        pendingSyncCount = pendingSyncCount,
                        isSyncing = isSyncing,
                        syncSuccessMessage = syncSuccessMessage,
                        onMarkTaken = { viewModel.markDoseTaken(it) },
                        onNavigateToLogReading = { currentScreen = ScreenRoute.LOG_READING },
                        onNavigateToHealthLog = { currentScreen = ScreenRoute.HEALTH_LOG },
                        onNavigateToMedications = { currentScreen = ScreenRoute.MEDICATIONS },
                        onNavigateToClinicLocator = { currentScreen = ScreenRoute.CLINIC_LOCATOR },
                        onNavigateToSettings = { currentScreen = ScreenRoute.SETTINGS },
                        onSyncNow = { viewModel.syncNow() }
                    )

                    ScreenRoute.MEDICATIONS -> MedicationsScreen(
                        language = currentLanguage,
                        medications = medications,
                        doseLogs = doseLogs,
                        healthReadings = healthReadings,
                        isVerifyingDrug = isVerifyingDrug,
                        drugVerification = drugVerification,
                        drugInteractions = drugInteractions,
                        onVerifyDrug = { viewModel.verifyMedicationName(it) },
                        onClearDrugVerification = { viewModel.clearDrugVerification() },
                        onAddMedication = { name, dosage, frequency, times ->
                            viewModel.addMedication(name, dosage, frequency, times)
                        },
                        onMarkDoseTaken = { viewModel.markDoseTaken(it) },
                        onDeleteMedication = { viewModel.deleteMedication(it) },
                        onToggleMedicationStatus = { id, status -> viewModel.updateMedicationStatus(id, status) },
                        onNavigateToDashboard = { currentScreen = ScreenRoute.DASHBOARD },
                        onNavigateToLogReading = { currentScreen = ScreenRoute.LOG_READING },
                        onNavigateToHealthLog = { currentScreen = ScreenRoute.HEALTH_LOG },
                        onNavigateToClinicLocator = { currentScreen = ScreenRoute.CLINIC_LOCATOR },
                        onNavigateToSettings = { currentScreen = ScreenRoute.SETTINGS }
                    )

                    ScreenRoute.LOG_READING -> LogReadingScreen(
                        language = currentLanguage,
                        onSaveReading = { type, value, unit, whenTaken, notes ->
                            viewModel.addHealthReading(type, value, unit, whenTaken, notes)
                        },
                        onReadingSavedNav = { currentScreen = ScreenRoute.HEALTH_LOG },
                        onNavigateToDashboard = { currentScreen = ScreenRoute.DASHBOARD },
                        onNavigateToMedications = { currentScreen = ScreenRoute.MEDICATIONS },
                        onNavigateToClinics = { currentScreen = ScreenRoute.CLINIC_LOCATOR }
                    )

                    ScreenRoute.HEALTH_LOG -> HealthHistoryScreen(
                        language = currentLanguage,
                        readings = healthReadings,
                        onNavigateToLogReading = { currentScreen = ScreenRoute.LOG_READING },
                        onNavigateToMedications = { currentScreen = ScreenRoute.MEDICATIONS },
                        onNavigateToClinicLocator = { currentScreen = ScreenRoute.CLINIC_LOCATOR },
                        onDeleteReading = { viewModel.deleteHealthReading(it) },
                        onSyncNow = { viewModel.syncNow() },
                        isSyncing = isSyncing,
                        pendingSyncCount = pendingSyncCount
                    )

                    ScreenRoute.CLINIC_LOCATOR -> ClinicLocatorScreen(
                        language = currentLanguage,
                        clinics = clinics,
                        liveClinics = liveClinics,
                        isSearchingClinics = isSearchingClinics,
                        clinicSearchError = clinicSearchError,
                        serpApiKey = serpApiKey,
                        activeSearchQuery = activeSearchQuery,
                        healthReadings = healthReadings,
                        medications = medications,
                        onSearchClinics = { q, key -> viewModel.searchClinicsGoogleMaps(q, key) },
                        onClearSearch = { viewModel.clearLiveClinicsSearch() },
                        onSetSerpApiKey = { viewModel.setSerpApiKey(it) },
                        onNavigateToLogReading = { currentScreen = ScreenRoute.LOG_READING },
                        onNavigateToHealthLog = { currentScreen = ScreenRoute.HEALTH_LOG },
                        onNavigateToMedications = { currentScreen = ScreenRoute.MEDICATIONS }
                    )

                    ScreenRoute.SETTINGS -> SettingsScreen(
                        userSession = userSession,
                        onUpdateLanguage = { viewModel.updateLanguage(it) },
                        onToggleDarkTheme = { viewModel.toggleDarkTheme(it) },
                        onToggleReminders = { viewModel.toggleReminders(it) },
                        onSyncNow = { viewModel.syncNow() },
                        onLogout = {
                            viewModel.logout()
                            currentScreen = ScreenRoute.AUTH
                        },
                        healthConnectManager = viewModel.healthConnectManager,
                        healthConnectStatus = healthConnectStatus,
                        healthConnectSyncMsg = healthConnectSyncMsg,
                        onSyncToHealthConnect = { viewModel.syncToHealthConnect() }
                    )

                    else -> {}
                }
            }
        }
    }
}
