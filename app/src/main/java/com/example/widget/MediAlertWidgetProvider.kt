package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.alarm.AlarmActionReceiver
import com.example.alarm.AlarmConstants
import com.example.alarm.NotificationHelper
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
import java.util.concurrent.TimeUnit

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

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelScheduledWidgetRefresh(context)
    }

    companion object {
        const val ACTION_WIDGET_REFRESH = "com.slm.medialert.WIDGET_REFRESH"
        private const val WIDGET_REFRESH_REQUEST_CODE = 77_501

        fun requestWidgetUpdate(context: Context) {
            val intent = Intent(context, MediAlertWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_REFRESH
            }
            context.sendBroadcast(intent)
        }

        private fun createRefreshPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MediAlertWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_REFRESH
            }
            return PendingIntent.getBroadcast(
                context,
                WIDGET_REFRESH_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun cancelScheduledWidgetRefresh(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(createRefreshPendingIntent(context))
        }

        private fun scheduleNextWidgetRefresh(context: Context, triggerAtMillis: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = createRefreshPendingIntent(context)
            alarmManager.cancel(pendingIntent)
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
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

            val takenToday = todayDoses.count { it.status == DoseStatus.TAKEN }
            val missedToday = todayDoses.count { it.status == DoseStatus.MISSED }
            val resolvedToday = todayDoses.count {
                it.status == DoseStatus.TAKEN || it.status == DoseStatus.SKIPPED || it.status == DoseStatus.MISSED
            }
            val adherencePct = if (resolvedToday == 0) 100 else ((takenToday * 100f) / resolvedToday).toInt().coerceIn(0, 100)
            val pendingCount = todayDoses.count { it.status == DoseStatus.PENDING || it.status == DoseStatus.SNOOZED }

            val nextDose = DoseSchedulerCalculator.findNextUpcomingDose(
                medicinesWithSchedules = meds,
                logsByScheduleAndDay = logsMap,
                patientsById = patients,
                activePatientId = prefs.activePatientId,
                nowMillis = nowMillis,
                timeZone = timeZone
            )

            // Also check if there's an active overdue/pending or snoozed dose within the missed grace window
            val dueNowDose = todayDoses.firstOrNull {
                (it.status == DoseStatus.PENDING || it.status == DoseStatus.SNOOZED) &&
                    DoseSchedulerCalculator.isActionWindowOpen(it, nowMillis)
            }
            val activeHeroItem = dueNowDose ?: nextDose?.item
            val activeRemainingMillis = if (dueNowDose != null) {
                (dueNowDose.effectiveTriggerMillis - nowMillis).coerceAtLeast(0L)
            } else {
                nextDose?.millisRemaining ?: 0L
            }

            if (activeHeroItem != null) {
                val leadMillis = TimeUnit.MINUTES.toMillis(DoseSchedulerCalculator.DEFAULT_ACTION_LEAD_MINUTES)
                val revealAt = activeHeroItem.effectiveTriggerMillis - leadMillis
                val nextRefreshAt = when {
                    revealAt > nowMillis -> revealAt + 1_000L
                    activeHeroItem.effectiveTriggerMillis > nowMillis -> activeHeroItem.effectiveTriggerMillis + 1_500L
                    else -> nowMillis + TimeUnit.MINUTES.toMillis(prefs.autoMissedWindowMinutes.toLong().coerceAtLeast(5L))
                }
                scheduleNextWidgetRefresh(context, nextRefreshAt)
            } else {
                cancelScheduledWidgetRefresh(context)
            }

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_medi_alert)
                val appTitle = if (lang == AppLanguage.BANGLA) "মেডি অ্যালার্ট" else "Medi Alert"
                val adherenceText = if (lang == AppLanguage.BANGLA) {
                    "আজ ${TimeFormatter.localizeDigits(adherencePct.toString(), lang)}%"
                } else {
                    "$adherencePct% today"
                }
                val pendingBadge = if (lang == AppLanguage.BANGLA) {
                    "${TimeFormatter.localizeDigits(pendingCount.toString(), lang)}টি বাকি"
                } else {
                    "$pendingCount left"
                }
                val footerText = if (lang == AppLanguage.BANGLA) {
                    val t = TimeFormatter.localizeDigits(takenToday.toString(), lang)
                    val tot = TimeFormatter.localizeDigits(todayDoses.size.toString(), lang)
                    val m = TimeFormatter.localizeDigits(missedToday.toString(), lang)
                    "সম্পন্ন: $t/$tot • মিসড: $m"
                } else {
                    "Taken: $takenToday/${todayDoses.size} • Missed: $missedToday"
                }

                views.setTextViewText(R.id.widget_app_title, appTitle)
                views.setTextViewText(R.id.widget_adherence_badge, adherenceText)
                views.setTextViewText(R.id.widget_pending_badge, pendingBadge)
                views.setTextViewText(R.id.widget_summary_footer, footerText)

                val openAppIntent = PendingIntent.getActivity(
                    context,
                    widgetId,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, openAppIntent)

                if (activeHeroItem != null) {
                    val timeText = TimeFormatter.formatTime(activeHeroItem.schedule.hour, activeHeroItem.schedule.minute, lang)
                    val mealText = NotificationHelper.formatMealTiming(activeHeroItem.medicine.mealTiming, lang)
                    val nextTimeHeader = if (lang == AppLanguage.BANGLA) {
                        "পরবর্তী ডোজ • $timeText"
                    } else {
                        "NEXT DOSE • $timeText"
                    }

                    views.setTextViewText(R.id.widget_next_time_label, nextTimeHeader)
                    views.setTextViewText(R.id.widget_next_medicine, activeHeroItem.medicine.name)
                    views.setTextViewText(
                        R.id.widget_next_details,
                        "${activeHeroItem.patient.name} • ${activeHeroItem.medicine.dosageAmount} ${activeHeroItem.medicine.dosageUnit} • $mealText"
                    )

                    val actionWindowOpen = DoseSchedulerCalculator.isActionWindowOpen(activeHeroItem, nowMillis)
                    val captionText = if (lang == AppLanguage.BANGLA) "বাকি আছে" else "remaining"
                    views.setTextViewText(R.id.widget_countdown_caption, captionText)

                    if (activeRemainingMillis <= 0L) {
                        views.setViewVisibility(R.id.widget_countdown, View.GONE)
                        views.setViewVisibility(R.id.widget_countdown_fallback, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_countdown_caption, View.GONE)
                        views.setTextViewText(
                            R.id.widget_countdown_fallback,
                            if (lang == AppLanguage.BANGLA) "এখন সময়" else "Due Now"
                        )
                    } else if (activeRemainingMillis < TimeUnit.HOURS.toMillis(24)) {
                        views.setViewVisibility(R.id.widget_countdown, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_countdown_fallback, View.GONE)
                        views.setViewVisibility(R.id.widget_countdown_caption, View.VISIBLE)
                        views.setChronometerCountDown(R.id.widget_countdown, true)
                        views.setChronometer(
                            R.id.widget_countdown,
                            SystemClock.elapsedRealtime() + activeRemainingMillis,
                            null,
                            true
                        )
                    } else {
                        val days = TimeUnit.MILLISECONDS.toDays(activeRemainingMillis)
                        val hrs = TimeUnit.MILLISECONDS.toHours(activeRemainingMillis) % 24
                        views.setViewVisibility(R.id.widget_countdown, View.GONE)
                        views.setViewVisibility(R.id.widget_countdown_fallback, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_countdown_caption, View.VISIBLE)
                        views.setTextViewText(
                            R.id.widget_countdown_fallback,
                            TimeFormatter.localizeDigits("${days}d ${hrs}h", lang)
                        )
                    }

                    if (actionWindowOpen) {
                        val takeLabel = if (lang == AppLanguage.BANGLA) "খেয়েছি" else "Taken"
                        views.setTextViewText(R.id.widget_take_button, takeLabel)
                        views.setViewVisibility(R.id.widget_take_button, View.VISIBLE)
                        views.setTextViewText(
                            R.id.widget_status_hint,
                            if (lang == AppLanguage.BANGLA) "ডোজ নেওয়ার সময়" else "Tap Taken to log"
                        )

                        val takePendingIntent = AlarmActionReceiver.createActionIntent(
                            context = context,
                            action = AlarmConstants.ACTION_MARK_TAKEN,
                            scheduleId = activeHeroItem.schedule.id,
                            medicineId = activeHeroItem.medicine.id,
                            patientId = activeHeroItem.patient.id,
                            epochDay = activeHeroItem.scheduledDateEpochDay,
                            scheduledMillis = activeHeroItem.scheduledTimeMillis,
                            notificationId = NotificationHelper.alarmNotificationId(activeHeroItem.schedule.id)
                        )
                        views.setOnClickPendingIntent(R.id.widget_take_button, takePendingIntent)
                    } else {
                        views.setViewVisibility(R.id.widget_take_button, View.GONE)
                        views.setTextViewText(
                            R.id.widget_status_hint,
                            if (lang == AppLanguage.BANGLA) "১৫ মি. আগে বাটন খুলবে" else "Taken unlocks 15m prior"
                        )
                    }
                } else {
                    val headerText = if (lang == AppLanguage.BANGLA) "আজকের সব সম্পন্ন" else "ALL CAUGHT UP"
                    val noUpcoming = if (lang == AppLanguage.BANGLA) {
                        "আপাতত কোনো আসন্ন ডোজ নেই"
                    } else {
                        "All doses completed for now"
                    }
                    val tapHint = if (lang == AppLanguage.BANGLA) {
                        "সময়সূচী ও রিপোর্ট দেখতে ট্যাপ করুন"
                    } else {
                        "Tap to view schedule & history"
                    }
                    views.setTextViewText(R.id.widget_next_time_label, headerText)
                    views.setTextViewText(R.id.widget_next_medicine, noUpcoming)
                    views.setTextViewText(R.id.widget_next_details, tapHint)
                    views.setViewVisibility(R.id.widget_countdown, View.GONE)
                    views.setViewVisibility(R.id.widget_countdown_fallback, View.GONE)
                    views.setViewVisibility(R.id.widget_countdown_caption, View.GONE)
                    views.setViewVisibility(R.id.widget_take_button, View.GONE)
                    views.setTextViewText(
                        R.id.widget_status_hint,
                        if (lang == AppLanguage.BANGLA) "অ্যাপ খুলুন" else "Tap to open"
                    )
                }

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
