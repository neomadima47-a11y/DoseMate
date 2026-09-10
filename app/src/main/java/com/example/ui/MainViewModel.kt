package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DoseLogEntity
import com.example.data.HealthBridgeDatabase
import com.example.data.HealthBridgeRepository
import com.example.data.HealthReadingEntity
import com.example.data.MedicationEntity
import com.example.data.OpenFdaDrugClient
import com.example.data.DrugVerificationResult
import com.example.data.DrugInteractionAlert
import com.example.data.HealthConnectManager
import com.example.data.SerpApiGoogleMapsClient
import com.example.data.SerpApiPlace
import com.example.data.SerpApiSearchResult
import com.example.data.UserSessionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: HealthBridgeRepository

    init {
        val db = HealthBridgeDatabase.getDatabase(application)
        repository = HealthBridgeRepository(
            db.userDao(),
            db.medicationDao(),
            db.healthReadingDao(),
            db.clinicDao()
        )
        viewModelScope.launch {
            repository.initializeDefaultDataIfEmpty()
        }
    }

    val userSession: StateFlow<UserSessionEntity?> = repository.userSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val medications: StateFlow<List<MedicationEntity>> = repository.medications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val doseLogs: StateFlow<List<DoseLogEntity>> = repository.doseLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val healthReadings: StateFlow<List<HealthReadingEntity>> = repository.healthReadings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val clinics = repository.clinics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val serpApiClient = SerpApiGoogleMapsClient()

    private val _isSearchingClinics = MutableStateFlow(false)
    val isSearchingClinics: StateFlow<Boolean> = _isSearchingClinics.asStateFlow()

    private val _liveClinics = MutableStateFlow<List<SerpApiPlace>>(emptyList())
    val liveClinics: StateFlow<List<SerpApiPlace>> = _liveClinics.asStateFlow()

    private val _clinicSearchError = MutableStateFlow<String?>(null)
    val clinicSearchError: StateFlow<String?> = _clinicSearchError.asStateFlow()

    private val _serpApiKey = MutableStateFlow(serpApiClient.getInjectedApiKey())
    val serpApiKey: StateFlow<String> = _serpApiKey.asStateFlow()

    private val _activeSearchQuery = MutableStateFlow("")
    val activeSearchQuery: StateFlow<String> = _activeSearchQuery.asStateFlow()

    fun setSerpApiKey(key: String) {
        _serpApiKey.value = key.trim()
    }

    fun searchClinicsGoogleMaps(query: String, customApiKey: String? = null) {
        val effectiveKey = customApiKey?.trim()?.ifBlank { null } ?: _serpApiKey.value
        _activeSearchQuery.value = query
        viewModelScope.launch {
            _isSearchingClinics.value = true
            _clinicSearchError.value = null
            val result = serpApiClient.searchGoogleMaps(query, customApiKey = effectiveKey)
            when (result) {
                is SerpApiSearchResult.Success -> {
                    _liveClinics.value = result.places
                    if (result.places.isEmpty()) {
                        _clinicSearchError.value = "No clinics found on Google Maps for '$query'. Try another location or keyword."
                    } else {
                        // Persist to local Room database for offline accessibility
                        val entities = result.places.mapIndexed { idx, p -> p.toClinicEntity(idx) }
                        repository.addClinics(entities)
                    }
                }
                is SerpApiSearchResult.Error -> {
                    _clinicSearchError.value = result.message
                }
            }
            _isSearchingClinics.value = false
        }
    }

    fun clearLiveClinicsSearch() {
        _liveClinics.value = emptyList()
        _activeSearchQuery.value = ""
        _clinicSearchError.value = null
    }

    private val openFdaClient = OpenFdaDrugClient()

    private val _isVerifyingDrug = MutableStateFlow(false)
    val isVerifyingDrug: StateFlow<Boolean> = _isVerifyingDrug.asStateFlow()

    private val _drugVerification = MutableStateFlow<DrugVerificationResult?>(null)
    val drugVerification: StateFlow<DrugVerificationResult?> = _drugVerification.asStateFlow()

    private val _drugInteractions = MutableStateFlow<List<DrugInteractionAlert>>(emptyList())
    val drugInteractions: StateFlow<List<DrugInteractionAlert>> = _drugInteractions.asStateFlow()

    fun verifyMedicationName(query: String) {
        if (query.trim().length < 3) {
            _drugVerification.value = null
            _drugInteractions.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isVerifyingDrug.value = true
            val result = openFdaClient.verifyDrug(query)
            _drugVerification.value = result

            // Also check for potential interactions against existing active medications
            val activeNames = medications.value.filter { it.status == "Active" }.map { it.name }
            val alerts = openFdaClient.checkInteraction(query, activeNames)
            _drugInteractions.value = alerts
            _isVerifyingDrug.value = false
        }
    }

    fun clearDrugVerification() {
        _drugVerification.value = null
        _drugInteractions.value = emptyList()
    }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncSuccessMessage = MutableStateFlow<String?>(null)
    val syncSuccessMessage: StateFlow<String?> = _syncSuccessMessage.asStateFlow()

    fun markDoseTaken(doseLogId: Long) {
        viewModelScope.launch {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val currentTimeStr = timeFormat.format(Date())
            repository.markDoseTaken(doseLogId, currentTimeStr)
        }
    }

    fun addMedication(name: String, dosage: String, frequency: String, reminderTimes: String) {
        viewModelScope.launch {
            repository.addMedication(name, dosage, frequency, reminderTimes)
        }
    }

    fun deleteMedication(id: Long) {
        viewModelScope.launch {
            repository.deleteMedication(id)
        }
    }

    fun updateMedicationStatus(id: Long, status: String) {
        viewModelScope.launch {
            repository.updateMedicationStatus(id, status)
        }
    }

    val healthConnectManager = HealthConnectManager(application)

    private val _healthConnectStatus = MutableStateFlow(
        if (healthConnectManager.isAvailable) "Ready (On-device)" else "Not Installed"
    )
    val healthConnectStatus: StateFlow<String> = _healthConnectStatus.asStateFlow()

    private val _healthConnectSyncMsg = MutableStateFlow<String?>(null)
    val healthConnectSyncMsg: StateFlow<String?> = _healthConnectSyncMsg.asStateFlow()

    fun addHealthReading(type: String, value: String, unit: String, whenTaken: String, notes: String) {
        viewModelScope.launch {
            repository.addHealthReading(type, value, unit, whenTaken, notes)
            // Option 2: Sync to on-device Android Health Connect (free & private)
            try {
                if (healthConnectManager.isAvailable) {
                    when {
                        type.contains("pressure", ignoreCase = true) -> {
                            val parts = value.split("/")
                            if (parts.size == 2) {
                                val sys = parts[0].trim().toDoubleOrNull()
                                val dia = parts[1].trim().toDoubleOrNull()
                                if (sys != null && dia != null) {
                                    healthConnectManager.writeBloodPressure(sys, dia)
                                }
                            }
                        }
                        type.contains("sugar", ignoreCase = true) || type.contains("glucose", ignoreCase = true) -> {
                            val glucose = value.trim().toDoubleOrNull()
                            if (glucose != null) {
                                healthConnectManager.writeBloodGlucose(glucose)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently ignore if permissions are not yet authorized
            }
        }
    }

    fun deleteHealthReading(id: Long) {
        viewModelScope.launch {
            repository.deleteHealthReading(id)
        }
    }

    fun syncToHealthConnect() {
        viewModelScope.launch {
            if (!healthConnectManager.isAvailable) {
                _healthConnectSyncMsg.value = "Health Connect is not available on this device."
                kotlinx.coroutines.delay(3000)
                _healthConnectSyncMsg.value = null
                return@launch
            }

            val readings = healthReadings.value
            var syncedCount = 0
            for (reading in readings) {
                if (reading.type.contains("pressure", ignoreCase = true)) {
                    val parts = reading.value.split("/")
                    if (parts.size == 2) {
                        val sys = parts[0].trim().toDoubleOrNull()
                        val dia = parts[1].trim().toDoubleOrNull()
                        if (sys != null && dia != null) {
                            val result = healthConnectManager.writeBloodPressure(sys, dia)
                            if (result.isSuccess) syncedCount++
                        }
                    }
                } else if (reading.type.contains("sugar", ignoreCase = true) || reading.type.contains("glucose", ignoreCase = true)) {
                    val glucose = reading.value.trim().toDoubleOrNull()
                    if (glucose != null) {
                        val result = healthConnectManager.writeBloodGlucose(glucose)
                        if (result.isSuccess) syncedCount++
                    }
                }
            }
            _healthConnectSyncMsg.value = if (syncedCount > 0) {
                "Synced $syncedCount record(s) to Android Health Connect!"
            } else {
                "Health Connect sync complete (Check permissions in Settings)."
            }
            kotlinx.coroutines.delay(3500)
            _healthConnectSyncMsg.value = null
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            kotlinx.coroutines.delay(1200) // Realistic background sync animation
            repository.syncOfflineData()
            _isSyncing.value = false
            _syncSuccessMessage.value = "All offline health records synced successfully with server!"
            kotlinx.coroutines.delay(3000)
            _syncSuccessMessage.value = null
        }
    }

    fun login(email: String, name: String, language: String) {
        viewModelScope.launch {
            repository.login(email, name, language)
        }
    }

    fun register(email: String, name: String, language: String) {
        viewModelScope.launch {
            repository.login(email, name, language)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun updateLanguage(newLanguage: String) {
        viewModelScope.launch {
            val user = userSession.value ?: return@launch
            repository.updateUserProfile(
                fullName = user.fullName,
                email = user.email,
                preferredLanguage = newLanguage,
                reminders = user.medicationRemindersEnabled,
                darkTheme = user.darkThemeEnabled
            )
        }
    }

    fun toggleDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            val user = userSession.value ?: return@launch
            repository.updateUserProfile(
                fullName = user.fullName,
                email = user.email,
                preferredLanguage = user.preferredLanguage,
                reminders = user.medicationRemindersEnabled,
                darkTheme = enabled
            )
        }
    }

    fun toggleReminders(enabled: Boolean) {
        viewModelScope.launch {
            val user = userSession.value ?: return@launch
            repository.updateUserProfile(
                fullName = user.fullName,
                email = user.email,
                preferredLanguage = user.preferredLanguage,
                reminders = enabled,
                darkTheme = user.darkThemeEnabled
            )
        }
    }
}
