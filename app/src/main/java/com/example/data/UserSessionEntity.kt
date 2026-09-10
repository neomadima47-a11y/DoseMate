package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_session")
data class UserSessionEntity(
    @PrimaryKey val id: Int = 1,
    val fullName: String = "Lerato Mokoena",
    val email: String = "lerato@example.com",
    val preferredLanguage: String = "English",
    val isLoggedIn: Boolean = true,
    val medicationRemindersEnabled: Boolean = true,
    val syncAlertsEnabled: Boolean = true,
    val darkThemeEnabled: Boolean = true,
    val condition: String = "Diabetes & Hypertension"
)
