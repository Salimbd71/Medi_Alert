package com.example.util

import com.example.data.model.AppLanguage
import com.example.domain.DoseSchedulerCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object TimeFormatter {

    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun localizeDigits(input: String, language: AppLanguage): String {
        if (language != AppLanguage.BANGLA) return input
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(banglaDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatTime(hour: Int, minute: Int, language: AppLanguage): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val locale = if (language == AppLanguage.BANGLA) Locale("bn", "BD") else Locale.ENGLISH
        val pattern = "hh:mm a"
        val formatted = SimpleDateFormat(pattern, locale).format(cal.time)
        return localizeDigits(formatted, language)
    }

    fun formatEpochDay(epochDay: Long, language: AppLanguage, pattern: String = "dd MMM yyyy"): String {
        val (year, month, day) = DoseSchedulerCalculator.yearMonthDayFromEpochDay(epochDay)
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            clear()
            set(year, month, day, 12, 0, 0)
        }
        val locale = if (language == AppLanguage.BANGLA) Locale("bn", "BD") else Locale.ENGLISH
        val formatted = SimpleDateFormat(pattern, locale).format(cal.time)
        return localizeDigits(formatted, language)
    }

    fun formatCountdown(millisRemaining: Long, language: AppLanguage): String {
        val totalSeconds = (millisRemaining / 1000L).coerceAtLeast(0L)
        val hours = TimeUnit.SECONDS.toHours(totalSeconds)
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
        val seconds = totalSeconds % 60
        val raw = if (hours > 0) {
            String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
        }
        return localizeDigits(raw, language)
    }

    fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.ENGLISH, "%.1f", value)
        }
    }
}
