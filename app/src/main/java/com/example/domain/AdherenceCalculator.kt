package com.example.domain

import com.example.data.model.DoseLogEntity
import com.example.data.model.DoseStatus
import com.example.data.model.MedicineEntity
import com.example.data.model.MedicineWithSchedules
import com.example.data.model.PatientEntity
import com.example.data.model.ReportPeriod
import java.util.TimeZone
import kotlin.math.roundToInt

data class DailyAdherencePoint(
    val epochDay: Long,
    val takenCount: Int,
    val expectedCount: Int,
    val missedOrSkippedCount: Int
) {
    val percentage: Int
        get() = if (expectedCount <= 0) 100 else ((takenCount.toDouble() / expectedCount) * 100.0).roundToInt().coerceIn(0, 100)
}

data class AdherenceSummary(
    val period: ReportPeriod,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val totalScheduled: Int,
    val takenCount: Int,
    val skippedCount: Int,
    val missedCount: Int,
    val pendingCount: Int,
    val adherencePercent: Int,
    val dailyBreakdown: List<DailyAdherencePoint>
)

object AdherenceCalculator {

    fun calculateSummary(
        period: ReportPeriod,
        anchorEpochDay: Long,
        medicinesWithSchedules: List<MedicineWithSchedules>,
        allLogs: List<DoseLogEntity>,
        patientsById: Map<Long, PatientEntity>,
        activePatientId: Long?,
        nowMillis: Long,
        autoMissedWindowMinutes: Int = 30,
        timeZone: TimeZone = TimeZone.getDefault()
    ): AdherenceSummary {
        val dayCount = when (period) {
            ReportPeriod.DAILY -> 1
            ReportPeriod.WEEKLY -> 7
            ReportPeriod.MONTHLY -> 30
        }
        val startEpochDay = anchorEpochDay - (dayCount - 1)
        val logsByDay = allLogs.groupBy { it.scheduledDateEpochDay }

        var totalScheduled = 0
        var taken = 0
        var skipped = 0
        var missed = 0
        var pending = 0
        val points = mutableListOf<DailyAdherencePoint>()

        for (day in startEpochDay..anchorEpochDay) {
            val dayDoses = DoseSchedulerCalculator.buildDosesForDate(
                epochDay = day,
                medicinesWithSchedules = medicinesWithSchedules,
                logsForDay = logsByDay[day].orEmpty(),
                patientsById = patientsById,
                activePatientId = activePatientId,
                nowMillis = nowMillis,
                autoMissedWindowMinutes = autoMissedWindowMinutes,
                timeZone = timeZone
            )
            val dayTaken = dayDoses.count { it.status == DoseStatus.TAKEN }
            val daySkipped = dayDoses.count { it.status == DoseStatus.SKIPPED }
            val dayMissed = dayDoses.count { it.status == DoseStatus.MISSED }
            val dayPending = dayDoses.count { it.status == DoseStatus.PENDING || it.status == DoseStatus.SNOOZED }

            totalScheduled += dayDoses.size
            taken += dayTaken
            skipped += daySkipped
            missed += dayMissed
            pending += dayPending

            val resolvedCount = dayTaken + daySkipped + dayMissed
            points.add(
                DailyAdherencePoint(
                    epochDay = day,
                    takenCount = dayTaken,
                    expectedCount = resolvedCount.coerceAtLeast(if (dayDoses.isNotEmpty()) dayDoses.size else 0),
                    missedOrSkippedCount = daySkipped + dayMissed
                )
            )
        }

        val denominator = taken + skipped + missed
        val adherencePercent = if (denominator == 0) {
            if (totalScheduled == 0) 100 else 0
        } else {
            ((taken.toDouble() / denominator) * 100.0).roundToInt().coerceIn(0, 100)
        }

        return AdherenceSummary(
            period = period,
            startEpochDay = startEpochDay,
            endEpochDay = anchorEpochDay,
            totalScheduled = totalScheduled,
            takenCount = taken,
            skippedCount = skipped,
            missedCount = missed,
            pendingCount = pending,
            adherencePercent = adherencePercent,
            dailyBreakdown = points
        )
    }

    fun calculateDaysOfStockRemaining(medicine: MedicineEntity, enabledScheduleCount: Int): Int {
        if (medicine.stockCount <= 0.0 || medicine.doseQuantityPerIntake <= 0.0 || enabledScheduleCount <= 0) {
            return 0
        }
        val dailyConsumption = medicine.doseQuantityPerIntake * enabledScheduleCount
        return (medicine.stockCount / dailyConsumption).toInt().coerceAtLeast(0)
    }
}
