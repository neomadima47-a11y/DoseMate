package com.example

import android.app.Application
import com.example.notifications.MedicationNotificationHelper
import com.mapbox.common.MapboxOptions

class DoseMateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Mapbox token globally as required by v11 SDK
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN

        // Initialize notification channel for medication reminders
        MedicationNotificationHelper.createNotificationChannel(this)
    }
}
