package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.AppLanguage
import com.example.data.model.DoseStatus
import com.example.data.model.MedicineWithSchedules
import com.example.data.model.PatientEntity
import com.example.data.model.ScheduledDoseItem
import com.example.domain.AdherenceSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfReportGenerator {

    suspend fun generateReportToUri(
        context: Context,
        outputUri: Uri,
        patientName: String,
        summary: AdherenceSummary,
        medicines: List<MedicineWithSchedules>,
        dosesForSelectedDay: List<ScheduledDoseItem>,
        selectedEpochDay: Long,
        language: AppLanguage
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val doc = buildDocument(
                patientName = patientName,
                summary = summary,
                medicines = medicines,
                dosesForSelectedDay = dosesForSelectedDay,
                selectedEpochDay = selectedEpochDay,
                language = language
            )
            try {
                context.contentResolver.openOutputStream(outputUri)?.use { out ->
                    doc.writeTo(out)
                } ?: error("Unable to open destination file")
            } finally {
                doc.close()
            }
        }
    }

    suspend fun generateShareableReportFile(
        context: Context,
        patientName: String,
        summary: AdherenceSummary,
        medicines: List<MedicineWithSchedules>,
        dosesForSelectedDay: List<ScheduledDoseItem>,
        selectedEpochDay: Long,
        language: AppLanguage
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val doc = buildDocument(
                patientName = patientName,
                summary = summary,
                medicines = medicines,
                dosesForSelectedDay = dosesForSelectedDay,
                selectedEpochDay = selectedEpochDay,
                language = language
            )
            val reportsDir = File(context.filesDir, "reports").apply { mkdirs() }
            val file = File(reportsDir, "medi_alert_report_${System.currentTimeMillis()}.pdf")
            try {
                FileOutputStream(file).use { out ->
                    doc.writeTo(out)
                }
            } finally {
                doc.close()
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
    }

    private fun buildDocument(
        patientName: String,
        summary: AdherenceSummary,
        medicines: List<MedicineWithSchedules>,
        dosesForSelectedDay: List<ScheduledDoseItem>,
        selectedEpochDay: Long,
        language: AppLanguage
    ): PdfDocument {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas

        val headerBgPaint = Paint().apply { color = Color.parseColor("#0A6C74") }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#D8F3F5")
            textSize = 11f
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#0A6C74")
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#1D2D30")
            textSize = 11f
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.parseColor("#546B70")
            textSize = 10f
            isAntiAlias = true
        }
        val cardBgPaint = Paint().apply { color = Color.parseColor("#F1F8F9") }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#CFE4E6")
            strokeWidth = 1f
        }

        // Top Banner
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 96f, headerBgPaint)
        canvas.drawText("Medi Alert — Medication & Adherence Report", 32f, 44f, titlePaint)
        val rangeText = "${TimeFormatter.formatEpochDay(summary.startEpochDay, AppLanguage.ENGLISH)} - ${
            TimeFormatter.formatEpochDay(summary.endEpochDay, AppLanguage.ENGLISH)
        }"
        canvas.drawText("Patient: $patientName   |   Period: ${summary.period.name} ($rangeText)", 32f, 70f, subtitlePaint)

        var y = 128f

        fun ensureSpace(neededHeight: Float) {
            if (y + neededHeight > pageHeight - 48f) {
                doc.finishPage(page)
                pageNumber++
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = 48f
            }
        }

        // Adherence Summary Box
        canvas.drawRoundRect(32f, y, pageWidth - 32f, y + 88f, 12f, 12f, cardBgPaint)
        canvas.drawText("Adherence Summary", 48f, y + 26f, sectionPaint)
        canvas.drawText(
            "Adherence Rate: ${summary.adherencePercent}%    |    Total Scheduled: ${summary.totalScheduled}",
            48f,
            y + 48f,
            bodyPaint
        )
        canvas.drawText(
            "Taken: ${summary.takenCount}    |    Skipped: ${summary.skippedCount}    |    Missed: ${summary.missedCount}    |    Pending: ${summary.pendingCount}",
            48f,
            y + 70f,
            bodyPaint
        )
        y += 116f

        // Active Medications
        canvas.drawText("Medications (${medicines.size})", 32f, y, sectionPaint)
        y += 18f
        canvas.drawLine(32f, y, pageWidth - 32f, y, dividerPaint)
        y += 18f

        if (medicines.isEmpty()) {
            canvas.drawText("No medications recorded.", 32f, y, mutedPaint)
            y += 24f
        } else {
            for (item in medicines) {
                ensureSpace(44f)
                val med = item.medicine
                val times = item.schedules.joinToString(", ") {
                    TimeFormatter.formatTime(it.hour, it.minute, AppLanguage.ENGLISH)
                }.ifBlank { "No active schedule" }
                val stockText = "Stock: ${TimeFormatter.formatNumber(med.stockCount)} ${med.dosageUnit}"
                canvas.drawText(
                    "• ${med.name} (${med.type.name}) — ${med.dosageAmount} ${med.dosageUnit} [${med.mealTiming.name}]",
                    36f,
                    y,
                    bodyPaint
                )
                y += 16f
                canvas.drawText("  Times: $times  |  $stockText", 36f, y, mutedPaint)
                y += 22f
            }
        }

        y += 12f
        ensureSpace(64f)
        val dayTitle = TimeFormatter.formatEpochDay(selectedEpochDay, AppLanguage.ENGLISH)
        canvas.drawText("Schedule Snapshot ($dayTitle)", 32f, y, sectionPaint)
        y += 18f
        canvas.drawLine(32f, y, pageWidth - 32f, y, dividerPaint)
        y += 18f

        if (dosesForSelectedDay.isEmpty()) {
            canvas.drawText("No scheduled doses for this date.", 32f, y, mutedPaint)
        } else {
            for (dose in dosesForSelectedDay) {
                ensureSpace(26f)
                val timeStr = TimeFormatter.formatTime(dose.schedule.hour, dose.schedule.minute, AppLanguage.ENGLISH)
                val statusLabel = when (dose.status) {
                    DoseStatus.TAKEN -> "TAKEN"
                    DoseStatus.SKIPPED -> "SKIPPED"
                    DoseStatus.MISSED -> "MISSED"
                    DoseStatus.SNOOZED -> "SNOOZED"
                    DoseStatus.PENDING -> "PENDING"
                }
                canvas.drawText(
                    "$timeStr  —  ${dose.medicine.name} (${dose.medicine.dosageAmount} ${dose.medicine.dosageUnit})  [$statusLabel]",
                    36f,
                    y,
                    bodyPaint
                )
                y += 20f
            }
        }

        doc.finishPage(page)
        return doc
    }
}
