package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosage: String,
    val frequency: String, // e.g., "Once", "Twice", "3x daily"
    val reminderTimes: String, // e.g. "7:00 AM, 7:00 PM"
    val status: String = "Active", // "Active", "Ended"
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "dose_logs")
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val medicationName: String,
    val scheduledTime: String,
    val takenTime: String? = null,
    val status: String = "Pending", // "Taken", "Missed", "Pending"
    val date: String // e.g., "2026-08-07"
)
