package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_readings")
data class HealthReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "Blood sugar", "Blood pressure", "Symptom"
    val value: String, // "7.2" or "120/80" or "Mild dizziness"
    val unit: String, // "mmol/L", "mmHg", ""
    val whenTaken: String, // "Before breakfast", "After dinner", "Bedtime", etc.
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val displayTime: String, // e.g. "Today, 6:45am"
    val isPendingSync: Boolean = true,
    val hasConflict: Boolean = false
)
