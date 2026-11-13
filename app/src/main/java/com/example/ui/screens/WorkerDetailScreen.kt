package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.HisabCalculator
import com.example.data.model.LabourTransaction
import com.example.data.model.LabourTxType
import com.example.data.model.PaymentMode
import com.example.data.model.Worker
import com.example.ui.theme.LocalHisabColors
import com.example.ui.viewmodel.HisabUiState
import com.example.util.HisabPdfGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDetailScreen(
    workerId: String,
    state: HisabUiState.Ready,
    shopOwnerLabel: String,
    onBack: () -> Unit,
    onUpdateWorker: (Worker, String, String, String, Long, String, Boolean) -> Unit,
    onDeleteWorker: (Worker) -> Unit,
    onMarkAttendance: (Worker, String, AttendanceStatus, Long, String) -> Unit,
    onAddTransaction: (Worker, LabourTxType, Long, String, PaymentMode, String) -> Unit,
    onDeleteTransaction: (LabourTransaction) -> Unit,
) {
    val context = LocalContext.current
    val hisabColors = LocalHisabColors.current
    val summary = state.workerSummaries[workerId]

    if (summary == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Worker Not Found") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Yeh staff member delete ho chuka hai.")
            }
        }
        return
    }

    val worker = summary.worker
    val workerAttendance = remember(state.attendanceRecords, workerId) {
        state.attendanceRecords.filter { it.workerId == workerId }.sortedByDescending { it.date }
    }
    val attendanceByDate = remember(workerAttendance) {
        workerAttendance.associateBy { it.date }
    }
    val workerTransactions = remember(state.labourTransactions, workerId) {
        state.labourTransactions.filter { it.workerId == workerId }
    }

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Ledger, 1 = Attendance Calendar
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var txDialogMode by remember { mutableStateOf<LabourTxType?>(null) }
    var calendarMonth by remember { mutableStateOf(state.todayDate.take(7)) }
    var selectedCalDate by remember { mutableStateOf(state.todayDate) }
    var showOvertimeDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        AddEditWorkerDialog(
            initialWorker = worker,
            onDismiss = { showEditDialog = false },
            onSave = { name, phone, role, wage, joiningDate, isActive ->
                onUpdateWorker(worker, name, phone, role, wage, joiningDate, isActive)
                showEditDialog = false
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove ${worker.name}?") },
            text = { Text("Staff profile list se hat jayega. Existing Galla entries safe rahengi.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteWorker(worker)
                        onBack()
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    txDialogMode?.let { mode ->
        AddLabourTransactionDialog(
            worker = worker,
            initialType = mode,
            initialAmount = if (mode == LabourTxType.SETTLEMENT && summary.remainingBalance > 0L) {
                summary.remainingBalance
            } else {
                null
            },
            initialNote = if (mode == LabourTxType.SETTLEMENT) "Full Hisab Clear" else "",
            onDismiss = { txDialogMode = null },
            onConfirm = { type, amount, date, paymentMode, note ->
                onAddTransaction(worker, type, amount, date, paymentMode, note)
                txDialogMode = null
            },
        )
    }

    if (showOvertimeDialog) {
        val existing = attendanceByDate[selectedCalDate]
        OvertimeAmountDialog(
            worker = worker,
            date = selectedCalDate,
            initialOvertime = existing?.overtimeAmount ?: 200L,
            initialNote = existing?.note ?: "",
            onDismiss = { showOvertimeDialog = false },
            onConfirm = { extra, note ->
                onMarkAttendance(worker, selectedCalDate, AttendanceStatus.OVERTIME, extra, note)
                showOvertimeDialog = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = worker.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${worker.role} • ${HisabCalculator.formatInr(worker.dailyWage)}/day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("worker_detail_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (worker.phone.isNotBlank()) {
                        IconButton(onClick = {
                            val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${worker.phone}"))
                            context.startActivity(dial)
                        }) {
                            Icon(Icons.Filled.Call, contentDescription = "Call worker")
                        }
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit worker")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete worker")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Real-Time Remaining Balance Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(hisabColors.heroStart, hisabColors.heroEnd)
                                )
                            )
                            .padding(18.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = "BAKI HISAB • REMAINING BALANCE",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.78f),
                                    )
                                    val bal = summary.remainingBalance
                                    Text(
                                        text = HisabCalculator.formatInr(bal),
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when {
                                            bal > 0L -> Color(0xFFFCD34D)
                                            bal < 0L -> Color(0xFFFCA5A5)
                                            else -> Color(0xFF6EE7B7)
                                        },
                                        modifier = Modifier.testTag("worker_remaining_balance"),
                                    )
                                    Text(
                                        text = when {
                                            bal > 0L -> "Worker ko dena baki hai"
                                            bal < 0L -> "Extra advance liya hua hai"
                                            else -> "Hisab Clear hai ✓"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Total Kamai (Earned)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.75f),
                                        )
                                        Text(
                                            text = HisabCalculator.formatInr(summary.totalEarned),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF6EE7B7),
                                        )
                                    }
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Kharcha / Advance (-)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.75f),
                                        )
                                        Text(
                                            text = HisabCalculator.formatInr(summary.totalPaid),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFCA5A5),
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "${summary.fullDaysCount} Full  •  ${summary.halfDaysCount} Half  •  ${summary.overtimeDaysCount} OT  •  ${summary.absentDaysCount} Absent",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.82f),
                            )
                        }
                    }
                }
            }

            // 2. Action Buttons: Add Kharcha/Advance & Settle / Clear Balance
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { txDialogMode = LabourTxType.KHARCHA },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("add_kharcha_button"),
                    ) {
                        Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Kharcha / Adv", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Button(
                        onClick = { txDialogMode = LabourTxType.SETTLEMENT },
                        enabled = summary.remainingBalance > 0L,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = hisabColors.income,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("settle_balance_button"),
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Settle / Clear", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // 3. Share PDF / WhatsApp Slip Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            HisabPdfGenerator.generateAndSharePdfSlip(
                                context = context,
                                summary = summary,
                                attendance = workerAttendance,
                                transactions = workerTransactions,
                                shopOwnerLabel = shopOwnerLabel,
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_pdf_slip_button"),
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PDF Hisab Slip")
                    }
                    FilledTonalButton(
                        onClick = {
                            HisabPdfGenerator.shareWhatsAppOrTextSlip(
                                context = context,
                                summary = summary,
                                transactions = workerTransactions,
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_whatsapp_slip_button"),
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("WhatsApp Slip")
                    }
                }
            }

            // 4. Ledger vs Calendar Segmented Switch
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("Kharcha / Payouts (${workerTransactions.size})", "Haziri Calendar (${workerAttendance.size})").forEachIndexed { idx, title ->
                        SegmentedButton(
                            selected = activeTab == idx,
                            onClick = { activeTab = idx },
                            shape = SegmentedButtonDefaults.itemShape(idx, 2),
                        ) {
                            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            if (activeTab == 0) {
                if (workerTransactions.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Abhi tak koi kharcha ya advance nahi diya gaya hai.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                } else {
                    items(workerTransactions, key = { it.id }) { tx ->
                        LabourTransactionRow(
                            tx = tx,
                            onDelete = { onDeleteTransaction(tx) },
                        )
                    }
                }
            } else {
                // Individual Worker Attendance Calendar + Quick Mark for Selected Day
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            MonthAttendanceCalendar(
                                yearMonth = calendarMonth,
                                selectedDate = selectedCalDate,
                                markedDates = attendanceByDate.keys,
                                dayStatusColorProvider = { dateIso ->
                                    when (attendanceByDate[dateIso]?.let { AttendanceStatus.fromCode(it.status) }) {
                                        AttendanceStatus.PRESENT -> hisabColors.income
                                        AttendanceStatus.HALF_DAY -> hisabColors.halfDay
                                        AttendanceStatus.ABSENT -> hisabColors.expense
                                        AttendanceStatus.OVERTIME -> hisabColors.warning
                                        null -> null
                                    }
                                },
                                onPrevMonth = { calendarMonth = HisabCalculator.shiftYearMonth(calendarMonth, -1) },
                                onNextMonth = { calendarMonth = HisabCalculator.shiftYearMonth(calendarMonth, 1) },
                                onDateSelected = { selectedCalDate = it },
                            )

                            val rec = attendanceByDate[selectedCalDate]
                            Text(
                                text = "Selected: ${HisabCalculator.formatDisplayDate(selectedCalDate)} — " +
                                    (rec?.let { "${AttendanceStatus.fromCode(it.status).label} (${HisabCalculator.formatInr(it.earnedAmount)})" } ?: "Not marked"),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                listOf(
                                    AttendanceStatus.PRESENT to "P (1x)",
                                    AttendanceStatus.HALF_DAY to "½ (0.5x)",
                                    AttendanceStatus.ABSENT to "A (0x)",
                                    AttendanceStatus.OVERTIME to "OT (+₹)",
                                ).forEach { (st, lbl) ->
                                    val selected = rec?.status == st.code
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clickable {
                                                if (st == AttendanceStatus.OVERTIME) {
                                                    showOvertimeDialog = true
                                                } else {
                                                    onMarkAttendance(worker, selectedCalDate, st, 0L, "")
                                                }
                                            },
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = lbl,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                items(workerAttendance, key = { it.id }) { rec ->
                    val status = AttendanceStatus.fromCode(rec.status)
                    val color = when (status) {
                        AttendanceStatus.PRESENT -> hisabColors.income
                        AttendanceStatus.HALF_DAY -> hisabColors.halfDay
                        AttendanceStatus.ABSENT -> hisabColors.expense
                        AttendanceStatus.OVERTIME -> hisabColors.warning
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = HisabCalculator.formatDisplayDate(rec.date),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = buildString {
                                        append(status.hinglishLabel)
                                        if (rec.note.isNotBlank()) append(" • ${rec.note}")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = color,
                                )
                            }
                            Text(
                                text = "+${HisabCalculator.formatInr(rec.earnedAmount)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = color,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabourTransactionRow(
    tx: LabourTransaction,
    onDelete: () -> Unit,
) {
    val hisabColors = LocalHisabColors.current
    val txType = LabourTxType.fromCode(tx.type)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (txType == LabourTxType.SETTLEMENT) hisabColors.incomeContainer
                            else hisabColors.expenseContainer
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Payments,
                        contentDescription = null,
                        tint = if (txType == LabourTxType.SETTLEMENT) hisabColors.income else hisabColors.expense,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column {
                    Text(
                        text = "${txType.label} • ${tx.paymentMode}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "${HisabCalculator.formatDisplayDate(tx.date)} • ${tx.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "-${HisabCalculator.formatInr(tx.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = hisabColors.expense,
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete payout",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
