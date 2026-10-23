# DoseMate / HealthBridge 🩺

**DoseMate (HealthBridge)** is a modern, high-performance mobile health management and prescription companion app for Android. Built with a **dual-engine architecture** (100% Native Android XML + Jetpack Compose) in Kotlin, DoseMate empowers patients to track chronic disease regimens, receive exact high-priority alarm notifications, consult an on-device AI medication assistant, locate healthcare facilities with turn-by-turn road navigation, and auto-sync data seamlessly across offline Room SQLite storage and Firebase Cloud Firestore.

---

## 🌟 Key Features

- 💊 **Medication Schedule & Multi-Dose Alarms**: Schedule prescriptions once, twice, thrice, or 4 times daily with individual time pickers for every dose.
- 🔔 **Heads-Up Pop-Up System Alarms**: Powered by Android `AlarmManager.setAlarmClock(...)` with `NotificationCompat.PRIORITY_MAX`, real audio ringtone playback, vibration, and top-of-screen heads-up pop-up banners (works when app is OPEN, CLOSED, or in BACKGROUND).
- 📅 **Interactive Calendar Medication History**: Custom 7-column monthly calendar grid with **Green (`✓ Taken`)** and **Orange (`⏰ Not Taken`)** date cell highlights. Tap any date cell to inspect exact dose logs, medication types, and completion times.
- 🤖 **Groq Llama 3.3 70B AI Medication Assistant**: Dedicated conversational AI assistant to answer questions about drug side effects, drug-drug interactions, missed doses, and wellness guidance.
- 🏥 **MapTiler & OSRM Real-Road Routing**: Leaflet map with MapTiler Outdoor-v4 tiles, MapTiler Geocoding API search, and OSRM turn-by-turn road routing tracing actual street networks.
- 🔐 **Authentication & Security**: Google Sign-In via Credential Manager, 4-Digit Security PIN, and Firebase Password Reset emails.
- 🔄 **Automated Offline-First & Cloud Sync**: Room SQLite database persistence paired with real-time `NetworkConnectivityMonitor` and `AutoSyncWorker` background sync to Firebase Cloud Firestore.
- 🌐 **South African Multi-Language Support**: English, isiZulu, Sesotho, Afrikaans, Sepedi, Setswana, and isiXhosa.

---

## 🛠️ Architecture & Tech Stack

- **Language**: 100% Kotlin
- **UI Engine**: Dual-Engine (Native Android XML ViewControllers + Jetpack Compose Material 3)
- **Local Database**: Room SQLite Database (`HealthBridgeDatabase` v4)
- **Cloud Database**: Firebase Cloud Firestore (`users`, `health_readings`, `medications`, `dose_logs`)
- **Authentication**: Firebase Auth, Google Credential Manager (`GetSignInWithGoogleOption`)
- **Notifications**: AlarmManager, WorkManager (`AutoSyncWorker`), `NotificationCompat` Heads-Up Banners
- **AI Engine**: Groq Llama 3.3 70B LLM API
- **Map & Geocoding**: Leaflet.js, MapTiler Outdoor-v4 API, OSRM Routing Engine

---

## 📱 Screen Breakdown

1. **Sign In & Register ([fragment_auth.xml](file:///C:/Users/ASUS/StudioProjects/DoseMate/app/src/main/res/layout/fragment_auth.xml))**: Google Sign-In, 4-digit PIN authentication, language selector, and Firebase password reset.
2. **Dashboard / Home ([fragment_dashboard.xml](file:///C:/Users/ASUS/StudioProjects/DoseMate/app/src/main/res/layout/fragment_dashboard.xml))**: Adherence tracker progress bar, weather health tips, quick actions, and active daily doses.
3. **AI Medication Assistant ([fragment_log_reading.xml](file:///C:/Users/ASUS/StudioProjects/DoseMate/app/src/main/res/layout/fragment_log_reading.xml))**: Groq Llama 3.3 70B AI chat thread with quick inquiry chips.
4. **Medication Schedule ([fragment_medications.xml](file:///C:/Users/ASUS/StudioProjects/DoseMate/app/src/main/res/layout/fragment_medications.xml))**: Prescription regimen list, individual dose time pickers dialog, and dose reminder test launcher.
5. **Calendar History ([fragment_health_history.xml](file:///C:/Users/ASUS/StudioProjects/DoseMate/app/src/main/res/layout/fragment_health_history.xml))**: Interactive monthly calendar grid with Green (`✓ Taken`) and Orange (`⏰ Not Taken`) cell highlights.
6. **Clinic Locator ([fragment_clinic_locator.xml](file:///C:/Users/ASUS/StudioProjects/DoseMate/app/src/main/res/layout/fragment_clinic_locator.xml))**: Embedded MapTiler Outdoor map with OSRM turn-by-turn road navigation.
7. **Settings ([fragment_settings.xml](file:///C:/Users/ASUS/StudioProjects/DoseMate/app/src/main/res/layout/fragment_settings.xml))**: Patient profile, language preferences, dark mode toggle, emergency contacts, and account logout.

---

## 🚀 Building & Running the Project

```bash
# On Windows (PowerShell or CMD):
gradlew.bat assembleDebug

# On macOS or Linux:
./gradlew assembleDebug
```

The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License

This project is open-sourced under the MIT License.
