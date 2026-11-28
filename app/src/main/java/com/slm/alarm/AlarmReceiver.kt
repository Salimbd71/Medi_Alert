package com.slm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.slm.data.local.MediAlertDatabase
import com.slm.data.local.UserPreferencesRepository
import com.slm.data.model.DoseStatus
import com.slm.data.repository.MedicationRepository
import com.slm.widget.MediAlertWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(AlarmConstants.EXTRA_SCHEDULE_ID, -1L)
        val medicineId = intent.getLongExtra(AlarmConstants.EXTRA_MEDICINE_ID, -1L)
        val patientId = intent.getLongExtra(AlarmConstants.EXTRA_PATIENT_ID, -1L)
        val epochDay = intent.getLongExtra(AlarmConstants.EXTRA_EPOCH_DAY, 0L)
        val scheduledMillis = intent.getLongExtra(AlarmConstants.EXTRA_SCHEDULED_MILLIS, System.currentTimeMillis())
        val isMissedCheck = intent.getBooleanExtra(AlarmConstants.EXTRA_IS_MISSED_CHECK, false) ||
            intent.action == AlarmConstants.ACTION_CHECK_MISSED

        if (scheduleId <= 0L || medicineId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val db = MediAlertDatabase.getInstance(context)
                val prefsRepo = UserPreferencesRepository(context)
                val prefs = prefsRepo.getPreferencesOnce()
                val repo = MedicationRepository(db)

                val medicine = db.medicineDao().getMedicineById(medicineId) ?: return@launch
                val schedule = db.scheduleDao().getScheduleById(scheduleId) ?: return@launch
                val patient = db.patientDao().getPatientById(patientId)
                    ?: db.patientDao().getAllPatientsOnce().firstOrNull()
                    ?: return@launch

                if (!medicine.isActive || !schedule.isEnabled) return@launch

                val existingLog = db.doseLogDao().getLogForScheduleAndDay(scheduleId, epochDay)
                val alreadyResolved = existingLog != null && (
                    existingLog.status == DoseStatus.TAKEN ||
                        existingLog.status == DoseStatus.SKIPPED ||
                        existingLog.status == DoseStatus.MISSED
                    )

                if (isMissedCheck) {
                    if (!alreadyResolved) {
                        val marked = repo.markDoseMissedIfPending(
                            medicineId = medicineId,
                            scheduleId = scheduleId,
                            patientId = patient.id,
                            epochDay = epochDay,
                            scheduledMillis = scheduledMillis
                        )
                        if (marked != null) {
                            // Silence ringing service if still active
                            context.stopService(Intent(context, AlarmService::class.java))
                            NotificationHelper.cancelNotification(
                                context,
                                NotificationHelper.alarmNotificationId(scheduleId)
                            )
                            NotificationHelper.showMissedDoseNotification(
                                context = context,
                                scheduleId = scheduleId,
                                medicine = medicine,
                                patient = patient,
                                schedule = schedule,
                                epochDay = epochDay,
                                scheduledMillis = scheduledMillis,
                                language = prefs.language
                            )
                            AlarmScheduler(context, db, prefsRepo).rescheduleAll()
                            MediAlertWidgetProvider.requestWidgetUpdate(context)
                        }
                    }
                } else {
                    if (alreadyResolved) {
                        // Reschedule next occurrence cleanly
                        AlarmScheduler(context, db, prefsRepo).rescheduleAll()
                        return@launch
                    }

                    val serviceIntent = Intent(context, AlarmService::class.java).apply {
                        putExtra(AlarmConstants.EXTRA_SCHEDULE_ID, scheduleId)
                        putExtra(AlarmConstants.EXTRA_MEDICINE_ID, medicineId)
                        putExtra(AlarmConstants.EXTRA_PATIENT_ID, patient.id)
                        putExtra(AlarmConstants.EXTRA_EPOCH_DAY, epochDay)
                        putExtra(AlarmConstants.EXTRA_SCHEDULED_MILLIS, scheduledMillis)
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                    MediAlertWidgetProvider.requestWidgetUpdate(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
