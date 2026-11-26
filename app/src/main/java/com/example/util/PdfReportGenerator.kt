package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.DoseStatus
import com.example.data.model.MedicineWithSchedules
import com.example.data.model.ScheduledDoseItem
import com.example.domain.AdherenceSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SavedPdfReport(
    val uri: Uri,
    val fileName: String,
    val displayPath: String
)

object PdfReportGenerator {

    suspend fun saveReportToDownloads(
        context: Context,
        patientName: String,
        summary: AdherenceSummary,
        medicines: List<MedicineWithSchedules>,
        dosesForSelectedDay: List<ScheduledDoseItem>,
        selectedEpochDay: Long,
        language: AppLanguage
    ): Result<SavedPdfReport> = withContext(Dispatchers.IO) {
        runCatching {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
            val fileName = "Medi_Alert_Report_$stamp.pdf"
            val doc = buildDocument(
                context = context,
                patientName = patientName,
                summary = summary,
                medicines = medicines,
                dosesForSelectedDay = dosesForSelectedDay,
                selectedEpochDay = selectedEpochDay,
                language = language
            )
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            "${Environment.DIRECTORY_DOWNLOADS}/Medi_Alert"
                        )
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: error("Unable to create PDF in Download/Medi_Alert")
                    runCatching {
                        resolver.openOutputStream(uri)?.use { out ->
                            doc.writeTo(out)
                        } ?: error("Unable to write PDF output stream")
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                    }.onFailure { err ->
                        runCatching { resolver.delete(uri, null, null) }
                        throw err
                    }
                    SavedPdfReport(
                        uri = uri,
                        fileName = fileName,
                        displayPath = "Download/Medi_Alert/$fileName"
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val publicDir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "Medi_Alert"
                    )
                    val targetDir = runCatching {
                        if (!publicDir.exists()) publicDir.mkdirs()
                        if (publicDir.exists() && publicDir.canWrite()) publicDir else null
                    }.getOrNull() ?: File(
                        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                        "Medi_Alert"
                    ).apply { mkdirs() }

                    val file = File(targetDir, fileName)
                    FileOutputStream(file).use { out ->
                        doc.writeTo(out)
                    }
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    SavedPdfReport(
                        uri = uri,
                        fileName = fileName,
                        displayPath = "Download/Medi_Alert/$fileName"
                    )
                }
            } finally {
                doc.close()
            }
        }
    }

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
                context = context,
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
                context = context,
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
        context: Context,
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
        val headerDividerPaint = Paint().apply {
            color = Color.parseColor("#4DD8F3F5")
            strokeWidth = 1f
        }
        val logoBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#99D8F3F5")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 21f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val sloganPaint = Paint().apply {
            color = Color.parseColor("#C8F6EE")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }
        val reportHeadingPaint = Paint().apply {
            color = Color.WHITE
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#D8F3F5")
            textSize = 10.5f
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

        // Top Banner with App Logo, Medi Alert, Slogan, and Report Details
        val headerHeight = 132f
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), headerHeight, headerBgPaint)

        val logoSize = 50f
        val logoLeft = 32f
        val logoTop = 16f
        val logoRect = RectF(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize)
        val logoBitmap = runCatching { loadRoundedLogoBitmap(context, 128) }.getOrNull()
        if (logoBitmap != null) {
            canvas.drawBitmap(
                logoBitmap,
                null,
                logoRect,
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            )
            logoBitmap.recycle()
        }
        canvas.drawRoundRect(logoRect, 11f, 11f, logoBorderPaint)

        val brandTextX = logoLeft + logoSize + 14f
        canvas.drawText("Medi Alert", brandTextX, 38f, titlePaint)
        canvas.drawText("Never Miss a Dose, Never Miss Good Health.", brandTextX, 57f, sloganPaint)

        canvas.drawLine(32f, 77f, pageWidth - 32f, 77f, headerDividerPaint)

        canvas.drawText("Medication & Adherence Report", 32f, 97f, reportHeadingPaint)
        val rangeText = "${TimeFormatter.formatEpochDay(summary.startEpochDay, AppLanguage.ENGLISH)} - ${
            TimeFormatter.formatEpochDay(summary.endEpochDay, AppLanguage.ENGLISH)
        }"
        canvas.drawText("Patient: $patientName   |   Period: ${summary.period.name} ($rangeText)", 32f, 117f, subtitlePaint)

        var y = 156f

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

    private fun loadRoundedLogoBitmap(context: Context, sizePx: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, R.drawable.ic_medi_alert_fg, opts)
        var sampleSize = 1
        while (opts.outWidth / sampleSize > sizePx * 2 && opts.outHeight / sampleSize > sizePx * 2) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val raw = BitmapFactory.decodeResource(context.resources, R.drawable.ic_medi_alert_fg, decodeOpts)
            ?: return null
        val scaled = if (raw.width != sizePx || raw.height != sizePx) {
            Bitmap.createScaledBitmap(raw, sizePx, sizePx, true).also {
                if (it !== raw) raw.recycle()
            }
        } else {
            raw
        }
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        val path = Path().apply {
            val radius = sizePx * 0.22f
            addRoundRect(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), radius, radius, Path.Direction.CW)
        }
        canvas.clipPath(path)
        canvas.drawBitmap(
            scaled,
            0f,
            0f,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        scaled.recycle()
        return output
    }
}
