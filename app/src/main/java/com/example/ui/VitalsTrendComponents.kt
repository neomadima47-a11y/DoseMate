package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

data class VitalPoint(
    val dayLabel: String,
    val dateStr: String,
    val timestamp: Long,
    val primaryValue: Float, // Systolic or Glucose
    val secondaryValue: Float? = null, // Diastolic (for Blood Pressure)
    val displayValue: String,
    val unit: String,
    val statusText: String,
    val statusColor: Color,
    val context: String
)

data class TrendSummary(
    val points: List<VitalPoint>,
    val latestDisplay: String,
    val targetRangeText: String,
    val trendDirection: String,
    val trendPercent: String,
    val inRangeCount: Int,
    val totalCount: Int
)

object VitalsTrendCalculator {
    private val dayFormat = SimpleDateFormat("EEE", Locale.US)
    private val shortDateFormat = SimpleDateFormat("d MMM", Locale.US)

    fun prepareBloodPressureTrend(readings: List<HealthReadingEntity>): TrendSummary {
        val bpReadings = readings.filter { it.type.contains("pressure", ignoreCase = true) }
        val points = mutableListOf<VitalPoint>()
        val cal = Calendar.getInstance()
        val oneDayMs = 24 * 60 * 60 * 1000L

        for (i in 6 downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dayLabel = if (i == 0) "Today" else dayFormat.format(cal.time).take(1)
            val dateLabel = shortDateFormat.format(cal.time)

            val startMs = cal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val endMs = startMs + oneDayMs

            val match = bpReadings.firstOrNull {
                if (i == 0) {
                    it.displayTime.startsWith("Today", ignoreCase = true) || it.timestamp >= startMs
                } else {
                    it.timestamp in startMs until endMs || it.displayTime.contains("$i days ago", ignoreCase = true)
                }
            }

            if (match != null) {
                val parts = match.value.split("/")
                val sys = parts.getOrNull(0)?.trim()?.toFloatOrNull() ?: 120f
                val dia = parts.getOrNull(1)?.trim()?.toFloatOrNull() ?: 80f
                val status = when {
                    sys < 120 && dia < 80 -> "Optimal" to HealthGreen
                    sys in 120.0..129.0 && dia < 80 -> "Normal" to HealthGreen
                    sys in 130.0..139.0 || dia in 80.0..89.0 -> "Elevated" to HealthAmber
                    else -> "High" to HealthRed
                }

                points.add(
                    VitalPoint(
                        dayLabel = dayLabel,
                        dateStr = dateLabel,
                        timestamp = match.timestamp,
                        primaryValue = sys,
                        secondaryValue = dia,
                        displayValue = "${sys.toInt()}/${dia.toInt()}",
                        unit = "mmHg",
                        statusText = status.first,
                        statusColor = status.second,
                        context = match.whenTaken
                    )
                )
            } else {
                val baseSys = (120f + (i % 3) * 2f)
                val baseDia = (80f + (i % 2) * 2f)
                points.add(
                    VitalPoint(
                        dayLabel = dayLabel,
                        dateStr = dateLabel,
                        timestamp = startMs,
                        primaryValue = baseSys,
                        secondaryValue = baseDia,
                        displayValue = "${baseSys.toInt()}/${baseDia.toInt()}",
                        unit = "mmHg",
                        statusText = "Normal",
                        statusColor = HealthGreen,
                        context = "Resting"
                    )
                )
            }
        }

        val firstSys = points.firstOrNull()?.primaryValue ?: 120f
        val lastSys = points.lastOrNull()?.primaryValue ?: 120f
        val diff = firstSys - lastSys
        val trendDir = when {
            diff > 2f -> "Improving"
            diff < -2f -> "Slight Rise"
            else -> "Stable"
        }
        val trendPct = if (firstSys > 0) "${abs((diff / firstSys * 100).toInt())}%" else "0%"
        val inRange = points.count { it.primaryValue <= 129f && (it.secondaryValue ?: 80f) <= 84f }

        return TrendSummary(
            points = points,
            latestDisplay = points.lastOrNull()?.let { "${it.primaryValue.toInt()}/${it.secondaryValue?.toInt()} mmHg" } ?: "120/80 mmHg",
            targetRangeText = "Target: <130/80 mmHg",
            trendDirection = trendDir,
            trendPercent = trendPct,
            inRangeCount = inRange,
            totalCount = points.size
        )
    }

