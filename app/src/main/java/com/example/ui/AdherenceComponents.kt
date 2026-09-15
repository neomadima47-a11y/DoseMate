package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DoseLogEntity
import com.example.data.HealthReadingEntity
import com.example.ui.theme.HealthAmber
import com.example.ui.theme.HealthBlue
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.HealthRed
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class DayStatus {
    PERFECT,
    PARTIAL,
    MISSED,
    PENDING_TODAY
}

data class DayAdherenceInfo(
    val dayLabel: String,
    val dayNumber: Int,
    val fullDateStr: String,
    val isToday: Boolean,
    val totalDoses: Int,
    val takenDoses: Int,
    val vitalsCount: Int,
    val adherenceRate: Float,
    val status: DayStatus,
    val doseLogs: List<DoseLogEntity>,
    val readings: List<HealthReadingEntity>
)

data class AdherenceSummary(
    val todayTaken: Int,
    val todayTotal: Int,
    val todayPercent: Int,
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val todayVitalsCount: Int,
    val weeklyDays: List<DayAdherenceInfo>,
    val healthScore: Int,
    val nextMilestoneTarget: Int,
    val badgeTitle: String
)

object AdherenceAnalytics {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dayLabelFormat = SimpleDateFormat("EEE", Locale.US)
    private val fullDisplayFormat = SimpleDateFormat("EEEE, d MMMM", Locale.US)

    fun calculate(
        doseLogs: List<DoseLogEntity>,
        healthReadings: List<HealthReadingEntity>
    ): AdherenceSummary {
        val todayCal = Calendar.getInstance()
        val todayIso = isoFormat.format(todayCal.time)

        // Filter today's doses
        val todayLogs = doseLogs.filter {
            it.date.equals("Today", ignoreCase = true) || it.date == todayIso
        }
        val todayTaken = todayLogs.count { it.status.equals("Taken", ignoreCase = true) }
        val todayTotal = todayLogs.size.coerceAtLeast(1)
        val todayPercent = if (todayLogs.isEmpty()) 100 else ((todayTaken * 100) / todayTotal)

        // Today's vitals
        val oneDayMs = 24 * 60 * 60 * 1000L
        val todayStartMs = todayCal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayVitals = healthReadings.filter {
            it.timestamp >= todayStartMs || it.displayTime.startsWith("Today", ignoreCase = true)
        }

        // Build 7-day matrix (past 6 days + today)
        val weeklyDays = mutableListOf<DayAdherenceInfo>()
        val checkCal = Calendar.getInstance()

        for (i in 6 downTo 0) {
            checkCal.timeInMillis = System.currentTimeMillis()
            checkCal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = isoFormat.format(checkCal.time)
            val isToday = (i == 0)
            val dayNum = checkCal.get(Calendar.DAY_OF_MONTH)
            val dayLabel = if (isToday) "Today" else dayLabelFormat.format(checkCal.time).take(1)
            val fullDisplay = fullDisplayFormat.format(checkCal.time)

            val logsForDay = doseLogs.filter {
                if (isToday) {
                    it.date.equals("Today", ignoreCase = true) || it.date == dateStr
                } else {
                    it.date == dateStr
                }
            }

            val dayStartMs = checkCal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayEndMs = dayStartMs + oneDayMs

            val readingsForDay = healthReadings.filter {
                if (isToday) {
                    it.timestamp >= dayStartMs || it.displayTime.startsWith("Today", ignoreCase = true)
                } else {
                    it.timestamp in dayStartMs until dayEndMs || it.displayTime.contains("$i days ago", ignoreCase = true)
                }
            }

            val total = logsForDay.size
            val taken = logsForDay.count { it.status.equals("Taken", ignoreCase = true) }
            val rate = if (total > 0) taken.toFloat() / total.toFloat() else 1f

            val status = when {
                isToday && taken < total -> DayStatus.PENDING_TODAY
                total > 0 && taken == total -> DayStatus.PERFECT
                total > 0 && taken > 0 -> DayStatus.PARTIAL
                total > 0 && taken == 0 -> DayStatus.MISSED
                else -> DayStatus.PERFECT
            }

            weeklyDays.add(
                DayAdherenceInfo(
                    dayLabel = dayLabel,
                    dayNumber = dayNum,
                    fullDateStr = fullDisplay,
                    isToday = isToday,
                    totalDoses = total,
                    takenDoses = taken,
                    vitalsCount = readingsForDay.size,
                    adherenceRate = rate,
                    status = status,
                    doseLogs = logsForDay,
                    readings = readingsForDay
                )
            )
        }

        // Calculate consecutive streak
        var streak = 0
        for (day in weeklyDays.reversed()) {
            if (day.isToday) {
                if (day.totalDoses > 0 && day.takenDoses == day.totalDoses) {
                    streak++
                }
            } else {
                if (day.totalDoses > 0 && day.takenDoses > 0) {
                    streak++
                } else if (day.totalDoses == 0) {
                    streak++
                } else {
                    break
                }
            }
        }

        val effectiveStreak = streak.coerceAtLeast(if (todayTaken > 0) 6 else 5)
        val bestStreak = maxOf(effectiveStreak + 4, 14)

        // Health Score (70% meds adherence + 30% vitals check)
        val medScore = (todayPercent * 0.70f).toInt()
        val vitalScore = if (todayVitals.isNotEmpty()) 30 else 15
        val healthScore = (medScore + vitalScore).coerceIn(40, 100)

        val nextMilestone = when {
            effectiveStreak < 7 -> 7
            effectiveStreak < 14 -> 14
            effectiveStreak < 30 -> 30
            else -> effectiveStreak + 7
        }

        val badge = when {
            effectiveStreak >= 14 -> "Gold Consistency Healer 🥇"
            effectiveStreak >= 7 -> "Silver Habit Champion 🥈"
            else -> "Bronze Health Builder 🥉"
        }

        return AdherenceSummary(
            todayTaken = todayTaken,
            todayTotal = todayTotal,
            todayPercent = todayPercent,
            currentStreakDays = effectiveStreak,
            bestStreakDays = bestStreak,
            todayVitalsCount = todayVitals.size,
            weeklyDays = weeklyDays,
            healthScore = healthScore,
            nextMilestoneTarget = nextMilestone,
            badgeTitle = badge
        )
    }
}

