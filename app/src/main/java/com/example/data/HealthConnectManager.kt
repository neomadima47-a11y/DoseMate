package com.example.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Pressure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset

class HealthConnectManager(private val context: Context) {

    val sdkStatus: Int
        get() = HealthConnectClient.getSdkStatus(context)

    val isAvailable: Boolean
        get() = sdkStatus == HealthConnectClient.SDK_AVAILABLE

    val healthConnectClient: HealthConnectClient? by lazy {
        if (isAvailable) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getWritePermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(BloodGlucoseRecord::class),
        HealthPermission.getWritePermission(BloodGlucoseRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class)
    )

    suspend fun hasAllPermissions(): Boolean = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext false
        try {
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(permissions)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun writeBloodPressure(
        systolic: Double,
        diastolic: Double,
        time: Instant = Instant.now()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext Result.failure(
            IllegalStateException("Health Connect is not available on this device")
        )
        try {
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(time)
            val record = BloodPressureRecord(
                systolic = Pressure.millimetersOfMercury(systolic),
                diastolic = Pressure.millimetersOfMercury(diastolic),
                time = time,
                zoneOffset = zoneOffset,
                metadata = Metadata()
            )
            client.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeBloodGlucose(
        mgDlValue: Double,
        time: Instant = Instant.now()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext Result.failure(
            IllegalStateException("Health Connect is not available on this device")
        )
        try {
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(time)
            val record = BloodGlucoseRecord(
                level = BloodGlucose.milligramsPerDeciliter(mgDlValue),
                time = time,
                zoneOffset = zoneOffset,
                metadata = Metadata()
            )
            client.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeSteps(
        count: Long,
        startTime: Instant,
        endTime: Instant
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext Result.failure(
            IllegalStateException("Health Connect is not available on this device")
        )
        try {
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime)
            val stepsRecord = StepsRecord(
                count = count,
                startTime = startTime,
                endTime = endTime,
                startZoneOffset = zoneOffset,
                endZoneOffset = zoneOffset,
                metadata = Metadata()
            )
            client.insertRecords(listOf(stepsRecord))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readRecentBloodPressure(
        startTime: Instant,
        endTime: Instant = Instant.now()
    ): List<BloodPressureRecord> = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext emptyList()
        try {
            val request = ReadRecordsRequest(
                recordType = BloodPressureRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            client.readRecords(request).records
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun readRecentBloodGlucose(
        startTime: Instant,
        endTime: Instant = Instant.now()
    ): List<BloodGlucoseRecord> = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext emptyList()
        try {
            val request = ReadRecordsRequest(
                recordType = BloodGlucoseRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            client.readRecords(request).records
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun readStepsAggregate(
        startTime: Instant,
        endTime: Instant = Instant.now()
    ): Long = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext 0L
        try {
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response[StepsRecord.COUNT_TOTAL] ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
