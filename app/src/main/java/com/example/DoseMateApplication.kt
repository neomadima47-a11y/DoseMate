package com.example

import android.app.Application
import android.util.Log
import com.example.notifications.MedicationNotificationHelper
import com.mapbox.common.MapboxOptions

class DoseMateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Safely initialize Mapbox token without crashing app if token is missing or invalid
        try {
            val token = try { BuildConfig.MAPBOX_ACCESS_TOKEN } catch (_: Throwable) { "" }
            if (!token.isNullOrBlank()) {
                MapboxOptions.accessToken = token
            }
        } catch (e: Throwable) {
            Log.w("DoseMateApplication", "Mapbox token initialization skipped: ${e.message}")
        }

        // Initialize notification channel for medication reminders
        try {
            MedicationNotificationHelper.createNotificationChannel(this)
        } catch (e: Throwable) {
            Log.e("DoseMateApplication", "Failed to create notification channel", e)
        }
    }
}