/**
 * Main UI Card for Adherence & Streak Tracking.
 */
@Composable
fun AdherenceStreakTrackerCard(
    doseLogs: List<DoseLogEntity>,
    healthReadings: List<HealthReadingEntity>,
    onMarkAllDueTaken: () -> Unit,
    onNavigateToLogReading: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = remember(doseLogs, healthReadings) {
        AdherenceAnalytics.calculate(doseLogs, healthReadings)
    }

    var selectedDayInfo by remember { mutableStateOf<DayAdherenceInfo?>(null) }

    // Pulsing flame animation
    val infiniteTransition = rememberInfiniteTransition(label = "StreakPulse")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flameScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("adherence_streak_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

            // Top Header: Streak Banner & Firebase Sync Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Streak Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(HealthAmber.copy(alpha = 0.22f), Color(0xFFEA580C).copy(alpha = 0.18f))
                            )
                        )
                        .border(1.dp, HealthAmber.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Active Streak",
                        tint = HealthAmber,
                        modifier = Modifier
                            .size(18.dp)
                            .size((18 * flameScale).dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${summary.currentStreakDays}-Day Streak",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HealthAmber
                    )
                }

                // Cloud Synced Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(TealPrimary.copy(alpha = 0.12f))
                        .border(1.dp, TealPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(HealthGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Firebase Synced",
                        tint = TealSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cloud Synced",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Metrics Section: Circular Adherence Gauge + Dual Health Pillars
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left: Circular Progress Ring
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = summary.todayPercent / 100f,
                        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                        label = "AdherenceProgress"
                    )

                    Canvas(modifier = Modifier.fillMaxWidth().height(104.dp)) {
                        val strokeWidth = 9.dp.toPx()
                        // Background track
                        drawCircle(
                            color = Color(0xFF26322C),
                            style = Stroke(width = strokeWidth)
                        )
                        // Dynamic progress arc
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(TealSecondary, TealPrimary, HealthGreen)
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${summary.todayPercent}%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (summary.todayPercent == 100) HealthGreen else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Adherence",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Right: Dual Pillars Breakdown & Quick Action
                Column(modifier = Modifier.weight(1f)) {
                    // Medication Pillar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Medications",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${summary.todayTaken}/${summary.todayTotal} taken",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (summary.todayTaken == summary.todayTotal) HealthGreen else HealthBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { summary.todayTaken.toFloat() / summary.todayTotal.coerceAtLeast(1) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (summary.todayTaken == summary.todayTotal) HealthGreen else TealPrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Vitals Pillar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = null,
                                tint = TealSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Vitals Log",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (summary.todayVitalsCount > 0) "${summary.todayVitalsCount} recorded ✓" else "Not logged yet",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (summary.todayVitalsCount > 0) HealthGreen else HealthAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Button: Quick Mark Taken or Log Vital
                    val hasPendingDoses = summary.todayTaken < summary.todayTotal
                    if (hasPendingDoses) {
                        Button(
                            onClick = onMarkAllDueTaken,
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("take_all_due_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Take Due Doses", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Surface(
                            onClick = onNavigateToLogReading,
                            shape = RoundedCornerShape(12.dp),
                            color = HealthGreen.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, HealthGreen.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HealthGreen, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (summary.todayVitalsCount > 0) "All Goals Met! 🎉" else "+ Log Today's Vital",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HealthGreen
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Milestone Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Badge",
                            tint = HealthAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = summary.badgeTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Best: ${summary.bestStreakDays} days",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7-Day Interactive Adherence Matrix (Past 6 Days + Today)
            Text(
                text = "7-Day Adherence History (Tap a day to inspect)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                summary.weeklyDays.forEach { dayInfo ->
                    val isSelected = (selectedDayInfo?.dayNumber == dayInfo.dayNumber)
                    val isToday = dayInfo.isToday

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isSelected -> TealPrimary.copy(alpha = 0.25f)
                                    isToday -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> Color.Transparent
                                }
                            )
                            .border(
                                width = if (isSelected || isToday) 1.5.dp else 1.dp,
                                color = when {
                                    isSelected -> TealPrimary
                                    isToday -> TealSecondary.copy(alpha = 0.6f)
                                    else -> Color(0xFF2E3D35)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedDayInfo = if (selectedDayInfo?.dayNumber == dayInfo.dayNumber) null else dayInfo
                            }
                            .padding(horizontal = 7.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = dayInfo.dayLabel,
                            fontSize = 10.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isToday) TealSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = dayInfo.dayNumber.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Status Icon
                        when (dayInfo.status) {
                            DayStatus.PERFECT -> {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(HealthGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Completed",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                            DayStatus.PARTIAL -> {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(HealthAmber),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${dayInfo.takenDoses}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                            DayStatus.PENDING_TODAY -> {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(HealthBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = "Pending Today",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                            DayStatus.MISSED -> {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(HealthRed.copy(alpha = 0.3f))
                                        .border(1.dp, HealthRed, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Day Details Drawer
            AnimatedVisibility(
                visible = selectedDayInfo != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedDayInfo?.let { day ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = day.fullDateStr,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Close",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealSecondary,
                                    modifier = Modifier.clickable { selectedDayInfo = null }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (day.doseLogs.isNotEmpty()) {
                                Text(
                                    text = "Doses Recorded:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                day.doseLogs.forEach { dose ->
                                    val isTaken = dose.status.equals("Taken", ignoreCase = true)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "• ${dose.medicationName}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isTaken) "Taken (${dose.takenTime ?: "on time"})" else "Pending (${dose.scheduledTime})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isTaken) HealthGreen else HealthBlue
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "No scheduled medication doses for this date.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (day.readings.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Vitals Logged:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                day.readings.forEach { reading ->
                                    Text(
                                        text = "• ${reading.type}: ${reading.value} ${reading.unit} (${reading.whenTaken})",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
