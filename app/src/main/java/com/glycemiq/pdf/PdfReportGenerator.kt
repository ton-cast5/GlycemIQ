package com.glycemiq.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.glycemiq.domain.model.GlucoseRecordUi
import com.glycemiq.util.DateTimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfReportGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 40f

    fun generateReport(records: List<GlucoseRecordUi>): File {
        val document = PdfDocument()
        val sortedRecords = records.sortedByDescending { it.timestamp }
        val rowsPerPage = 25
        val totalPages = maxOf(1, (sortedRecords.size + rowsPerPage - 1) / rowsPerPage)

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawPage(
                canvas = canvas,
                records = sortedRecords,
                pageIndex = pageIndex,
                rowsPerPage = rowsPerPage,
                isFirstPage = pageIndex == 0,
                isLastPage = pageIndex == totalPages - 1
            )

            document.finishPage(page)
        }

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val fileName = "GlycemIQ_Reporte_${System.currentTimeMillis()}.pdf"
        val file = File(reportsDir, fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun drawPage(
        canvas: Canvas,
        records: List<GlucoseRecordUi>,
        pageIndex: Int,
        rowsPerPage: Int,
        isFirstPage: Boolean,
        isLastPage: Boolean
    ) {
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1565C0")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#424242")
            textSize = 12f
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val cellPaint = Paint().apply {
            color = Color.parseColor("#212121")
            textSize = 10f
            isAntiAlias = true
        }
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#1565C0")
        }
        val rowAltPaint = Paint().apply {
            color = Color.parseColor("#F5F5F5")
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
        }

        var y = margin

        if (isFirstPage) {
            canvas.drawText("GlycemIQ", margin, y + 20f, titlePaint)
            y += 35f

            val generationDate = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale("es", "MX"))
                .format(Instant.now().atZone(DateTimeUtils.MEXICO_ZONE))
            canvas.drawText("Fecha de generación: $generationDate", margin, y + 15f, subtitlePaint)
            y += 25f

            val intro = "Reporte de monitoreo glucémico del paciente, generado automáticamente " +
                "con base en los registros almacenados en la aplicación."
            drawWrappedText(canvas, intro, margin, y, pageWidth - margin * 2, subtitlePaint)
            y += 50f
        }

        val colWidths = floatArrayOf(100f, 70f, 90f, 200f)
        val headers = arrayOf("Fecha", "Hora", "Glucosa", "Contexto")
        val tableWidth = colWidths.sum()
        val rowHeight = 28f

        canvas.drawRect(margin, y, margin + tableWidth, y + rowHeight, headerBgPaint)
        var x = margin + 8f
        headers.forEachIndexed { index, header ->
            canvas.drawText(header, x, y + 18f, headerPaint)
            x += colWidths[index]
        }
        y += rowHeight

        val startIndex = pageIndex * rowsPerPage
        val endIndex = minOf(startIndex + rowsPerPage, records.size)
        val pageRecords = if (records.isEmpty()) emptyList() else records.subList(startIndex, endIndex)

        if (pageRecords.isEmpty() && isFirstPage) {
            canvas.drawText("No hay registros disponibles.", margin + 8f, y + 18f, cellPaint)
        }

        pageRecords.forEachIndexed { index, record ->
            if (index % 2 == 1) {
                canvas.drawRect(margin, y, margin + tableWidth, y + rowHeight, rowAltPaint)
            }
            x = margin + 8f
            val values = arrayOf(
                DateTimeUtils.formatDate(record.timestamp),
                DateTimeUtils.formatTime(record.timestamp),
                "${record.value} mg/dL",
                record.context.label
            )
            values.forEachIndexed { colIndex, value ->
                canvas.drawText(value, x, y + 18f, cellPaint)
                x += colWidths[colIndex]
            }
            canvas.drawLine(margin, y + rowHeight, margin + tableWidth, y + rowHeight, linePaint)
            y += rowHeight
        }

        if (isLastPage) {
            val footer = "Documento generado por GlycemIQ — Monitoreo inteligente de glucosa"
            canvas.drawText(footer, margin, pageHeight - margin, subtitlePaint)
        }
    }

    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: Paint) {
        val words = text.split(" ")
        var line = ""
        var currentY = y
        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(testLine) > maxWidth) {
                canvas.drawText(line, x, currentY, paint)
                line = word
                currentY += 16f
            } else {
                line = testLine
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, x, currentY, paint)
        }
    }
}
