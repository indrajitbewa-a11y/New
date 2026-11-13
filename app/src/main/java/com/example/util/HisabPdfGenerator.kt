package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.HisabCalculator
import com.example.data.model.LabourTransaction
import com.example.data.model.LabourTxType
import com.example.data.model.WorkerLedgerSummary
import java.io.File
import java.io.FileOutputStream

object HisabPdfGenerator {

    fun generateAndSharePdfSlip(
        context: Context,
        summary: WorkerLedgerSummary,
        attendance: List<AttendanceRecord>,
        transactions: List<LabourTransaction>,
        shopOwnerLabel: String,
    ) {
        val pdfFile = createWorkerSlipPdf(
            context = context,
            summary = summary,
            attendance = attendance,
            transactions = transactions,
            shopOwnerLabel = shopOwnerLabel,
        )
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile,
        )
        val slipText = buildWhatsAppSummaryText(summary, transactions)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Hisab Slip - ${summary.worker.name}")
            putExtra(Intent.EXTRA_TEXT, slipText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, "Share Hisab PDF Slip")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun shareWhatsAppOrTextSlip(
        context: Context,
        summary: WorkerLedgerSummary,
        transactions: List<LabourTransaction>,
    ) {
        val message = buildWhatsAppSummaryText(summary, transactions)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share Hisab on WhatsApp / SMS")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun buildWhatsAppSummaryText(
        summary: WorkerLedgerSummary,
        transactions: List<LabourTransaction>,
    ): String {
        val w = summary.worker
        val balanceLine = when {
            summary.remainingBalance > 0 -> "Baki Hisab (Payable): ${HisabCalculator.formatInr(summary.remainingBalance)}"
            summary.remainingBalance < 0 -> "Extra Advance Taken: ${HisabCalculator.formatInr(-summary.remainingBalance)}"
            else -> "Hisab Status: CLEAR (₹0)"
        }
        val recentLines = transactions.take(5).joinToString("\n") { tx ->
            val typeName = LabourTxType.fromCode(tx.type).label
            "• ${HisabCalculator.formatDisplayDate(tx.date)}: -${HisabCalculator.formatInr(tx.amount)} ($typeName - ${tx.note})"
        }.ifBlank { "• Koi advance/kharcha entry nahi" }

        return buildString {
            appendLine("*LABOUR HISAB SLIP*")
            appendLine("Worker: *${w.name}* (${w.role})")
            appendLine("Daily Rate: ${HisabCalculator.formatInr(w.dailyWage)}/day")
            appendLine("Date: ${HisabCalculator.formatDisplayDate(HisabCalculator.todayIso())}")
            appendLine("------------------------")
            appendLine("Attendance: ${summary.fullDaysCount} Full, ${summary.halfDaysCount} Half, ${summary.overtimeDaysCount} OT, ${summary.absentDaysCount} Absent")
            appendLine("Total Kamai (Earned): ${HisabCalculator.formatInr(summary.totalEarned)}")
            appendLine("Total Paid (Kharcha/Adv): ${HisabCalculator.formatInr(summary.totalPaid)}")
            appendLine("*$balanceLine*")
            appendLine("------------------------")
            appendLine("Recent Payments:")
            append(recentLines)
        }
    }

