package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AttendanceStatus(
    val code: String,
    val label: String,
    val hinglishLabel: String,
    val multiplier: Double,
) {
    PRESENT("PRESENT", "Present (1x)", "Full Day • Hazir", 1.0),
    HALF_DAY("HALF_DAY", "Half Day (0.5x)", "Half Day • Aadha", 0.5),
    ABSENT("ABSENT", "Absent (0x)", "Absent • Chhutti", 0.0),
    OVERTIME("OVERTIME", "Overtime (+₹)", "Overtime • Extra", 1.0);

    companion object {
        fun fromCode(code: String): AttendanceStatus =
            entries.firstOrNull { it.code == code } ?: ABSENT
    }
}

enum class LabourTxType(
    val code: String,
    val label: String,
    val hinglishLabel: String,
) {
    KHARCHA("KHARCHA", "Kharcha", "Chai / Nashta Kharcha"),
    ADVANCE("ADVANCE", "Advance", "Peshgi / Advance"),
    SETTLEMENT("SETTLEMENT", "Settlement", "Hisab Clear / Payout");

    companion object {
        fun fromCode(code: String): LabourTxType =
            entries.firstOrNull { it.code == code } ?: ADVANCE
    }
}

enum class PaymentMode(val code: String, val label: String) {
    CASH("CASH", "Cash / Nagad"),
    UPI("UPI", "UPI / Online");

    companion object {
        fun fromCode(code: String): PaymentMode =
            entries.firstOrNull { it.code == code } ?: CASH
    }
}

enum class CashbookType(val code: String, val label: String) {
    INCOME("INCOME", "Income (+) • Aamdani"),
    EXPENSE("EXPENSE", "Expense (-) • Kharch");

    companion object {
        fun fromCode(code: String): CashbookType =
            entries.firstOrNull { it.code == code } ?: EXPENSE
    }
}

enum class CashbookPeriod(val label: String, val hinglishLabel: String) {
    DAILY("Today", "Aaj (Daily)"),
    WEEKLY("7 Days", "Hafta (Weekly)"),
    MONTHLY("30 Days", "Mahina (Monthly)"),
}

@IgnoreExtraProperties
data class Worker(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val role: String = "",
    val dailyWage: Long = 0L,
    val joiningDate: String = "",
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val workerId: String = "",
    val date: String = "",
    val status: String = AttendanceStatus.ABSENT.code,
    val dailyWageSnapshot: Long = 0L,
    val overtimeAmount: Long = 0L,
    val earnedAmount: Long = 0L,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class LabourTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val workerId: String = "",
    val workerName: String = "",
    val type: String = LabourTxType.ADVANCE.code,
    val amount: Long = 0L,
    val date: String = "",
    val paymentMode: String = PaymentMode.CASH.code,
    val note: String = "",
    val linkedCashbookId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CashbookEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = CashbookType.INCOME.code,
    val category: String = "",
    val amount: Long = 0L,
    val paymentMode: String = PaymentMode.CASH.code,
    val date: String = "",
    val note: String = "",
    val linkedWorkerId: String = "",
    val linkedLabourTxId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class WorkerLedgerSummary(
    val worker: Worker,
    val totalEarned: Long,
    val totalPaid: Long,
    val remainingBalance: Long,
    val fullDaysCount: Int,
    val halfDaysCount: Int,
    val absentDaysCount: Int,
    val overtimeDaysCount: Int,
    val totalOvertimeEarned: Long,
)

data class CashbookSummary(
    val totalIncome: Long,
    val totalExpense: Long,
    val netBalance: Long,
    val cashIncome: Long,
    val cashExpense: Long,
    val upiIncome: Long,
    val upiExpense: Long,
) {
    val cashNet: Long get() = cashIncome - cashExpense
    val upiNet: Long get() = upiIncome - upiExpense
}
