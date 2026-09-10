# DoseMate

**DoseMate** is a self-contained mobile health management application built for Android. It enables users to manage chronic health conditions, track medication schedules, log daily health readings, and locate nearby health clinics—all with local offline data persistence.

---

## 🌟 Key Features

- 💊 **Medication Tracker**: Schedule and manage daily medications, dosage amounts, frequencies, and track adherence history.
- 📊 **Health Vitals Logging**: Record essential health metrics including Blood Pressure, Blood Glucose, Heart Rate, Weight, Temperature, and Symptoms with timestamped notes.
- 📈 **Health History & Analytics**: View logs, filter by metric type, and monitor historical trends.
- 🏥 **Clinic Locator**: Browse and find local healthcare clinics and medical centers.
- 🔒 **Local & Secure Auth**: Protect personal health information with local PIN or credentials.
- 💾 **Offline-First Persistence**: Powered by Android's Room database to ensure all health logs and schedules remain safely stored on your device.
- 🎨 **Modern Material 3 Design**: Built using Jetpack Compose with custom dynamic colors, dark theme support, and edge-to-edge layouts.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Data Layer
- **Local Persistence**: Room Database (SQLite abstraction)
- **Async & Reactive Flow**: Kotlin Coroutines & `StateFlow`
- **Navigation**: Type-safe Jetpack Navigation Compose
- **Testing**: Robolectric & Roborazzi screenshot testing support

---

## 📱 Screen Overview

1. **Dashboard (`DashboardScreen`)**: Direct access to upcoming medications, health stats summary, and quick action logs.
2. **Medications (`MedicationScreen`)**: Comprehensive list and schedule for active medications and daily dose intake tracking.
3. **Log Reading (`LogReadingScreen`)**: Dedicated input form for capturing vitals with automatic unit suggestions and timestamp logging.
4. **Health Log History (`HealthHistoryScreen`)**: Interactive log feed with chart visualizers and metric filters.
5. **Clinic Locator (`ClinicLocatorScreen`)**: Directory of medical centers and emergency contacts.
6. **Settings (`SettingsScreen`)**: App preferences, local data management, language selection, and theme toggles.

---

## 🚀 Getting Started & Android Studio Setup

### Opening in Android Studio

1. **Download or Clone**:
   - **Download as ZIP**: In Google AI Studio, click the project menu (or download icon) and select **Download as ZIP**, then extract the ZIP file onto your computer.
   - **GitHub**: Clone your repository to your local computer:
     ```bash
     git clone <your-repository-url>
     ```
2. **Open the Project**:
   - Launch **Android Studio** (Ladybug, Koala, Meerkat, or newer recommended).
   - Select **Open** (or **File > Open...**).
   - Navigate to the extracted project folder (the folder containing `build.gradle.kts` and `settings.gradle.kts`) and click **OK**.
3. **Gradle Sync & JDK**:
   - The project includes the official **Gradle Wrapper** (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`), so Android Studio will automatically download the correct Gradle runtime.
   - Ensure your Gradle JDK is set to **JDK 17 or JDK 21**:
     - Go to: **Settings / Preferences > Build, Execution, Deployment > Build Tools > Gradle**.
     - Set **Gradle JDK** to **JDK 21** or **JDK 17** (or standard Android Studio embedded Java).
   - Let the initial Gradle project sync finish.
4. **Run the App**:
   - Connect an Android device with USB debugging enabled or launch an Android Virtual Device (AVD emulator).
   - Click the green **Run (Play)** button or press `Shift + F10`.

---

## 🔧 GitHub Push Troubleshooting

If you encounter `Failed to push commit to GitHub: Request contains an invalid argument`:

1. **Repository Name Format**:
   - When AI Studio asks for the repository name, enter **only a plain name** with letters, numbers, hyphens, or underscores (e.g. `DoseMate` or `dosemate-android`).
   - ❌ **Do NOT** include spaces (e.g. `Dose Mate Android`).
   - ❌ **Do NOT** paste full URLs (e.g. `https://github.com/...`).
   - ❌ **Do NOT** include special characters like `@`, `!`, `#`, `(`, `)`.
2. **Excluded Build Artifacts**:
   - The `.gitignore` has been configured to exclude heavy compiled files (`.build-outputs/`, `*.apk`, `build/`, `debug.keystore`), preventing payload size limits during GitHub API exports.

---

### Building the Project via Terminal

```bash
# On macOS / Linux:
./gradlew assembleDebug

# On Windows:
gradlew.bat assembleDebug
```

The compiled APK file will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License

This project is open-sourced under the MIT License.
