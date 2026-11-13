package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HisabCalculatorTest {

    @Test
    fun calculateEarnedForAttendance_handlesAllStatuses() {
        assertEquals(600L, HisabCalculator.calculateEarnedForAttendance(600L, AttendanceStatus.PRESENT))
        assertEquals(300L, HisabCalculator.calculateEarnedForAttendance(600L, AttendanceStatus.HALF_DAY))
        assertEquals(0L, HisabCalculator.calculateEarnedForAttendance(600L, AttendanceStatus.ABSENT))
        assertEquals(850L, HisabCalculator.calculateEarnedForAttendance(600L, AttendanceStatus.OVERTIME, 250L))
    }

    @Test
    fun computeWorkerSummary_calculatesRemainingBalanceAccurately() {
        val worker = Worker(id = "w1", name = "Suresh", dailyWage = 500L)
        val attendance = listOf(
            AttendanceRecord(id = "w1_2026-11-10", workerId = "w1", date = "2026-11-10", status = "PRESENT", earnedAmount = 500L),
            AttendanceRecord(id = "w1_2026-11-11", workerId = "w1", date = "2026-11-11", status = "HALF_DAY", earnedAmount = 250L),
            AttendanceRecord(id = "w1_2026-11-12", workerId = "w1", date = "2026-11-12", status = "OVERTIME", overtimeAmount = 150L, earnedAmount = 650L),
        )
        val transactions = listOf(
            LabourTransaction(id = "t1", workerId = "w1", type = "KHARCHA", amount = 100L),
            LabourTransaction(id = "t2", workerId = "w1", type = "ADVANCE", amount = 400L),
        )

        val summary = HisabCalculator.computeWorkerSummary(worker, attendance, transactions)
        assertEquals(1400L, summary.totalEarned)
        assertEquals(500L, summary.totalPaid)
        assertEquals(900L, summary.remainingBalance)
        assertEquals(1, summary.fullDaysCount)
        assertEquals(1, summary.halfDaysCount)
        assertEquals(1, summary.overtimeDaysCount)
        assertEquals(150L, summary.totalOvertimeEarned)
    }

    @Test
    fun filterAndSummarizeCashbook_handlesDailyWeeklyMonthlyAndModes() {
        val entries = listOf(
            CashbookEntry(id = "1", type = "INCOME", amount = 5000L, paymentMode = "CASH", date = "2026-11-12"),
            CashbookEntry(id = "2", type = "INCOME", amount = 2000L, paymentMode = "UPI", date = "2026-11-12"),
            CashbookEntry(id = "3", type = "EXPENSE", amount = 800L, paymentMode = "CASH", date = "2026-11-12"),
            CashbookEntry(id = "4", type = "EXPENSE", amount = 1200L, paymentMode = "UPI", date = "2026-11-08"),
            CashbookEntry(id = "5", type = "INCOME", amount = 3000L, paymentMode = "CASH", date = "2026-10-25"),
        )

        val daily = HisabCalculator.filterCashbookByPeriod(entries, CashbookPeriod.DAILY, "2026-11-12")
        val dailySummary = HisabCalculator.computeCashbookSummary(daily)
        assertEquals(7000L, dailySummary.totalIncome)
        assertEquals(800L, dailySummary.totalExpense)
        assertEquals(6200L, dailySummary.netBalance)
        assertEquals(4200L, dailySummary.cashNet)
        assertEquals(2000L, dailySummary.upiNet)

        val weekly = HisabCalculator.filterCashbookByPeriod(entries, CashbookPeriod.WEEKLY, "2026-11-12")
        assertEquals(4, weekly.size)

        val monthly = HisabCalculator.filterCashbookByPeriod(entries, CashbookPeriod.MONTHLY, "2026-11-12")
        assertEquals(5, monthly.size)
    }
}