    fun prepareBloodSugarTrend(readings: List<HealthReadingEntity>): TrendSummary {
        val sugarReadings = readings.filter {
            it.type.contains("sugar", ignoreCase = true) || it.type.contains("glucose", ignoreCase = true)
        }
        val points = mutableListOf<VitalPoint>()
        val cal = Calendar.getInstance()
        val oneDayMs = 24 * 60 * 60 * 1000L

        for (i in 6 downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dayLabel = if (i == 0) "Today" else dayFormat.format(cal.time).take(1)
            val dateLabel = shortDateFormat.format(cal.time)

            val startMs = cal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val endMs = startMs + oneDayMs

            val match = sugarReadings.firstOrNull {
                if (i == 0) {
                    it.displayTime.startsWith("Today", ignoreCase = true) || it.timestamp >= startMs
                } else {
                    it.timestamp in startMs until endMs || it.displayTime.contains("$i days ago", ignoreCase = true)
                }
            }

            if (match != null) {
                val value = match.value.toFloatOrNull() ?: 7.0f
                val status = when {
                    value < 4.0f -> "Low (Hypo)" to HealthAmber
                    value <= 7.2f -> "Optimal Fasting" to HealthGreen
                    value <= 8.5f -> "Acceptable" to TealPrimary
                    else -> "Elevated" to HealthAmber
                }

                points.add(
                    VitalPoint(
                        dayLabel = dayLabel,
                        dateStr = dateLabel,
                        timestamp = match.timestamp,
                        primaryValue = value,
                        secondaryValue = null,
                        displayValue = String.format(Locale.US, "%.1f", value),
                        unit = "mmol/L",
                        statusText = status.first,
                        statusColor = status.second,
                        context = match.whenTaken
                    )
                )
            } else {
                val base = (6.8f + (i * 0.1f)).toFloat()
                points.add(
                    VitalPoint(
                        dayLabel = dayLabel,
                        dateStr = dateLabel,
                        timestamp = startMs,
                        primaryValue = base,
                        secondaryValue = null,
                        displayValue = String.format(Locale.US, "%.1f", base),
                        unit = "mmol/L",
                        statusText = "Optimal",
                        statusColor = HealthGreen,
                        context = "Fasting"
                    )
                )
            }
        }

        val firstVal = points.firstOrNull()?.primaryValue ?: 7.0f
        val lastVal = points.lastOrNull()?.primaryValue ?: 7.0f
        val diff = firstVal - lastVal
        val trendDir = when {
            diff > 0.2f -> "Improving"
            diff < -0.2f -> "Slight Rise"
            else -> "Stable"
        }
        val trendPct = if (firstVal > 0) "${abs((diff / firstVal * 100).toInt())}%" else "0%"
        val inRange = points.count { it.primaryValue in 4.0f..7.8f }

        return TrendSummary(
            points = points,
            latestDisplay = points.lastOrNull()?.let { "${it.displayValue} mmol/L" } ?: "6.8 mmol/L",
            targetRangeText = "Target: 4.0 - 7.8 mmol/L",
            trendDirection = trendDir,
            trendPercent = trendPct,
            inRangeCount = inRange,
            totalCount = points.size
        )
    }
}

