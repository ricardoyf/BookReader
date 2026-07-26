package com.ricardo.bookreader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.ricardo.bookreader.model.BookDocument
import com.ricardo.bookreader.model.PageMark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

object MarkedPagesPdfExporter {
    suspend fun export(
        context: Context,
        book: BookDocument,
        marks: List<PageMark>
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(marks.isNotEmpty()) { "No hay páginas marcadas" }
            val ordered = marks.sortedBy { it.pageNumber }
            val document = PdfDocument()
            val pdfHandle = if (book.isPdf) openSourcePdf(context, book.uri) else null

            try {
                ordered.forEachIndexed { index, mark ->
                    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
                    val outputPage = document.startPage(pageInfo)
                    drawHeader(outputPage.canvas, book.name, mark.pageNumber, index + 1, ordered.size)
                    if (mark.isPdf && pdfHandle != null) {
                        drawPdfPage(outputPage.canvas, pdfHandle.renderer, mark.pageNumber - 1)
                    } else {
                        drawTextPage(outputPage.canvas, mark.pageText)
                    }
                    document.finishPage(outputPage)
                }

                val output = File(context.cacheDir, "BookReader_paginas_marcadas.pdf")
                output.outputStream().buffered().use(document::writeTo)
                output
            } finally {
                document.close()
                pdfHandle?.close()
            }
        }
    }

    private fun drawHeader(
        canvas: Canvas,
        bookName: String,
        originalPage: Int,
        exportPage: Int,
        total: Int
    ) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 35, 35)
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metadataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(90, 90, 90)
            textSize = 25f
        }
        canvas.drawColor(Color.WHITE)
        canvas.drawText(bookName.take(64), MARGIN.toFloat(), 58f, titlePaint)
        canvas.drawText(
            "Página original $originalPage  ·  Post-it $exportPage de $total",
            MARGIN.toFloat(),
            98f,
            metadataPaint
        )
        canvas.drawLine(
            MARGIN.toFloat(),
            HEADER_HEIGHT - 15f,
            PAGE_WIDTH - MARGIN.toFloat(),
            HEADER_HEIGHT - 15f,
            metadataPaint
        )
    }

    private fun drawPdfPage(canvas: Canvas, renderer: PdfRenderer, index: Int) {
        if (index !in 0 until renderer.pageCount) return
        synchronized(renderer) {
            renderer.openPage(index).use { source ->
                val availableWidth = PAGE_WIDTH - MARGIN * 2
                val availableHeight = PAGE_HEIGHT - HEADER_HEIGHT - MARGIN
                val scale = minOf(
                    availableWidth.toFloat() / source.width,
                    availableHeight.toFloat() / source.height
                )
                val width = (source.width * scale).roundToInt().coerceAtLeast(1)
                val height = (source.height * scale).roundToInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                Canvas(bitmap).drawColor(Color.WHITE)
                source.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val left = (PAGE_WIDTH - width) / 2
                val top = HEADER_HEIGHT + (availableHeight - height) / 2
                canvas.drawBitmap(bitmap, null, Rect(left, top, left + width, top + height), null)
                bitmap.recycle()
            }
        }
    }

    private fun drawTextPage(canvas: Canvas, text: String) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(25, 25, 25)
            textSize = 30f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        val width = PAGE_WIDTH - MARGIN * 2
        val content = text.ifBlank { "(Página sin texto)" }
        val layout = StaticLayout.Builder
            .obtain(content, 0, content.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(true)
            .setLineSpacing(3f, 1.2f)
            .setMaxLines(46)
            .build()
        canvas.save()
        canvas.translate(MARGIN.toFloat(), HEADER_HEIGHT.toFloat())
        layout.draw(canvas)
        canvas.restore()
    }

    private fun openSourcePdf(context: Context, uri: Uri): SourcePdf {
        val resolver = context.contentResolver
        val direct = resolver.openFileDescriptor(uri, "r") ?: error("No se pudo abrir el PDF")
        try {
            return SourcePdf(direct, PdfRenderer(direct))
        } catch (_: Exception) {
            direct.close()
        }

        val cache = File(
            context.cacheDir,
            "bookreader_export_${uri.toString().hashCode().toUInt().toString(16)}.pdf"
        )
        resolver.openInputStream(uri)?.use { input ->
            cache.outputStream().buffered().use { output -> input.copyTo(output) }
        } ?: error("No se pudo copiar el PDF")
        val descriptor = ParcelFileDescriptor.open(cache, ParcelFileDescriptor.MODE_READ_ONLY)
        return SourcePdf(descriptor, PdfRenderer(descriptor))
    }

    private class SourcePdf(
        private val descriptor: ParcelFileDescriptor,
        val renderer: PdfRenderer
    ) {
        fun close() {
            runCatching { renderer.close() }
            runCatching { descriptor.close() }
        }
    }

    private const val PAGE_WIDTH = 1240
    private const val PAGE_HEIGHT = 1754
    private const val HEADER_HEIGHT = 130
    private const val MARGIN = 56
}
