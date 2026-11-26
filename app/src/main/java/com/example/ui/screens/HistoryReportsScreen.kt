package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.DoseStatus
import com.example.data.model.ReportPeriod
import com.example.data.model.ScheduledDoseItem
import com.example.domain.DoseSchedulerCalculator
import com.example.ui.strings.LocalAppStrings
import com.example.ui.theme.AdherenceBarChart
import com.example.ui.theme.AdherenceRingChart
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldTaken
import com.example.ui.theme.GlassCard
import com.example.ui.theme.IndigoMuted
import com.example.ui.theme.StatusPill
import com.example.ui.viewmodel.MediAlertUiState
import com.example.util.SavedPdfReport
import com.example.util.TimeFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HistoryReportsScreen(
    uiState: MediAlertUiState,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onSelectDateEpochDay: (Long) -> Unit,
    onSavePdfReport: () -> Unit,
    onOpenSavedPdf: (SavedPdfReport) -> Unit,
    onSharePdfReport: () -> Unit,
    onTakeDose: (ScheduledDoseItem) -> Unit,
    onSnoozeDose: (ScheduledDoseItem, Int) -> Unit,
    onSkipDose: (ScheduledDoseItem) -> Unit
) {
    val strings = LocalAppStrings.current
    val language = uiState.preferences.language
    val summary = uiState.adherenceSummary

    val initialCal = remember(uiState.selectedDateEpochDay) {
        val (y, m, _) = DoseSchedulerCalculator.yearMonthDayFromEpochDay(uiState.selectedDateEpochDay)
        y to m
    }
    var visibleYearMonth by remember { mutableStateOf(initialCal) }

    val logsByDay = remember(uiState.allDoseLogs) {
        uiState.allDoseLogs.groupBy { it.scheduledDateEpochDay }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("history_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Period Filter
        item(key = "period_selector") {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ReportPeriod.entries.forEachIndexed { index, period ->
                    SegmentedButton(
                        selected = uiState.reportPeriod == period,
                        onClick = { onSelectPeriod(period) },
                        shape = SegmentedButtonDefaults.itemShape(index, ReportPeriod.entries.size),
                        modifier = Modifier.testTag("period_${period.name}")
                    ) {
                        Text(strings.period(period))
                    }
                }
            }
        }

        // Adherence Analytics Card
        if (summary != null) {
            item(key = "analytics_card") {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AdherenceRingChart(
                                percentage = summary.adherencePercent,
                                language = language,
                                label = strings.period(summary.period),
                                size = 96.dp
                            )
                            val totalDoses = (summary.takenCount + summary.skippedCount + summary.missedCount + summary.pendingCount)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatLine(
                                    label = strings.takenCountLabel,
                                    count = summary.takenCount,
                                    totalCount = totalDoses,
                                    color = EmeraldTaken,
                                    language = language
                                )
                                StatLine(
                                    label = strings.skippedCountLabel,
                                    count = summary.skippedCount,
                                    totalCount = totalDoses,
                                    color = IndigoMuted,
                                    language = language
                                )
                                StatLine(
                                    label = strings.missedCountLabel,
                                    count = summary.missedCount,
                                    totalCount = totalDoses,
                                    color = CoralAlert,
                                    language = language
                                )
                                StatLine(
                                    label = strings.pendingCountLabel,
                                    count = summary.pendingCount,
                                    totalCount = totalDoses,
                                    color = AmberWarning,
                                    language = language
                                )
                            }
                        }

                        if (summary.dailyBreakdown.size > 1) {
                            AdherenceBarChart(
                                points = summary.dailyBreakdown,
                                language = language
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onSavePdfReport,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_pdf_button")
                            ) {
                                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.exportPdfReport)
                            }
                            FilledTonalButton(
                                onClick = onSharePdfReport,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_pdf_button")
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.sharePdfReport)
                            }
                        }

                        AnimatedVisibility(visible = uiState.lastSavedPdfReport != null) {
                            uiState.lastSavedPdfReport?.let { savedPdf ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(EmeraldTaken.copy(alpha = 0.12f))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldTaken,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = strings.pdfSavedMsg,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = EmeraldTaken
                                        )
                                        Text(
                                            text = savedPdf.displayPath,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = { onOpenSavedPdf(savedPdf) },
                                        modifier = Modifier.testTag("open_saved_pdf_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                            contentDescription = strings.openPdfButton,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(strings.openPdfLabel)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Interactive Monthly Calendar Card
        item(key = "calendar_card") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                val (year, month) = visibleYearMonth
                val monthCal = remember(year, month) {
                    Calendar.getInstance().apply {
                        clear()
                        set(year, month, 1)
                    }
                }
                val monthTitle = remember(year, month, language) {
                    val locale = if (language == AppLanguage.BANGLA) Locale("bn", "BD") else Locale.ENGLISH
                    TimeFormatter.localizeDigits(SimpleDateFormat("MMMM yyyy", locale).format(monthCal.time), language)
                }
                val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val firstDayOffset = (monthCal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    visibleYearMonth = if (month == 0) (year - 1) to 11 else year to (month - 1)
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
                            }
                            IconButton(
                                onClick = {
                                    visibleYearMonth = if (month == 11) (year + 1) to 0 else year to (month + 1)
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
                            }
                        }
                    }

                    val weekdays = if (language == AppLanguage.BANGLA) {
                        listOf("র", "সো", "ম", "বু", "বৃ", "শু", "শ")
                    } else {
                        listOf("S", "M", "T", "W", "T", "F", "S")
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        weekdays.forEach { dayLabel ->
                            Text(
                                text = dayLabel,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val totalCells = firstDayOffset + daysInMonth
                    val weeks = (totalCells + 6) / 7
                    for (week in 0 until weeks) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (dow in 0..6) {
                                val cellIdx = week * 7 + dow
                                val dayNum = cellIdx - firstDayOffset + 1
                                if (dayNum in 1..daysInMonth) {
                                    val cellEpochDay = DoseSchedulerCalculator.toEpochDay(year, month, dayNum)
                                    val isSelected = cellEpochDay == uiState.selectedDateEpochDay
                                    val dayLogs = logsByDay[cellEpochDay].orEmpty()
                                    val dotColor = when {
                                        dayLogs.any { it.status == DoseStatus.MISSED } -> CoralAlert
                                        dayLogs.any { it.status == DoseStatus.TAKEN } -> EmeraldTaken
                                        dayLogs.any { it.status == DoseStatus.SKIPPED } -> IndigoMuted
                                        else -> null
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else Color.Transparent
                                            )
                                            .clickable { onSelectDateEpochDay(cellEpochDay) }
                                            .testTag("calendar_day_$dayNum"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = TimeFormatter.localizeDigits(dayNum.toString(), language),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (dotColor != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else dotColor)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Day's Doses
        item(key = "selected_day_header") {
            Text(
                text = TimeFormatter.formatEpochDay(uiState.selectedDateEpochDay, language, "EEEE, dd MMM yyyy"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (uiState.selectedDateDoses.isEmpty()) {
            item(key = "selected_day_empty") {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = strings.emptyDosesTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(
                items = uiState.selectedDateDoses,
                key = { "hist_${it.schedule.id}_${it.scheduledDateEpochDay}" }
            ) { dose ->
                DoseTimelineCard(
                    dose = dose,
                    nowMillis = uiState.currentTimeMillis,
                    language = language,
                    onTake = { onTakeDose(dose) },
                    onSnooze = { mins -> onSnoozeDose(dose, mins) },
                    onSkip = { onSkipDose(dose) }
                )
            }
        }
    }
}

@Composable
private fun StatLine(
    label: String,
    count: Int,
    totalCount: Int,
    color: Color,
    language: AppLanguage
) {
    val targetFraction = if (totalCount > 0) (count.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "stat_seekbar"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
            StatusPill(
                text = TimeFormatter.localizeDigits(count.toString(), language),
                containerColor = color.copy(alpha = 0.16f),
                contentColor = color
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
        ) {
            if (animatedFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}