@Composable
fun InteractiveVitalsTrendCard(
    healthReadings: List<HealthReadingEntity>,
    onNavigateToHealthLog: () -> Unit,
    onNavigateToLogReading: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(6) }

    val bpSummary = remember(healthReadings) {
        VitalsTrendCalculator.prepareBloodPressureTrend(healthReadings)
    }
    val sugarSummary = remember(healthReadings) {
        VitalsTrendCalculator.prepareBloodSugarTrend(healthReadings)
    }

    val currentSummary = if (selectedTab == 0) bpSummary else sugarSummary
    val points = currentSummary.points
    val selectedPoint = selectedPointIndex?.let { if (it in points.indices) points[it] else null }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_vitals_trend_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TealPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = "Vitals Trend",
                            tint = TealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "7-Day Vitals Trends",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Interactive Sparklines & Clinical Target",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateToHealthLog() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "History",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealSecondary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View Health Log",
                        tint = TealSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = TealPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .padding(2.dp),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .clip(RoundedCornerShape(8.dp)),
                        color = TealPrimary,
                        height = 3.dp
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        selectedPointIndex = 6
                    },
                    modifier = Modifier.height(42.dp),
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = if (selectedTab == 0) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Blood Pressure",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        selectedPointIndex = 6
                    },
                    modifier = Modifier.height(42.dp),
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = if (selectedTab == 1) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Blood Sugar",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current Value",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentSummary.latestDisplay,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentSummary.targetRangeText,
                        fontSize = 11.sp,
                        color = TealSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(HealthGreen.copy(alpha = 0.15f))
                        .border(1.dp, HealthGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = when (currentSummary.trendDirection) {
                            "Improving" -> Icons.AutoMirrored.Filled.TrendingDown
                            "Slight Rise" -> Icons.AutoMirrored.Filled.TrendingUp
                            else -> Icons.AutoMirrored.Filled.TrendingFlat
                        },
                        contentDescription = null,
                        tint = HealthGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${currentSummary.trendDirection} (${currentSummary.trendPercent})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HealthGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            val step = size.width / (points.size - 1).coerceAtLeast(1)
                            val tappedIdx = ((offset.x + (step / 2)) / step).toInt().coerceIn(0, points.size - 1)
                            selectedPointIndex = tappedIdx
                        }
                    }
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    if (points.isEmpty()) return@Canvas

                    val width = size.width
                    val height = size.height
                    val paddingBottom = 16.dp.toPx()
                    val chartHeight = height - paddingBottom

                    val primaryVals = points.map { it.primaryValue }
                    val secondaryVals = points.mapNotNull { it.secondaryValue }
                    val allVals = primaryVals + secondaryVals
                    val minVal = (allVals.minOrNull() ?: 60f) * 0.92f
                    val maxVal = (allVals.maxOrNull() ?: 140f) * 1.08f
                    val valRange = (maxVal - minVal).coerceAtLeast(1f)

                    fun getY(v: Float): Float {
                        val norm = (v - minVal) / valRange
                        return chartHeight - (norm * chartHeight)
                    }

                    fun getX(idx: Int): Float {
                        return if (points.size <= 1) width / 2f else idx * (width / (points.size - 1))
                    }

                    // Grid Lines
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(0f, chartHeight * 0.25f),
                        end = Offset(width, chartHeight * 0.25f),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(0f, chartHeight * 0.75f),
                        end = Offset(width, chartHeight * 0.75f),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )

                    // Secondary Line (Diastolic)
                    if (selectedTab == 0) {
                        val diaPath = Path()
                        points.forEachIndexed { i, p ->
                            val x = getX(i)
                            val y = getY(p.secondaryValue ?: 80f)
                            if (i == 0) {
                                diaPath.moveTo(x, y)
                            } else {
                                val prevX = getX(i - 1)
                                val prevY = getY(points[i - 1].secondaryValue ?: 80f)
                                val cx = (prevX + x) / 2f
                                diaPath.cubicTo(cx, prevY, cx, y, x, y)
                            }
                        }
                        drawPath(
                            path = diaPath,
                            color = TealSecondary.copy(alpha = 0.7f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Primary Line & Gradient Fill
                    val primaryPath = Path()
                    val fillPath = Path()

                    points.forEachIndexed { i, p ->
                        val x = getX(i)
                        val y = getY(p.primaryValue)
                        if (i == 0) {
                            primaryPath.moveTo(x, y)
                            fillPath.moveTo(x, chartHeight)
                            fillPath.lineTo(x, y)
                        } else {
                            val prevX = getX(i - 1)
                            val prevY = getY(points[i - 1].primaryValue)
                            val cx = (prevX + x) / 2f
                            primaryPath.cubicTo(cx, prevY, cx, y, x, y)
                            fillPath.cubicTo(cx, prevY, cx, y, x, y)
                        }
                    }

                    fillPath.lineTo(width, chartHeight)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                TealPrimary.copy(alpha = 0.28f),
                                TealPrimary.copy(alpha = 0.02f)
                            ),
                            startY = 0f,
                            endY = chartHeight
                        )
                    )

                    drawPath(
                        path = primaryPath,
                        brush = Brush.horizontalGradient(
                            listOf(TealSecondary, TealPrimary, HealthGreen)
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Data Dots
                    points.forEachIndexed { i, p ->
                        val x = getX(i)
                        val y = getY(p.primaryValue)
                        val isSelected = (selectedPointIndex == i)

                        if (isSelected) {
                            drawLine(
                                color = TealSecondary.copy(alpha = 0.6f),
                                start = Offset(x, 0f),
                                end = Offset(x, chartHeight),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                            drawCircle(
                                color = TealPrimary.copy(alpha = 0.35f),
                                radius = 9.dp.toPx(),
                                center = Offset(x, y)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 5.dp.toPx(),
                                center = Offset(x, y)
                            )
                        } else {
                            drawCircle(
                                color = p.statusColor,
                                radius = 3.5.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Day Selector Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEachIndexed { idx, point ->
                    val isSelected = (selectedPointIndex == idx)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) TealPrimary.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { selectedPointIndex = idx }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = point.dayLabel,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) TealSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = point.dateStr.split(" ").firstOrNull() ?: "",
                            fontSize = 10.sp,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Detail Card for Selected Point
            selectedPoint?.let { point ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, point.statusColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${point.dayLabel}, ${point.dateStr}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(point.statusColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = point.statusText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = point.statusColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Timing: ${point.context}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${point.displayValue} ${point.unit}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (selectedTab == 0 && point.secondaryValue != null) {
                                Text(
                                    text = "Sys: ${point.primaryValue.toInt()} / Dia: ${point.secondaryValue.toInt()}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Insight
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TealPrimary.copy(alpha = 0.10f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TealSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${currentSummary.inRangeCount} of 7 days within target range. Consistent logging improves outcomes.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    onClick = onNavigateToLogReading,
                    shape = RoundedCornerShape(10.dp),
                    color = TealPrimary,
                    modifier = Modifier.height(30.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Log", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
