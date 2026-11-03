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
    val lngOffsetDp: Float = 0f,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class DisplayClinic(
    val id: String,
    val name: String,
    val distanceKm: Double,
    val openHours: String,
    val address: String,
    val phone: String,
    val isNearest: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
