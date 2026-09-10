package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HealthReadingEntity
import com.example.ui.theme.HealthAmber
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.HealthRed
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealSecondary
import com.example.util.ClinicalEvaluator
import com.example.util.HealthSeverity
import com.example.util.Strings

@Composable
fun HealthHistoryScreen(
    language: String,
    readings: List<HealthReadingEntity>,
    onNavigateToLogReading: () -> Unit = {},
    onNavigateToMedications: () -> Unit = {},
    onNavigateToClinicLocator: () -> Unit = {},
    onDeleteReading: (Long) -> Unit = {},
    onSyncNow: () -> Unit = {},
    isSyncing: Boolean = false,
    pendingSyncCount: Int = 0
) {
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedReadingForDetail by remember { mutableStateOf<HealthReadingEntity?>(null) }

    val filteredReadings = remember(readings, selectedCategoryFilter) {
        when (selectedCategoryFilter) {
            "Sugar" -> readings.filter { it.type.contains("sugar", ignoreCase = true) }
            "Pressure" -> readings.filter { it.type.contains("pressure", ignoreCase = true) }
            "Symptoms" -> readings.filter { it.type.contains("symptom", ignoreCase = true) }
            else -> readings
        }
    }

    // Dynamic chart points from real readings in database
    val chartDataPoints = remember(readings, selectedCategoryFilter) {
        val targetList = when (selectedCategoryFilter) {
            "Pressure" -> readings.filter { it.type.contains("pressure", ignoreCase = true) }
            "Sugar" -> readings.filter { it.type.contains("sugar", ignoreCase = true) }
            else -> readings.filter { it.type.contains("sugar", ignoreCase = true) || it.type.contains("pressure", ignoreCase = true) }
        }
        // Take up to 8 readings in chronological order (oldest to newest)
        targetList.take(8).reversed().mapNotNull { r ->
            if (r.type.contains("pressure", ignoreCase = true)) {
                ClinicalEvaluator.parseBloodPressure(r.value)?.first?.toDouble()
            } else {
                r.value.toDoubleOrNull()
            }
        }
    }

    // Check if there are any critical readings to show quick clinic navigation alert
    val hasCriticalVitals = remember(readings) {
        readings.take(5).any { item ->
            if (item.type.contains("pressure", ignoreCase = true)) {
                val bp = ClinicalEvaluator.parseBloodPressure(item.value)
                bp != null && (bp.first >= 140 || bp.second >= 90)
            } else if (item.type.contains("sugar", ignoreCase = true)) {
                val s = item.value.toDoubleOrNull()
                s != null && (s >= 11.1 || s < 3.9)
            } else false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Header with "Log New" Action Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = Strings.get("health_log", language),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${readings.size} entries recorded · syncs with all pages",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onNavigateToLogReading,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("health_log_add_reading_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Log", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Urgent Clinic Prompt Banner if severe vitals are present in the log
        if (hasCriticalVitals) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToClinicLocator() },
                colors = CardDefaults.cardColors(containerColor = HealthRed.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = HealthRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Elevated vitals logged. Review nearest clinics & doctors.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HealthRed
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = HealthRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Category Filter Chips (All, Sugar, Pressure, Symptoms)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("All" to "All Logs", "Sugar" to "Sugar", "Pressure" to "Pressure", "Symptoms" to "Symptoms").forEach { (filterKey, filterLabel) ->
                val isSelected = (selectedCategoryFilter == filterKey)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) TealPrimary else Color.Transparent)
                        .clickable { selectedCategoryFilter = filterKey }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filterLabel,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Canvas Trend Line Chart Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint = HealthGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedCategoryFilter == "Pressure") "Blood Pressure Trend" else "Blood Glucose Trend (mmol/L)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (selectedCategoryFilter == "Pressure") "Target < 120/80" else "Target: 4.0 - 10.0",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Line Canvas Graph dynamically sized to real readings
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val width = size.width
                    val height = size.height

                    // Baseline grid
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.2f),
                        start = Offset(0f, height * 0.5f),
                        end = Offset(width, height * 0.5f),
                        strokeWidth = 1f
                    )

                    if (chartDataPoints.isNotEmpty()) {
                        val minVal = chartDataPoints.minOrNull() ?: 4.0
                        val maxVal = chartDataPoints.maxOrNull() ?: 10.0
                        val range = if (maxVal > minVal) (maxVal - minVal) else 1.0

                        val points = chartDataPoints.mapIndexed { idx, valNum ->
                            val x = if (chartDataPoints.size > 1) {
                                (idx.toFloat() / (chartDataPoints.size - 1)) * (width - 40.dp.toPx()) + 20.dp.toPx()
                            } else {
                                width / 2f
                            }
                            val normalized = ((valNum - minVal) / range).toFloat()
                            val y = (height - 20.dp.toPx()) - (normalized * (height - 40.dp.toPx()))
                            Offset(x, y)
                        }

                        if (points.size >= 2) {
                            val path = Path().apply {
                                moveTo(points[0].x, points[0].y)
                                for (i in 1 until points.size) {
                                    lineTo(points[i].x, points[i].y)
                                }
                            }

                            drawPath(
                                path = path,
                                color = HealthGreen,
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }

                        points.forEach { pt ->
                            drawCircle(
                                color = HealthGreen,
                                radius = 4.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Earlier", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Latest", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Inter-page Action Hub (Quick shortcuts to Medication review & Clinic finder)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToMedications() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(TealPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = Strings.get("medications", language),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Check Doses",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToClinicLocator() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(TealSecondary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalHospital,
                            contentDescription = null,
                            tint = TealSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = Strings.get("find_clinic", language),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Nearest Doctor",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Recent Entries List Header with sync indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent entries (${filteredReadings.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (pendingSyncCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onSyncNow() }
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = HealthAmber)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = HealthAmber, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$pendingSyncCount pending sync",
                        fontSize = 11.sp,
                        color = HealthAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredReadings.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No records found for this category.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onNavigateToLogReading) {
                        Text("Log a reading now")
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filteredReadings) { item ->
                    val isExpanded = (selectedReadingForDetail?.id == item.id)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedReadingForDetail = if (isExpanded) null else item
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val itemTiming = when {
                                        item.whenTaken.isNotBlank() && item.whenTaken != "Health Log" && item.whenTaken != "Recorded" -> item.whenTaken
                                        item.notes.contains("2h Post-Meal", ignoreCase = true) -> "2h Post-Meal"
                                        item.notes.contains("Bedtime", ignoreCase = true) -> "Bedtime"
                                        item.notes.contains("Before Meal", ignoreCase = true) || item.notes.contains("Pre-Meal", ignoreCase = true) -> "Before Meal"
                                        item.notes.contains("Post-Exercise", ignoreCase = true) -> "Post-Exercise"
                                        item.notes.contains("Evening", ignoreCase = true) -> "Evening"
                                        else -> "Routine"
                                    }

                                    // Clinical Evaluation for past record
                                    val eval = when {
                                        item.type.contains("pressure", ignoreCase = true) -> {
                                            val bp = ClinicalEvaluator.parseBloodPressure(item.value)
                                            if (bp != null) ClinicalEvaluator.evaluateBloodPressure(bp.first, bp.second, item.notes, itemTiming) else null
                                        }
                                        item.type.contains("sugar", ignoreCase = true) -> {
                                            val s = item.value.toDoubleOrNull()
                                            if (s != null) ClinicalEvaluator.evaluateBloodSugar(s, item.unit, item.notes, itemTiming) else null
                                        }
                                        else -> null
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (item.unit.isNotBlank()) "${item.value} ${item.unit}" else item.value,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (eval != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(eval.severity.color.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${eval.badgeIcon} ${eval.classification}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = eval.severity.color
                                                )
                                            }
                                        } else if (item.type.contains("symptom", ignoreCase = true)) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(TealPrimary.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "🩺 Symptom",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TealPrimary
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "${item.type} · ${item.whenTaken}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (item.notes.isNotBlank()) {
                                        Text(
                                            text = "• ${item.notes}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = item.displayTime,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (item.isPendingSync) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = if (item.isPendingSync) HealthAmber else HealthGreen,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (item.isPendingSync) "Saved offline" else "Synced",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (item.isPendingSync) HealthAmber else HealthGreen
                                        )
                                    }
                                }
                            }

                            // Expanded detail view with cross-page action shortcuts
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "Log Entry Actions",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = onNavigateToLogReading,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Log New", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = onNavigateToClinicLocator,
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Find Clinic", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                onDeleteReading(item.id)
                                                selectedReadingForDetail = null
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = HealthRed)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp), tint = HealthRed)
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Delete", fontSize = 11.sp, color = HealthRed)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
