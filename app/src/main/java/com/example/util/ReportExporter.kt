package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.FireExtinguisher
import com.example.data.InspectionLog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ReportExporter {
    private const val TAG = "ReportExporter"

    /**
     * Exports fire extinguishers list and recent log records to a beautifully styled PDF document.
     */
    fun exportToPdf(
        context: Context,
        extinguishers: List<FireExtinguisher>,
        logs: List<InspectionLog>
    ): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 page size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paintText = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintBold = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintTitle = Paint().apply {
            color = Color.parseColor("#E21C26") // CPRAM Red
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintSubtitle = Paint().apply {
            color = Color.parseColor("#1B365D") // CPRAM Blue-gray
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintSuccess = Paint().apply {
            color = Color.parseColor("#2E7D32")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintDanger = Paint().apply {
            color = Color.parseColor("#C62828")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.parseColor("#F5F5F5")
            style = Paint.Style.FILL
        }

        var yPos = 40f

        // Title Header
        canvas.drawText("บริษัท ซีพีแรม จำกัด (สุราษฎร์ธานี)", 30f, yPos, paintTitle)
        yPos += 22f
        canvas.drawText("รายงานสรุปตรวจสอบและบำรุงรักษาถังดับเพลิงประจำรอบ", 30f, yPos, paintSubtitle)
        yPos += 15f

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        canvas.drawText("วันที่พิมพ์ระบบ: $dateStr | รวมทั้งหมด: ${extinguishers.size} ถัง", 30f, yPos, paintText)
        yPos += 10f
        canvas.drawText("เขตปฏิบัติการ: ตู้ควบคุมและโรงงานสุราษฎร์ธานี", 30f, yPos, paintText)
        yPos += 20f

        // Table Header
        canvas.drawRect(30f, yPos, 565f, yPos + 22f, tableHeaderPaint)
        canvas.drawLine(30f, yPos, 565f, yPos, linePaint)
        canvas.drawLine(30f, yPos + 22f, 565f, yPos + 22f, linePaint)

        val headerY = yPos + 15f
        canvas.drawText("รหัสถัง", 35f, headerY, paintBold)
        canvas.drawText("ประเภท", 120f, headerY, paintBold)
        canvas.drawText("ตำแหน่งที่ตั้งถังดับเพลิง", 220f, headerY, paintBold)
        canvas.drawText("สถานะล่าสุด", 460f, headerY, paintBold)
        yPos += 22f

        for (ext in extinguishers) {
            if (yPos > 790) { // Limit reach of A4 page to avoid overflow (simple paging fallback)
                break
            }
            val itemY = yPos + 16f
            canvas.drawText(ext.id, 35f, itemY, paintText)
            canvas.drawText(ext.type, 120f, itemY, paintText)

            // Wrap location string if too long for layout density
            val maxLocChar = 32
            val locTrunc = if (ext.location.length > maxLocChar) {
                ext.location.take(maxLocChar) + "..."
            } else {
                ext.location
            }
            canvas.drawText(locTrunc, 220f, itemY, paintText)

            val statusText = when (ext.status) {
                "NORMAL" -> "ปกติ (Checked)"
                "PROBLEM" -> "มีปัญหา (Issue)"
                else -> "ยังไม่ได้ตรวจ"
            }
            val statusPaint = when (ext.status) {
                "NORMAL" -> paintSuccess
                "PROBLEM" -> paintDanger
                else -> paintText
            }
            canvas.drawText(statusText, 460f, itemY, statusPaint)

            canvas.drawLine(30f, yPos + 22f, 565f, yPos + 22f, linePaint)
            yPos += 22f
        }

        yPos += 25f
        canvas.drawText("ประวัติรายงานการแก้ไขและตรวจสอบ 5 รายการล่าสุด:", 30f, yPos, paintSubtitle)
        yPos += 15f

        // Logs Header
        canvas.drawRect(30f, yPos, 565f, yPos + 22f, tableHeaderPaint)
        canvas.drawLine(30f, yPos, 565f, yPos, linePaint)
        canvas.drawLine(30f, yPos + 22f, 565f, yPos + 22f, linePaint)

        val logHeaderY = yPos + 15f
        canvas.drawText("ผู้ตรวจเช็ค", 35f, logHeaderY, paintBold)
        canvas.drawText("เวลาตรวจสอบ", 140f, logHeaderY, paintBold)
        canvas.drawText("รหัสถัง", 280f, logHeaderY, paintBold)
        canvas.drawText("ผลการเช็ค", 360f, logHeaderY, paintBold)
        canvas.drawText("หมายเหตุ", 460f, logHeaderY, paintBold)
        yPos += 22f

        val recentLogs = logs.take(5)
        for (log in recentLogs) {
            if (yPos > 810) break
            val itemY = yPos + 16f
            canvas.drawText(log.inspectorName, 35f, itemY, paintText)

            val logTime = dateFormat.format(Date(log.timestamp))
            canvas.drawText(logTime, 140f, itemY, paintText)
            canvas.drawText(log.extinguisherId, 280f, itemY, paintText)

            val outcome = if (log.overallStatus == "NORMAL") "ปกติ" else "ตรวจพบข้อบกพร่อง"
            val outcomePaint = if (log.overallStatus == "NORMAL") paintSuccess else paintDanger
            canvas.drawText(outcome, 360f, itemY, outcomePaint)

            val remText = log.remarks ?: "-"
            canvas.drawText(if (remText.length > 15) remText.take(12) + "..." else remText, 460f, itemY, paintText)

            canvas.drawLine(30f, yPos + 22f, 565f, yPos + 22f, linePaint)
            yPos += 22f
        }

        yPos += 20f
        canvas.drawText("* การรับรองมาตรฐานระบบรักษาความปลอดภัยจากอัคคีภัย ISO 45001 บริษัท ซีพีแรม จำกัด", 30f, 815f, Paint().apply {
            color = Color.GRAY
            textSize = 8f
            isAntiAlias = true
        })

        pdfDocument.finishPage(page)

        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val file = File(reportsDir, "CPRAM_Fire_Report_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.flush()
            outputStream.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create PDF report: ${e.message}", e)
            pdfDocument.close()
            null
        }
    }

    /**
     * Exports database tables to a unified CSV string, shareable as an Excel compatible sheet.
     */
    fun exportToCsv(context: Context, extinguishers: List<FireExtinguisher>): Uri? {
        val csvHeader = "ID,Location,Type,Size,Installation_Date,Next_Maintenance,Status,Last_Inspected_By,Last_Inspected_At\n"
        val builder = java.lang.StringBuilder()
        builder.append(csvHeader)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        for (ext in extinguishers) {
            val lastTime = ext.lastInspectedAt?.let { dateFormat.format(Date(it)) } ?: "Never"
            val row = "\"${ext.id}\",\"${ext.location}\",\"${ext.type}\",\"${ext.size}\",\"${ext.installationDate}\",\"${ext.nextMaintenanceDate}\",\"${ext.status}\",\"${ext.lastInspectedBy ?: "-"}\",\"$lastTime\"\n"
            builder.append(row)
        }

        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val file = File(reportsDir, "CPRAM_Fire_Extinguishers_${System.currentTimeMillis()}.csv")
            file.writeText(builder.toString(), Charsets.UTF_8)

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create CSV summary: ${e.message}", e)
            null
        }
    }
}
