package com.example.data.model

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

object HisabCalculator {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val inrFormatter = NumberFormat.getIntegerInstance(
        Locale.Builder().setLanguage("en").setRegion("IN").build()
    )

    fun todayIso(): String = dateFormatter.format(Date())

    fun formatDisplayDate(isoDate: String): String {
        return try {
            val parsed = dateFormatter.parse(isoDate)
            if (parsed != null) displayDateFormatter.format(parsed) else isoDate
        } catch (_: Exception) {
            isoDate
        }
    }

    fun formatInr(amount: Long): String {
        val absFormatted = inrFormatter.format(kotlin.math.abs(amount))
        return if (amount < 0) "-₹$absFormatted" else "₹$absFormatted"
    }

    fun calculateEarnedForAttendance(
        dailyWage: Long,
        status: AttendanceStatus,
        overtimeAmount: Long = 0L,
    ): Long {
        val base = when (status) {
            AttendanceStatus.PRESENT -> dailyWage
            AttendanceStatus.HALF_DAY -> (dailyWage * 0.5).roundToLong()
            AttendanceStatus.ABSENT -> 0L
            AttendanceStatus.OVERTIME -> dailyWage + overtimeAmount.coerceAtLeast(0L)
        }
        return base.coerceAtLeast(0L)
    }

    fun computeWorkerSummary(
        worker: Worker,
        attendanceRecords: List<AttendanceRecord>,
        transactions: List<LabourTransaction>,
    ): WorkerLedgerSummary {
        val workerAttendance = attendanceRecords.filter { it.workerId == worker.id }
        val workerTx = transactions.filter { it.workerId == worker.id }

        val totalEarned = workerAttendance.sumOf { it.earnedAmount }
        val totalPaid = workerTx.sumOf { it.amount }
        val remainingBalance = totalEarned - totalPaid

        var fullDays = 0
        var halfDays = 0
        var absentDays = 0
        var overtimeDays = 0
        var overtimeTotal = 0L

        for (record in workerAttendance) {
            when (AttendanceStatus.fromCode(record.status)) {
                AttendanceStatus.PRESENT -> fullDays++
                AttendanceStatus.HALF_DAY -> halfDays++
                AttendanceStatus.ABSENT -> absentDays++
                AttendanceStatus.OVERTIME -> {
                    overtimeDays++
                    overtimeTotal += record.overtimeAmount
                }
            }
        }

        return WorkerLedgerSummary(
            worker = worker,
            totalEarned = totalEarned,
            totalPaid = totalPaid,
            remainingBalance = remainingBalance,
            fullDaysCount = fullDays,
            halfDaysCount = halfDays,
            absentDaysCount = absentDays,
            overtimeDaysCount = overtimeDays,
            totalOvertimeEarned = overtimeTotal,
        )
    }

    fun filterCashbookByPeriod(
        entries: List<CashbookEntry>,
        period: CashbookPeriod,
        referenceDateIso: String = todayIso(),
    ): List<CashbookEntry> {
        val refDate = try {
            dateFormatter.parse(referenceDateIso) ?: Date()
        } catch (_: Exception) {
            Date()
        }
        val cal = Calendar.getInstance()
        cal.time = refDate

        val startIso = when (period) {
            CashbookPeriod.DAILY -> referenceDateIso
            CashbookPeriod.WEEKLY -> {
                cal.add(Calendar.DAY_OF_YEAR, -6)
                dateFormatter.format(cal.time)
            }
            CashbookPeriod.MONTHLY -> {
                cal.add(Calendar.DAY_OF_YEAR, -29)
                dateFormatter.format(cal.time)
            }
        }
        return entries.filter { it.date in startIso..referenceDateIso }
    }

    fun computeCashbookSummary(entries: List<CashbookEntry>): CashbookSummary {
        var totalIncome = 0L
        var totalExpense = 0L
        var cashIncome = 0L
        var cashExpense = 0L
        var upiIncome = 0L
        var upiExpense = 0L

        for (entry in entries) {
            val isIncome = entry.type == CashbookType.INCOME.code
            val isUpi = entry.paymentMode == PaymentMode.UPI.code
            if (isIncome) {
                totalIncome += entry.amount
                if (isUpi) upiIncome += entry.amount else cashIncome += entry.amount
            } else {
                totalExpense += entry.amount
                if (isUpi) upiExpense += entry.amount else cashExpense += entry.amount
            }
        }

        return CashbookSummary(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = totalIncome - totalExpense,
            cashIncome = cashIncome,
            cashExpense = cashExpense,
            upiIncome = upiIncome,
            upiExpense = upiExpense,
        )
    }

    fun shiftIsoDate(isoDate: String, deltaDays: Int): String {
        return try {
            val parsed = dateFormatter.parse(isoDate) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = parsed
            cal.add(Calendar.DAY_OF_YEAR, deltaDays)
            dateFormatter.format(cal.time)
        } catch (_: Exception) {
            isoDate
        }
    }

    fun buildMonthDays(yearMonthPrefix: String): List<String> {
        // yearMonthPrefix e.g. "2026-11"
        return try {
            val firstDay = dateFormatter.parse("$yearMonthPrefix-01") ?: return emptyList()
            val cal = Calendar.getInstance()
            cal.time = firstDay
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            (1..maxDays).map { day ->
                "$yearMonthPrefix-${day.toString().padStart(2, '0')}"
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun firstDayWeekdayOffset(yearMonthPrefix: String): Int {
        // Returns 0 for Sunday, 1 for Monday ... 6 for Saturday
        return try {
            val firstDay = dateFormatter.parse("$yearMonthPrefix-01") ?: return 0
            val cal = Calendar.getInstance()
            cal.time = firstDay
            cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        } catch (_: Exception) {
            0
        }
    }

    fun shiftYearMonth(yearMonthPrefix: String, deltaMonths: Int): String {
        return try {
            val firstDay = dateFormatter.parse("$yearMonthPrefix-01") ?: Date()
            val cal = Calendar.getInstance()
            cal.time = firstDay
            cal.add(Calendar.MONTH, deltaMonths)
            dateFormatter.format(cal.time).substring(0, 7)
        } catch (_: Exception) {
            yearMonthPrefix
        }
    }

    fun formatMonthTitle(yearMonthPrefix: String): String {
        return try {
            val firstDay = dateFormatter.parse("$yearMonthPrefix-01") ?: return yearMonthPrefix
            SimpleDateFormat("MMMM yyyy", Locale.US).format(firstDay)
        } catch (_: Exception) {
            yearMonthPrefix
        }
    }
}
