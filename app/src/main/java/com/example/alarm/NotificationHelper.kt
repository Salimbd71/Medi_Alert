package com.example.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.MealTiming
import com.example.data.model.MedicineEntity
import com.example.data.model.PatientEntity
import com.example.data.model.ScheduleEntity
import com.example.util.TimeFormatter

object NotificationHelper {

    const val CHANNEL_ALARM_ID = "medi_alert_alarm_channel"
    const val CHANNEL_SERVICE_ID = "medi_alert_alarm_service_channel"
    const val CHANNEL_ALERTS_ID = "medi_alert_missed_refill_channel"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM_ID,
            context.getString(R.string.notification_channel_alarm_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_alarm_desc)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
            setSound(
                null,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE_ID,
            context.getString(R.string.notification_channel_alarm_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_alarm_desc)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val alertsChannel = NotificationChannel(
            CHANNEL_ALERTS_ID,
            context.getString(R.string.notification_channel_missed_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_missed_desc)
            enableVibration(true)
        }

        manager.createNotificationChannel(alarmChannel)
        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(alertsChannel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun buildAlarmForegroundNotification(
        context: Context,
        scheduleId: Long,
        medicine: MedicineEntity,
        patient: PatientEntity,
        schedule: ScheduleEntity,
        epochDay: Long,
        scheduledMillis: Long,
        language: AppLanguage
    ): Notification {
        createChannels(context)
        val notificationId = alarmNotificationId(scheduleId)

        val fullScreenIntent = Intent(context, AlarmFullScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmConstants.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(AlarmConstants.EXTRA_MEDICINE_ID, medicine.id)
            putExtra(AlarmConstants.EXTRA_PATIENT_ID, patient.id)
            putExtra(AlarmConstants.EXTRA_EPOCH_DAY, epochDay)
            putExtra(AlarmConstants.EXTRA_SCHEDULED_MILLIS, scheduledMillis)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val takenIntent = AlarmActionReceiver.createActionIntent(
            context = context,
            action = AlarmConstants.ACTION_MARK_TAKEN,
            scheduleId = scheduleId,
            medicineId = medicine.id,
            patientId = patient.id,
            epochDay = epochDay,
            scheduledMillis = scheduledMillis,
            notificationId = notificationId
        )
        val snoozeIntent = AlarmActionReceiver.createActionIntent(
            context = context,
            action = AlarmConstants.ACTION_SNOOZE_10,
            scheduleId = scheduleId,
            medicineId = medicine.id,
            patientId = patient.id,
            epochDay = epochDay,
            scheduledMillis = scheduledMillis,
            notificationId = notificationId
        )
        val skipIntent = AlarmActionReceiver.createActionIntent(
            context = context,
            action = AlarmConstants.ACTION_MARK_SKIPPED,
            scheduleId = scheduleId,
            medicineId = medicine.id,
            patientId = patient.id,
            epochDay = epochDay,
            scheduledMillis = scheduledMillis,
            notificationId = notificationId
        )

        val takenLabel = if (language == AppLanguage.BANGLA) "খেয়েছি" else "Taken"
        val snoozeLabel = if (language == AppLanguage.BANGLA) "স্নুজ ১০মি" else "Snooze 10m"
        val skipLabel = if (language == AppLanguage.BANGLA) "বাদ দিন" else "Skip"

        val mealText = formatMealTiming(medicine.mealTiming, language)
        val timeText = TimeFormatter.formatTime(schedule.hour, schedule.minute, language)
        val title = if (language == AppLanguage.BANGLA) {
            "ওষুধের সময়: ${medicine.name}"
        } else {
            "Medicine Time: ${medicine.name}"
        }
        val body = "${patient.name} • ${medicine.dosageAmount} ${medicine.dosageUnit} • $mealText ($timeText)"

        val compactView = RemoteViews(context.packageName, R.layout.notification_alarm_compact).apply {
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_body, body)
        }
        val actionsView = RemoteViews(context.packageName, R.layout.notification_alarm_actions).apply {
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_body, body)
            setTextViewText(R.id.notif_btn_taken, takenLabel)
            setTextViewText(R.id.notif_btn_snooze, snoozeLabel)
            setTextViewText(R.id.notif_btn_skip, skipLabel)
            setOnClickPendingIntent(R.id.notif_btn_taken, takenIntent)
            setOnClickPendingIntent(R.id.notif_btn_snooze, snoozeIntent)
            setOnClickPendingIntent(R.id.notif_btn_skip, skipIntent)
        }

        return NotificationCompat.Builder(context, CHANNEL_SERVICE_ID)
            .setSmallIcon(R.drawable.ic_stat_medi_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(compactView)
            .setCustomBigContentView(actionsView)
            .setCustomHeadsUpContentView(actionsView)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()
    }

    fun showMissedDoseNotification(
        context: Context,
        scheduleId: Long,
        medicine: MedicineEntity,
        patient: PatientEntity,
        schedule: ScheduleEntity,
        epochDay: Long,
        scheduledMillis: Long,
        language: AppLanguage
    ) {
        if (!hasNotificationPermission(context)) return
        createChannels(context)

        val notifId = missedNotificationId(scheduleId)
        val openAppIntent = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val takenIntent = AlarmActionReceiver.createActionIntent(
            context = context,
            action = AlarmConstants.ACTION_MARK_TAKEN,
            scheduleId = scheduleId,
            medicineId = medicine.id,
            patientId = patient.id,
            epochDay = epochDay,
            scheduledMillis = scheduledMillis,
            notificationId = notifId
        )
        val snoozeIntent = AlarmActionReceiver.createActionIntent(
            context = context,
            action = AlarmConstants.ACTION_SNOOZE_10,
            scheduleId = scheduleId,
            medicineId = medicine.id,
            patientId = patient.id,
            epochDay = epochDay,
            scheduledMillis = scheduledMillis,
            notificationId = notifId
        )

        val takenLabel = if (language == AppLanguage.BANGLA) "খেয়েছি" else "Taken"
        val snoozeLabel = if (language == AppLanguage.BANGLA) "স্নুজ ১০মি" else "Snooze 10m"

        val timeText = TimeFormatter.formatTime(schedule.hour, schedule.minute, language)
        val title = if (language == AppLanguage.BANGLA) {
            "মিসড ডোজ সতর্কতা: ${medicine.name}"
        } else {
            "Missed Dose Alert: ${medicine.name}"
        }
        val body = if (language == AppLanguage.BANGLA) {
            "${patient.name}-এর $timeText-এর ডোজটি এখনো নেওয়া হয়নি।"
        } else {
            "${patient.name} missed the $timeText dose (${medicine.dosageAmount} ${medicine.dosageUnit})."
        }

        val compactView = RemoteViews(context.packageName, R.layout.notification_alarm_compact).apply {
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_body, body)
        }
        val actionsView = RemoteViews(context.packageName, R.layout.notification_alarm_actions).apply {
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_body, body)
            setTextViewText(R.id.notif_btn_taken, takenLabel)
            setTextViewText(R.id.notif_btn_snooze, snoozeLabel)
            setViewVisibility(R.id.notif_btn_skip, View.GONE)
            setOnClickPendingIntent(R.id.notif_btn_taken, takenIntent)
            setOnClickPendingIntent(R.id.notif_btn_snooze, snoozeIntent)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_stat_medi_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(compactView)
            .setCustomBigContentView(actionsView)
            .setCustomHeadsUpContentView(actionsView)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }

    fun showLowStockNotification(
        context: Context,
        medicine: MedicineEntity,
        patient: PatientEntity,
        language: AppLanguage
    ) {
        if (!hasNotificationPermission(context)) return
        createChannels(context)

        val openAppIntent = PendingIntent.getActivity(
            context,
            lowStockNotificationId(medicine.id),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remaining = TimeFormatter.formatNumber(medicine.stockCount)
        val title = if (language == AppLanguage.BANGLA) {
            "ওষুধ রিফিল সতর্কতা: ${medicine.name}"
        } else {
            "Low Stock Refill Alert: ${medicine.name}"
        }
        val body = if (language == AppLanguage.BANGLA) {
            "${patient.name}-এর জন্য মাত্র $remaining ${medicine.dosageUnit} অবশিষ্ট আছে।"
        } else {
            "Only $remaining ${medicine.dosageUnit} left for ${patient.name}. Time to refill!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_stat_medi_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .build()

        NotificationManagerCompat.from(context).notify(lowStockNotificationId(medicine.id), notification)
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun alarmNotificationId(scheduleId: Long): Int = 10_000 + (scheduleId.toInt() and 0xFFFF)
    fun missedNotificationId(scheduleId: Long): Int = 30_000 + (scheduleId.toInt() and 0xFFFF)
    fun lowStockNotificationId(medicineId: Long): Int = 50_000 + (medicineId.toInt() and 0xFFFF)

    fun formatMealTiming(timing: MealTiming, language: AppLanguage): String {
        return when (timing) {
            MealTiming.BEFORE_FOOD -> if (language == AppLanguage.BANGLA) "খাবারের আগে" else "Before Food"
            MealTiming.AFTER_FOOD -> if (language == AppLanguage.BANGLA) "খাবারের পরে" else "After Food"
            MealTiming.WITH_FOOD -> if (language == AppLanguage.BANGLA) "খাবারের সাথে" else "With Food"
            MealTiming.ANYTIME -> if (language == AppLanguage.BANGLA) "যেকোনো সময়" else "Anytime"
        }
    }
}

object AlarmConstants {
    const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    const val EXTRA_MEDICINE_ID = "extra_medicine_id"
    const val EXTRA_PATIENT_ID = "extra_patient_id"
    const val EXTRA_EPOCH_DAY = "extra_epoch_day"
    const val EXTRA_SCHEDULED_MILLIS = "extra_scheduled_millis"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_IS_MISSED_CHECK = "extra_is_missed_check"

    const val ACTION_TRIGGER_DOSE = "com.slm.medialert.ACTION_TRIGGER_DOSE"
    const val ACTION_CHECK_MISSED = "com.slm.medialert.ACTION_CHECK_MISSED"
    const val ACTION_MARK_TAKEN = "com.slm.medialert.ACTION_MARK_TAKEN"
    const val ACTION_MARK_SKIPPED = "com.slm.medialert.ACTION_MARK_SKIPPED"
    const val ACTION_SNOOZE_5 = "com.slm.medialert.ACTION_SNOOZE_5"
    const val ACTION_SNOOZE_10 = "com.slm.medialert.ACTION_SNOOZE_10"
    const val ACTION_SNOOZE_15 = "com.slm.medialert.ACTION_SNOOZE_15"
    const val ACTION_STOP_SERVICE = "com.slm.medialert.ACTION_STOP_SERVICE"
}
