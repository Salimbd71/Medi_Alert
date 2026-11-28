package com.slm.domain

import com.slm.data.model.DoseLogEntity
import com.slm.data.model.DoseStatus
import com.slm.data.model.MedicineEntity
import com.slm.data.model.MedicineWithSchedules
import com.slm.data.model.NextDoseInfo
import com.slm.data.model.PatientEntity
import com.slm.data.model.ScheduleEntity
import com.slm.data.model.ScheduledDoseItem
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.floor

object DoseSchedulerCalculator {

    /**
     * Converts a local calendar date (year, month 0-based, dayOfMonth) into an epochDay integer
     * independent of daylight saving shifts.
     */
    fun toEpochDay(year: Int, monthZeroBased: Int, dayOfMonth: Int): Long {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, monthZeroBased, dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return floor(utcCal.timeInMillis / 86_400_000.0).toLong()
    }

    fun epochDayFromMillis(
        millis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = millis
        }
        return toEpochDay(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun yearMonthDayFromEpochDay(epochDay: Long): Triple<Int, Int, Int> {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = epochDay * 86_400_000L
        }
        return Triple(
            utcCal.get(Calendar.YEAR),
            utcCal.get(Calendar.MONTH),
            utcCal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun triggerMillisForEpochDay(
        epochDay: Long,
        hour: Int,
        minute: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val (year, month, day) = yearMonthDayFromEpochDay(epochDay)
        val cal = Calendar.getInstance(timeZone).apply {
            clear()
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Bit 0 = Monday ... Bit 6 = Sunday
     */
    fun isWeekdayActive(epochDay: Long, daysOfWeekMask: Int): Boolean {
        val (year, month, day) = yearMonthDayFromEpochDay(epochDay)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month, day, 12, 0, 0)
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val bitIndex = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        return (daysOfWeekMask and (1 shl bitIndex)) != 0
    }

    fun isMedicineScheduledOnDay(
        medicine: MedicineEntity,
        schedule: ScheduleEntity,
        epochDay: Long
    ): Boolean {
        if (!medicine.isActive || !schedule.isEnabled) return false
        if (epochDay < medicine.startDateEpochDay) return false
        val endDay = medicine.endDateEpochDay
        if (endDay != null && epochDay > endDay) return false
        return isWeekdayActive(epochDay, schedule.daysOfWeekMask)
    }

    /**
     * Computes the next future trigger timestamp in millis for a schedule starting from [nowMillis].
     * Looks ahead up to 366 days so weekly/custom schedules are always found.
     */
    fun computeNextTriggerMillis(
        medicine: MedicineEntity,
        schedule: ScheduleEntity,
        nowMillis: Long,
        alreadyHandledToday: Boolean = false,
        snoozeUntilMillis: Long? = null,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long? {
        if (!medicine.isActive || !schedule.isEnabled) return null
        if (snoozeUntilMillis != null && snoozeUntilMillis > nowMillis) {
            return snoozeUntilMillis
        }

        val todayEpochDay = epochDayFromMillis(nowMillis, timeZone)
        val startSearchDay = maxOf(todayEpochDay, medicine.startDateEpochDay)
        val maxSearchDay = medicine.endDateEpochDay?.let { minOf(it, startSearchDay + 366L) }
            ?: (startSearchDay + 366L)

        for (day in startSearchDay..maxSearchDay) {
            if (day == todayEpochDay && alreadyHandledToday) continue
            if (!isMedicineScheduledOnDay(medicine, schedule, day)) continue
            val candidateMillis = triggerMillisForEpochDay(day, schedule.hour, schedule.minute, timeZone)
            if (candidateMillis > nowMillis) {
                return candidateMillis
            }
        }
        return null
    }

    fun buildDosesForDate(
        epochDay: Long,
        medicinesWithSchedules: List<MedicineWithSchedules>,
        logsForDay: List<DoseLogEntity>,
        patientsById: Map<Long, PatientEntity>,
        activePatientId: Long?,
        nowMillis: Long,
        autoMissedWindowMinutes: Int = 30,
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<ScheduledDoseItem> {
        val logByScheduleId = logsForDay.associateBy { it.scheduleId }
        val todayEpochDay = epochDayFromMillis(nowMillis, timeZone)
        val missedGraceMillis = TimeUnit.MINUTES.toMillis(autoMissedWindowMinutes.toLong())

        val result = mutableListOf<ScheduledDoseItem>()
        for (item in medicinesWithSchedules) {
            val medicine = item.medicine
            if (activePatientId != null && medicine.patientId != activePatientId) continue
            val patient = item.patient ?: patientsById[medicine.patientId] ?: continue

            for (schedule in item.schedules) {
                if (!isMedicineScheduledOnDay(medicine, schedule, epochDay)) continue
                val scheduledMillis = triggerMillisForEpochDay(
                    epochDay = epochDay,
                    hour = schedule.hour,
                    minute = schedule.minute,
                    timeZone = timeZone
                )
                val log = logByScheduleId[schedule.id]
                val effectiveTrigger = log?.snoozeUntilMillis ?: scheduledMillis

                val resolvedStatus = when {
                    log != null && log.status != DoseStatus.PENDING -> {
                        if (log.status == DoseStatus.SNOOZED &&
                            log.snoozeUntilMillis != null &&
                            nowMillis > log.snoozeUntilMillis + missedGraceMillis
                        ) {
                            DoseStatus.MISSED
                        } else {
                            log.status
                        }
                    }
                    epochDay < todayEpochDay -> DoseStatus.MISSED
                    epochDay == todayEpochDay && nowMillis > scheduledMillis + missedGraceMillis -> DoseStatus.MISSED
                    else -> DoseStatus.PENDING
                }

                result.add(
                    ScheduledDoseItem(
                        schedule = schedule,
                        medicine = medicine,
                        patient = patient,
                        doseLog = log,
                        scheduledDateEpochDay = epochDay,
                        scheduledTimeMillis = scheduledMillis,
                        effectiveTriggerMillis = effectiveTrigger,
                        status = resolvedStatus
                    )
                )
            }
        }
        return result.sortedBy { it.effectiveTriggerMillis }
    }

    fun findNextUpcomingDose(
        medicinesWithSchedules: List<MedicineWithSchedules>,
        logsByScheduleAndDay: Map<Pair<Long, Long>, DoseLogEntity>,
        patientsById: Map<Long, PatientEntity>,
        activePatientId: Long?,
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): NextDoseInfo? {
        val todayEpochDay = epochDayFromMillis(nowMillis, timeZone)
        var bestCandidate: ScheduledDoseItem? = null
        var bestMillis = Long.MAX_VALUE

        for (dayOffset in 0L..7L) {
            val targetDay = todayEpochDay + dayOffset
            for (item in medicinesWithSchedules) {
                val medicine = item.medicine
                if (activePatientId != null && medicine.patientId != activePatientId) continue
                val patient = item.patient ?: patientsById[medicine.patientId] ?: continue

                for (schedule in item.schedules) {
                    if (!isMedicineScheduledOnDay(medicine, schedule, targetDay)) continue
                    val log = logsByScheduleAndDay[schedule.id to targetDay]
                    if (log != null && (log.status == DoseStatus.TAKEN || log.status == DoseStatus.SKIPPED || log.status == DoseStatus.MISSED)) {
                        continue
                    }
                    val baseTrigger = triggerMillisForEpochDay(targetDay, schedule.hour, schedule.minute, timeZone)
                    val effectiveTrigger = if (log?.status == DoseStatus.SNOOZED && log.snoozeUntilMillis != null) {
                        log.snoozeUntilMillis
                    } else {
                        baseTrigger
                    }
                    if (effectiveTrigger > nowMillis && effectiveTrigger < bestMillis) {
                        bestMillis = effectiveTrigger
                        bestCandidate = ScheduledDoseItem(
                            schedule = schedule,
                            medicine = medicine,
                            patient = patient,
                            doseLog = log,
                            scheduledDateEpochDay = targetDay,
                            scheduledTimeMillis = baseTrigger,
                            effectiveTriggerMillis = effectiveTrigger,
                            status = log?.status ?: DoseStatus.PENDING
                        )
                    }
                }
            }
            if (bestCandidate != null) break
        }

        return bestCandidate?.let {
            NextDoseInfo(item = it, millisRemaining = (bestMillis - nowMillis).coerceAtLeast(0L))
        }
    }

    const val DEFAULT_ACTION_LEAD_MINUTES = 15L

    fun isActionWindowOpen(
        dose: ScheduledDoseItem,
        nowMillis: Long,
        leadMinutes: Long = DEFAULT_ACTION_LEAD_MINUTES
    ): Boolean {
        return when (dose.status) {
            DoseStatus.TAKEN, DoseStatus.SKIPPED -> false
            DoseStatus.SNOOZED, DoseStatus.MISSED -> true
            DoseStatus.PENDING -> nowMillis >= dose.effectiveTriggerMillis - TimeUnit.MINUTES.toMillis(leadMinutes)
        }
    }

    fun doseAlarmRequestCode(scheduleId: Long): Int =
        (scheduleId.toInt() and 0x00FFFFFF) or 0x10000000

    fun missedCheckRequestCode(scheduleId: Long): Int =
        (scheduleId.toInt() and 0x00FFFFFF) or 0x20000000

    fun snoozeAlarmRequestCode(scheduleId: Long): Int =
        (scheduleId.toInt() and 0x00FFFFFF) or 0x30000000
}
