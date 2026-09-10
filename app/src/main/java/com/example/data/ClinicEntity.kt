package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clinics")
data class ClinicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val distanceKm: Double,
    val openHours: String,
    val address: String,
    val phone: String,
    val isNearest: Boolean = false,
    val latOffsetDp: Float = 0f,
    val lngOffsetDp: Float = 0f
)
