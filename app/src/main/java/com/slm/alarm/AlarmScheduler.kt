package com.slm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.slm.MainActivity
import com.slm.data.local.MediAlertDatabase
import com.slm.data.local.UserPreferencesRepository
import com.slm.data.model.DoseStatus
import com.slm.data.model.MedicineEntity
import com.slm.data.model.ScheduleEntity
import com.slm.domain.DoseSchedulerCalculator
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class AlarmScheduler(
    private val context: Context,
    private val database: MediAlertDatabase,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    suspend fun rescheduleAll(nowMillis: Long = System.currentTimeMillis()) {
        val allSchedules = database.scheduleDao().getAllSchedulesOnce()
        // Cancel all known schedule alarm slots first to prevent obsolete/duplicate alarms.
        allSchedules.forEach { schedule ->
            cancelScheduleAlarms(schedule.id)
        }

        val prefs = preferencesRepository.getPreferencesOnce()
        val timeZone = TimeZone.getDefault()
        val todayEpochDay = DoseSchedulerCalculator.epochDayFromMillis(nowMillis, timeZone)
        val todayLogs = database.doseLogDao().getLogsForDayOnce(todayEpochDay).associateBy { it.scheduleId }
        val activeMedicines = database.medicineDao().getActiveMedicinesWithSchedulesOnce()

        for (medWithSchedules in activeMedicines) {
            val medicine = medWithSchedules.medicine
            if (!medicine.isActive) continue
            for (schedule in medWithSchedules.schedules) {
                if (!schedule.isEnabled) continue
                val todayLog = todayLogs[schedule.id]
                val handledToday = todayLog != null && (
                    todayLog.status == DoseStatus.TAKEN ||
                        todayLog.status == DoseStatus.SKIPPED ||
                        todayLog.status == DoseStatus.MISSED
                    )
                val snoozeUntil = if (todayLog?.status == DoseStatus.SNOOZED) todayLog.snoozeUntilMillis else null

                val nextTrigger = DoseSchedulerCalculator.computeNextTriggerMillis(
                    medicine = medicine,
                    schedule = schedule,
                    nowMillis = nowMillis,
                    alreadyHandledToday = handledToday,
                    snoozeUntilMillis = snoozeUntil,
                    timeZone = timeZone
                ) ?: continue

                val triggerEpochDay = DoseSchedulerCalculator.epochDayFromMillis(nextTrigger, timeZone)
                scheduleDoseAlarm(
                    medicine = medicine,
                    schedule = schedule,
                    triggerAtMillis = nextTrigger,
                    epochDay = triggerEpochDay,
                    isSnooze = snoozeUntil != null && snoozeUntil > nowMillis
                )

                // Schedule missed-dose verification check if not already handled
                val missedCheckAt = nextTrigger + TimeUnit.MINUTES.toMillis(prefs.autoMissedWindowMinutes.toLong())
                if (missedCheckAt > nowMillis) {
                    scheduleMissedCheckAlarm(
                        medicine = medicine,
                        schedule = schedule,
                        checkAtMillis = missedCheckAt,
                        epochDay = triggerEpochDay,
                        scheduledMillis = nextTrigger
                    )
                }
            }
        }
    }

    fun scheduleDoseAlarm(
        medicine: MedicineEntity,
        schedule: ScheduleEntity,
        triggerAtMillis: Long,
        epochDay: Long,
        isSnooze: Boolean = false
    ) {
        val requestCode = if (isSnooze) {
            DoseSchedulerCalculator.snoozeAlarmRequestCode(schedule.id)
        } else {
            DoseSchedulerCalculator.doseAlarmRequestCode(schedule.id)
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmConstants.ACTION_TRIGGER_DOSE
            putExtra(AlarmConstants.EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(AlarmConstants.EXTRA_MEDICINE_ID, medicine.id)
            putExtra(AlarmConstants.EXTRA_PATIENT_ID, medicine.patientId)
            putExtra(AlarmConstants.EXTRA_EPOCH_DAY, epochDay)
            putExtra(AlarmConstants.EXTRA_SCHEDULED_MILLIS, triggerAtMillis)
            putExtra(AlarmConstants.EXTRA_IS_MISSED_CHECK, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showAppIntent = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleReliable(triggerAtMillis, pendingIntent, showAppIntent)
    }

    fun scheduleSnoozeAlarm(
        medicine: MedicineEntity,
        schedule: ScheduleEntity,
        snoozeUntilMillis: Long,
        epochDay: Long,
        autoMissedWindowMinutes: Int
    ) {
        cancelScheduleAlarms(schedule.id)
        scheduleDoseAlarm(
            medicine = medicine,
            schedule = schedule,
            triggerAtMillis = snoozeUntilMillis,
            epochDay = epochDay,
            isSnooze = true
        )
        scheduleMissedCheckAlarm(
            medicine = medicine,
            schedule = schedule,
            checkAtMillis = snoozeUntilMillis + TimeUnit.MINUTES.toMillis(autoMissedWindowMinutes.toLong()),
            epochDay = epochDay,
            scheduledMillis = snoozeUntilMillis
        )
    }

    fun scheduleMissedCheckAlarm(
        medicine: MedicineEntity,
        schedule: ScheduleEntity,
        checkAtMillis: Long,
        epochDay: Long,
        scheduledMillis: Long
    ) {
        val requestCode = DoseSchedulerCalculator.missedCheckRequestCode(schedule.id)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmConstants.ACTION_CHECK_MISSED
            putExtra(AlarmConstants.EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(AlarmConstants.EXTRA_MEDICINE_ID, medicine.id)
            putExtra(AlarmConstants.EXTRA_PATIENT_ID, medicine.patientId)
            putExtra(AlarmConstants.EXTRA_EPOCH_DAY, epochDay)
            putExtra(AlarmConstants.EXTRA_SCHEDULED_MILLIS, scheduledMillis)
            putExtra(AlarmConstants.EXTRA_IS_MISSED_CHECK, true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, checkAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, checkAtMillis, pendingIntent)
        }
    }

    fun cancelScheduleAlarms(scheduleId: Long) {
        val codes = listOf(
            DoseSchedulerCalculator.doseAlarmRequestCode(scheduleId) to AlarmConstants.ACTION_TRIGGER_DOSE,
            DoseSchedulerCalculator.snoozeAlarmRequestCode(scheduleId) to AlarmConstants.ACTION_TRIGGER_DOSE,
            DoseSchedulerCalculator.missedCheckRequestCode(scheduleId) to AlarmConstants.ACTION_CHECK_MISSED
        )
        for ((code, actionName) in codes) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = actionName
            }
            val existing = PendingIntent.getBroadcast(
                context,
                code,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (existing != null) {
                alarmManager.cancel(existing)
                existing.cancel()
            }
        }
    }

    private fun scheduleReliable(
        triggerAtMillis: Long,
        operation: PendingIntent,
        showIntent: PendingIntent
    ) {
        if (canScheduleExactAlarms()) {
            val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
            alarmManager.setAlarmClock(clockInfo, operation)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        }
    }
}
