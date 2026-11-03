package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.DoseLogEntity
import com.example.data.HealthReadingEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Feature 5: exports the patient's health history to a CSV file.
 *
 * The file is written to the app's private cache directory and shared through
 * FileProvider, so no storage permission is required. The resulting file opens
 * in any spreadsheet application and can be emailed to a doctor.
 */
object HealthDataExporter {

    private const val TAG = "HealthDataExporter"
    private const val EXPORT_DIR = "exports"

    /**
     * Escapes a single CSV field.
     *
     * Any value containing a comma, a quote or a line break must be wrapped in
     * quotes, and embedded quotes must be doubled, or the columns shift.
     */
    private fun escape(value: String?): String {
        val raw = value ?: ""
        val needsQuotes = raw.contains(",") || raw.contains("\"") || raw.contains("\n") || raw.contains("\r")
        val escaped = raw.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    /**
     * Builds the CSV text from the readings and dose logs.
     *
     * Returns the file contents as a string so it can be unit tested without
     * touching the file system.
     */
    fun buildCsv(
        patientName: String,
        readings: List<HealthReadingEntity>,
        doseLogs: List<DoseLogEntity>
    ): String {
        val builder = StringBuilder()
        val exportedOn = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        // Report header
        builder.append("DoseMate Health History Export\n")
        builder.append("Patient,${escape(patientName)}\n")
        builder.append("Exported,$exportedOn\n")
        builder.append("\n")

        // Section 1: health readings
        builder.append("HEALTH READINGS\n")
        builder.append("Type,Value,Unit,When Taken,Notes,Recorded\n")
        if (readings.isEmpty()) {
            builder.append("No readings recorded\n")
        } else {
            for (r in readings) {
                builder.append(escape(r.type)).append(",")
                builder.append(escape(r.value)).append(",")
                builder.append(escape(r.unit)).append(",")
                builder.append(escape(r.whenTaken)).append(",")
                builder.append(escape(r.notes)).append(",")
                builder.append(escape(r.displayTime)).append("\n")
            }
        }

        builder.append("\n")

        // Section 2: medication adherence
        builder.append("MEDICATION ADHERENCE\n")
        builder.append("Medication,Scheduled Time,Status,Taken At,Date\n")
        if (doseLogs.isEmpty()) {
            builder.append("No doses logged\n")
        } else {
            for (d in doseLogs) {
                builder.append(escape(d.medicationName)).append(",")
                builder.append(escape(d.scheduledTime)).append(",")
                builder.append(escape(d.status)).append(",")
                builder.append(escape(d.takenTime ?: "-")).append(",")
                builder.append(escape(d.date)).append("\n")
            }
        }

        builder.append("\n")

        // Section 3: adherence summary
        val taken = doseLogs.count { it.status.equals("Taken", ignoreCase = true) }
        val total = doseLogs.size
        val percent = if (total > 0) (taken * 100) / total else 0
        builder.append("SUMMARY\n")
        builder.append("Total doses logged,$total\n")
        builder.append("Doses taken,$taken\n")
        builder.append("Adherence,$percent%\n")

        return builder.toString()
    }

    /**
     * Writes the CSV to the cache directory and returns the file.
     *
     * Returns null when nothing could be written, so the caller can show an
     * error rather than opening an empty share sheet.
     */
    fun writeCsvFile(
        context: Context,
        patientName: String,
        readings: List<HealthReadingEntity>,
        doseLogs: List<DoseLogEntity>
    ): File? {
        return try {
            val dir = File(context.cacheDir, EXPORT_DIR)
            if (!dir.exists()) dir.mkdirs()

            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val file = File(dir, "DoseMate_Health_History_$stamp.csv")

            file.writeText(buildCsv(patientName, readings, doseLogs))
            Log.d(TAG, "Exported ${readings.size} readings and ${doseLogs.size} dose logs to ${file.name}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write CSV export: ${e.message}", e)
            null
        }
    }

    /**
     * Opens the Android share sheet so the CSV can be emailed or saved.
     */
    fun shareCsvFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "DoseMate Health History")
            putExtra(
                Intent.EXTRA_TEXT,
                "Attached is my DoseMate health history export, including blood sugar and blood pressure readings plus medication adherence."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share health history"))
        Log.d(TAG, "Share sheet opened for ${file.name}")
    }
}