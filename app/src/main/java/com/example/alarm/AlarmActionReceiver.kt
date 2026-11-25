package com.example.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.MediAlertDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.MedicationRepository
import com.example.widget.MediAlertWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val scheduleId = intent.getLongExtra(AlarmConstants.EXTRA_SCHEDULE_ID, -1L)
        val medicineId = intent.getLongExtra(AlarmConstants.EXTRA_MEDICINE_ID, -1L)
        val patientId = intent.getLongExtra(AlarmConstants.EXTRA_PATIENT_ID, -1L)
        val epochDay = intent.getLongExtra(AlarmConstants.EXTRA_EPOCH_DAY, 0L)
        val scheduledMillis = intent.getLongExtra(AlarmConstants.EXTRA_SCHEDULED_MILLIS, System.currentTimeMillis())
        val notificationId = intent.getIntExtra(
            AlarmConstants.EXTRA_NOTIFICATION_ID,
            NotificationHelper.alarmNotificationId(scheduleId)
        )

        if (scheduleId <= 0L || medicineId <= 0L) return

        // Stop ringing immediately
        context.stopService(Intent(context, AlarmService::class.java))
        if (notificationId > 0) {
            NotificationHelper.cancelNotification(context, notificationId)
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val db = MediAlertDatabase.getInstance(context)
                val prefsRepo = UserPreferencesRepository(context)
                val prefs = prefsRepo.getPreferencesOnce()
                val repo = MedicationRepository(db)
                val scheduler = AlarmScheduler(context, db, prefsRepo)

                when (action) {
                    AlarmConstants.ACTION_MARK_TAKEN -> {
                        val outcome = repo.markDoseTaken(
                            medicineId = medicineId,
                            scheduleId = scheduleId,
                            patientId = patientId,
                            epochDay = epochDay,
                            scheduledMillis = scheduledMillis
                        )
                        outcome.lowStockMedicine?.let { lowMed ->
                            val patient = outcome.patient ?: db.patientDao().getAllPatientsOnce().firstOrNull()
                            if (patient != null) {
                                NotificationHelper.showLowStockNotification(
                                    context = context,
                                    medicine = lowMed,
                                    patient = patient,
                                    language = prefs.language
                                )
                            }
                        }
                        scheduler.rescheduleAll()
                    }
                    AlarmConstants.ACTION_MARK_SKIPPED -> {
                        repo.markDoseSkipped(
                            medicineId = medicineId,
                            scheduleId = scheduleId,
                            patientId = patientId,
                            epochDay = epochDay,
                            scheduledMillis = scheduledMillis
                        )
                        scheduler.rescheduleAll()
                    }
                    AlarmConstants.ACTION_SNOOZE_5,
                    AlarmConstants.ACTION_SNOOZE_10,
                    AlarmConstants.ACTION_SNOOZE_15 -> {
                        val minutes = when (action) {
                            AlarmConstants.ACTION_SNOOZE_5 -> 5
                            AlarmConstants.ACTION_SNOOZE_15 -> 15
                            else -> 10
                        }
                        val log = repo.snoozeDose(
                            medicineId = medicineId,
                            scheduleId = scheduleId,
                            patientId = patientId,
                            epochDay = epochDay,
                            scheduledMillis = scheduledMillis,
                            snoozeMinutes = minutes
                        )
                        val medicine = db.medicineDao().getMedicineById(medicineId)
                        val schedule = db.scheduleDao().getScheduleById(scheduleId)
                        val snoozeUntil = log.snoozeUntilMillis
                        if (medicine != null && schedule != null && snoozeUntil != null) {
                            scheduler.scheduleSnoozeAlarm(
                                medicine = medicine,
                                schedule = schedule,
                                snoozeUntilMillis = snoozeUntil,
                                epochDay = epochDay,
                                autoMissedWindowMinutes = prefs.autoMissedWindowMinutes
                            )
                        }
                    }
                }
                MediAlertWidgetProvider.requestWidgetUpdate(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun createActionIntent(
            context: Context,
            action: String,
            scheduleId: Long,
            medicineId: Long,
            patientId: Long,
            epochDay: Long,
            scheduledMillis: Long,
            notificationId: Int
        ): PendingIntent {
            val intent = Intent(context, AlarmActionReceiver::class.java).apply {
                this.action = action
                putExtra(AlarmConstants.EXTRA_SCHEDULE_ID, scheduleId)
                putExtra(AlarmConstants.EXTRA_MEDICINE_ID, medicineId)
                putExtra(AlarmConstants.EXTRA_PATIENT_ID, patientId)
                putExtra(AlarmConstants.EXTRA_EPOCH_DAY, epochDay)
                putExtra(AlarmConstants.EXTRA_SCHEDULED_MILLIS, scheduledMillis)
                putExtra(AlarmConstants.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val requestCode = (scheduleId.toInt() * 31) + action.hashCode()
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
