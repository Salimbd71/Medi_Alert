package com.slm.util

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

data class PermissionHealthState(
    val hasNotificationPermission: Boolean,
    val canScheduleExactAlarms: Boolean,
    val isIgnoringBatteryOptimizations: Boolean,
    val canUseFullScreenIntent: Boolean
) {
    val allOptimal: Boolean
        get() = hasNotificationPermission && canScheduleExactAlarms && isIgnoringBatteryOptimizations && canUseFullScreenIntent
}

object PermissionHelper {

    fun checkHealth(context: Context): PermissionHealthState {
        val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val ignoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }

        val fullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.canUseFullScreenIntent()
        } else {
            true
        }

        return PermissionHealthState(
            hasNotificationPermission = hasNotification,
            canScheduleExactAlarms = exactAlarms,
            isIgnoringBatteryOptimizations = ignoringBattery,
            canUseFullScreenIntent = fullScreen
        )
    }

    fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    fun canResolveIntent(context: Context, intent: Intent): Boolean {
        return intent.resolveActivity(context.packageManager) != null
    }

    fun buildRequestIgnoreBatteryOptimizationsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun buildAppDetailsSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun buildBatteryOptimizationListIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }

    fun openExactAlarmSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            buildAppDetailsSettingsIntent(context)
        }
        launchSafely(context, intent)
    }

    fun openFullScreenIntentSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            buildAppDetailsSettingsIntent(context)
        }
        launchSafely(context, intent)
    }

    fun openBatteryOptimizationSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activity = findActivity(context)
            val directIntent = buildRequestIgnoreBatteryOptimizationsIntent(context)
            if (canResolveIntent(context, directIntent)) {
                val started = runCatching {
                    if (activity != null) {
                        activity.startActivity(directIntent)
                    } else {
                        directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(directIntent)
                    }
                }.isSuccess
                if (started) return
            }
            val appDetails = buildAppDetailsSettingsIntent(context)
            if (canResolveIntent(context, appDetails)) {
                launchSafely(context, appDetails)
            } else {
                launchSafely(context, buildBatteryOptimizationListIntent())
            }
        } else {
            launchSafely(context, buildAppDetailsSettingsIntent(context))
        }
    }

    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            buildAppDetailsSettingsIntent(context)
        }
        launchSafely(context, intent)
    }

    private fun launchSafely(context: Context, intent: Intent) {
        val activity = findActivity(context)
        runCatching {
            if (activity != null) {
                activity.startActivity(intent)
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }.onFailure {
            runCatching {
                val fallback = buildAppDetailsSettingsIntent(context)
                if (activity != null) {
                    activity.startActivity(fallback)
                } else {
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(fallback)
                }
            }
        }
    }
}
