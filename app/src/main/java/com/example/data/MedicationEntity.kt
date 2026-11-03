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
    val createdTimestamp: Long = System.currentTimeMillis(),
    val isPendingSync: Boolean = true,

    // --- Refill tracking (Feature 4) ---
    // Number of tablets currently left in the pack. Decremented automatically
    // every time a dose is marked as taken.
    val pillsRemaining: Int = 0,
    // Size of a full pack, used to work out the percentage left.
    val packSize: Int = 0,
    // Warn the patient once the remaining count drops to this number.
    val refillThreshold: Int = 5
)

@Entity(tableName = "dose_logs")
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val medicationName: String,
    val scheduledTime: String,
    val takenTime: String? = null,
    val status: String = "Pending", // "Taken", "Missed", "Pending"
    val date: String, // e.g., "2026-08-07"
    val isPendingSync: Boolean = true
)