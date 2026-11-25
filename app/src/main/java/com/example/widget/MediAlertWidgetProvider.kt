package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.alarm.AlarmActionReceiver
import com.example.alarm.AlarmConstants
import com.example.data.local.MediAlertDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.AppLanguage
import com.example.data.model.DoseStatus
import com.example.domain.DoseSchedulerCalculator
import com.example.util.TimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.TimeZone

class MediAlertWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_REFRESH) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val manager = AppWidgetManager.getInstance(context)
                    val ids = manager.getAppWidgetIds(ComponentName(context, MediAlertWidgetProvider::class.java))
                    if (ids.isNotEmpty()) {
                        updateWidgets(context, manager, ids)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_REFRESH = "com.slm.medialert.WIDGET_REFRESH"

        fun requestWidgetUpdate(context: Context) {
            val intent = Intent(context, MediAlertWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_REFRESH
            }
            context.sendBroadcast(intent)
        }

        suspend fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val db = MediAlertDatabase.getInstance(context)
            val prefs = UserPreferencesRepository(context).getPreferencesOnce()
            val lang = prefs.language
            val nowMillis = System.currentTimeMillis()
            val timeZone = TimeZone.getDefault()
            val todayEpochDay = DoseSchedulerCalculator.epochDayFromMillis(nowMillis, timeZone)

            val patients = db.patientDao().getAllPatientsOnce().associateBy { it.id }
            val meds = db.medicineDao().getActiveMedicinesWithSchedulesOnce()
            val todayLogs = db.doseLogDao().getLogsForDayOnce(todayEpochDay)
            val tomorrowLogs = db.doseLogDao().getLogsForDayOnce(todayEpochDay + 1)

            val logsMap = (todayLogs + tomorrowLogs).associateBy { it.scheduleId to it.scheduledDateEpochDay }
            val todayDoses = DoseSchedulerCalculator.buildDosesForDate(
                epochDay = todayEpochDay,
                medicinesWithSchedules = meds,
                logsForDay = todayLogs,
                patientsById = patients,
                activePatientId = prefs.activePatientId,
                nowMillis = nowMillis,
                autoMissedWindowMinutes = prefs.autoMissedWindowMinutes,
                timeZone = timeZone
            )
            val pendingCount = todayDoses.count { it.status == DoseStatus.PENDING || it.status == DoseStatus.SNOOZED }
            val nextDose = DoseSchedulerCalculator.findNextUpcomingDose(
                medicinesWithSchedules = meds,
                logsByScheduleAndDay = logsMap,
                patientsById = patients,
                activePatientId = prefs.activePatientId,
                nowMillis = nowMillis,
                timeZone = timeZone
            )

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_medi_alert)
                val appTitle = if (lang == AppLanguage.BANGLA) "মেডি অ্যালার্ট" else "Medi Alert"
                val pendingBadge = if (lang == AppLanguage.BANGLA) {
                    "${TimeFormatter.localizeDigits(pendingCount.toString(), lang)}টি বাকি"
                } else {
                    "$pendingCount pending"
                }
                views.setTextViewText(R.id.widget_app_title, appTitle)
                views.setTextViewText(R.id.widget_pending_badge, pendingBadge)

                val openAppIntent = PendingIntent.getActivity(
                    context,
                    widgetId,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, openAppIntent)

                if (nextDose != null) {
                    val item = nextDose.item
                    val timeText = TimeFormatter.formatTime(item.schedule.hour, item.schedule.minute, lang)
                    views.setTextViewText(R.id.widget_next_medicine, "${item.medicine.name} • $timeText")
                    views.setTextViewText(
                        R.id.widget_next_details,
                        "${item.patient.name} • ${item.medicine.dosageAmount} ${item.medicine.dosageUnit}"
                    )
                    val takeLabel = if (lang == AppLanguage.BANGLA) "খাওয়া হয়েছে" else "Mark Taken"
                    views.setTextViewText(R.id.widget_take_button, takeLabel)
                    views.setViewVisibility(R.id.widget_take_button, View.VISIBLE)

                    val takePendingIntent = AlarmActionReceiver.createActionIntent(
                        context = context,
                        action = AlarmConstants.ACTION_MARK_TAKEN,
                        scheduleId = item.schedule.id,
                        medicineId = item.medicine.id,
                        patientId = item.patient.id,
                        epochDay = item.scheduledDateEpochDay,
                        scheduledMillis = item.scheduledTimeMillis,
                        notificationId = 0
                    )
                    views.setOnClickPendingIntent(R.id.widget_take_button, takePendingIntent)
                } else {
                    val noUpcoming = if (lang == AppLanguage.BANGLA) {
                        "আপাতত কোনো আসন্ন ডোজ নেই"
                    } else {
                        "All doses completed for now"
                    }
                    val tapHint = if (lang == AppLanguage.BANGLA) {
                        "সময়সূচী দেখতে ট্যাপ করুন"
                    } else {
                        "Tap to manage today's schedule"
                    }
                    views.setTextViewText(R.id.widget_next_medicine, noUpcoming)
                    views.setTextViewText(R.id.widget_next_details, tapHint)
                    views.setViewVisibility(R.id.widget_take_button, View.GONE)
                }

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
