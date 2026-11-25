package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object PrescriptionFileHelper {

    data class CopiedPrescription(
        val internalPath: String,
        val mimeType: String
    )

    suspend fun copyToInternalStorage(
        context: Context,
        sourceUri: Uri
    ): Result<CopiedPrescription> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(sourceUri) ?: "application/octet-stream"
            val ext = when {
                mimeType.contains("pdf", ignoreCase = true) -> "pdf"
                mimeType.contains("png", ignoreCase = true) -> "png"
                mimeType.contains("webp", ignoreCase = true) -> "webp"
                else -> "jpg"
            }
            val dir = File(context.filesDir, "prescriptions").apply { mkdirs() }
            val destFile = File(dir, "rx_${UUID.randomUUID()}.$ext")
            resolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: error("Unable to read selected file")
            CopiedPrescription(internalPath = destFile.absolutePath, mimeType = mimeType)
        }
    }

    fun deleteIfInternal(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val file = File(path)
            if (file.exists() && file.absolutePath.contains("prescriptions")) {
                file.delete()
            }
        }
    }

    suspend fun renderPdfPages(filePath: String, maxPages: Int = 6): List<Bitmap> =
        withContext(Dispatchers.IO) {
            val file = File(filePath)
            if (!file.exists()) return@withContext emptyList()
            val bitmaps = mutableListOf<Bitmap>()
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            try {
                pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)
                val count = renderer.pageCount.coerceAtMost(maxPages)
                for (i in 0 until count) {
                    renderer.openPage(i).use { page ->
                        val width = (page.width * 2).coerceIn(600, 1600)
                        val height = ((width.toFloat() / page.width) * page.height).toInt().coerceIn(400, 2200)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmaps.add(bitmap)
                    }
                }
            } catch (_: Throwable) {
                // Return already rendered pages or empty list
            } finally {
                runCatching { renderer?.close() }
                runCatching { pfd?.close() }
            }
            bitmaps
        }
}