    private fun createWorkerSlipPdf(
        context: Context,
        summary: WorkerLedgerSummary,
        attendance: List<AttendanceRecord>,
        transactions: List<LabourTransaction>,
        shopOwnerLabel: String,
    ): File {
        val pageWidth = 595
        val pageHeight = 842
        val document = PdfDocument()

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 11f
            isAntiAlias = true
        }
        val headingPaint = Paint().apply {
            color = Color.parseColor("#0F2942")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#1F2937")
            textSize = 11f
            isAntiAlias = true
        }
        val boldBodyPaint = Paint().apply {
            color = Color.parseColor("#111827")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.parseColor("#6B7280")
            textSize = 10f
            isAntiAlias = true
        }
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#0F2942")
        }
        val cardBgPaint = Paint().apply {
            color = Color.parseColor("#FAF7F2")
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 1f
        }

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Top Navy Header
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 96f, headerBgPaint)
        canvas.drawText("SHOP & LABOUR HISAB SLIP", 32f, 40f, titlePaint)
        canvas.drawText(
            "Shop Account: $shopOwnerLabel   |   Date: ${HisabCalculator.formatDisplayDate(HisabCalculator.todayIso())}",
            32f,
            66f,
            subtitlePaint,
        )

        var y = 124f
        val w = summary.worker
        canvas.drawText("Worker: ${w.name} (${w.role})", 32f, y, headingPaint)
        y += 18f
        canvas.drawText(
            "Phone: ${w.phone.ifBlank { "N/A" }}   |   Daily Wage: ${HisabCalculator.formatInr(w.dailyWage)}/day   |   Joined: ${HisabCalculator.formatDisplayDate(w.joiningDate)}",
            32f,
            y,
            mutedPaint,
        )

        y += 20f
        canvas.drawRect(32f, y, pageWidth - 32f, y + 76f, cardBgPaint)
        val balPaint = Paint(boldBodyPaint).apply {
            textSize = 14f
            color = when {
                summary.remainingBalance > 0 -> Color.parseColor("#D97706")
                summary.remainingBalance < 0 -> Color.parseColor("#DC2626")
                else -> Color.parseColor("#059669")
            }
        }
        canvas.drawText("Total Earned (Kamai): ${HisabCalculator.formatInr(summary.totalEarned)}", 48f, y + 24f, boldBodyPaint)
        canvas.drawText("Total Paid (Kharcha/Adv): ${HisabCalculator.formatInr(summary.totalPaid)}", 310f, y + 24f, boldBodyPaint)
        val balText = when {
            summary.remainingBalance > 0 -> "Remaining Balance (Baki): ${HisabCalculator.formatInr(summary.remainingBalance)}"
            summary.remainingBalance < 0 -> "Extra Advance Taken: ${HisabCalculator.formatInr(-summary.remainingBalance)}"
            else -> "Remaining Balance: ₹0 (Settled / Clear)"
        }
        canvas.drawText(balText, 48f, y + 54f, balPaint)

        y += 100f
        canvas.drawText(
            "Attendance Summary (${summary.fullDaysCount} Full, ${summary.halfDaysCount} Half, ${summary.overtimeDaysCount} OT, ${summary.absentDaysCount} Absent)",
            32f,
            y,
            headingPaint,
        )
        y += 14f
        canvas.drawLine(32f, y, pageWidth - 32f, y, dividerPaint)
        y += 18f

        val attSlice = attendance.take(12)
        if (attSlice.isEmpty()) {
            canvas.drawText("No attendance records yet.", 32f, y, mutedPaint)
            y += 18f
        } else {
            for (rec in attSlice) {
                val st = AttendanceStatus.fromCode(rec.status).label
                canvas.drawText(HisabCalculator.formatDisplayDate(rec.date), 32f, y, bodyPaint)
                canvas.drawText(st, 170f, y, bodyPaint)
                canvas.drawText(HisabCalculator.formatInr(rec.earnedAmount), 340f, y, boldBodyPaint)
                if (rec.note.isNotBlank()) {
                    canvas.drawText(rec.note.take(26), 415f, y, mutedPaint)
                }
                y += 17f
            }
        }

        y += 14f
        canvas.drawText("Kharcha / Advance & Payout Ledger", 32f, y, headingPaint)
        y += 14f
        canvas.drawLine(32f, y, pageWidth - 32f, y, dividerPaint)
        y += 18f

        val txSlice = transactions.take(14)
        if (txSlice.isEmpty()) {
            canvas.drawText("No kharcha or advance entries recorded.", 32f, y, mutedPaint)
        } else {
            for (tx in txSlice) {
                if (y > pageHeight - 40f) break
                val typeLbl = LabourTxType.fromCode(tx.type).label
                canvas.drawText(HisabCalculator.formatDisplayDate(tx.date), 32f, y, bodyPaint)
                canvas.drawText("$typeLbl (${tx.paymentMode})", 140f, y, bodyPaint)
                canvas.drawText("-${HisabCalculator.formatInr(tx.amount)}", 300f, y, boldBodyPaint)
                canvas.drawText(tx.note.take(30), 390f, y, mutedPaint)
                y += 17f
            }
        }

        document.finishPage(page)

        val dir = File(context.cacheDir, "hisab_slips").apply { mkdirs() }
        val safeName = w.name.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase().ifBlank { "worker" }
        val outFile = File(dir, "${safeName}_hisab_slip.pdf")
        FileOutputStream(outFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()
        return outFile
    }
}
