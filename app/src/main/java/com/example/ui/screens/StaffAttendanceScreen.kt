package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.HisabCalculator
import com.example.data.model.Worker
import com.example.ui.theme.LocalHisabColors
import com.example.ui.viewmodel.HisabUiState

@Composable
fun StaffAttendanceScreen(
    state: HisabUiState.Ready,
    onSelectDate: (String) -> Unit,
    onMarkAttendance: (Worker, String, AttendanceStatus, Long, String) -> Unit,
    onOpenWorkerDetail: (String) -> Unit,
    onAddWorkerClick: () -> Unit,
) {
    val selectedDate = state.selectedAttendanceDate
    var showCalendar by remember { mutableStateOf(false) }
    var calendarMonth by remember(selectedDate) { mutableStateOf(selectedDate.take(7)) }
    var searchQuery by remember { mutableStateOf("") }
    var overtimeWorker by remember { mutableStateOf<Worker?>(null) }

    val recordsByWorker = remember(state.attendanceRecords, selectedDate) {
        state.attendanceRecords
            .filter { it.date == selectedDate }
            .associateBy { it.workerId }
    }

    val datesWithAttendance = remember(state.attendanceRecords) {
        state.attendanceRecords.map { it.date }.toSet()
    }

    val filteredWorkers = remember(state.workers, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) {
            state.workers
        } else {
            state.workers.filter {
                it.name.lowercase().contains(q) || it.role.lowercase().contains(q)
            }
        }
    }

    overtimeWorker?.let { worker ->
        val existing = recordsByWorker[worker.id]
        OvertimeAmountDialog(
            worker = worker,
            date = selectedDate,
            initialOvertime = existing?.overtimeAmount ?: 200L,
            initialNote = existing?.note ?: "",
            onDismiss = { overtimeWorker = null },
            onConfirm = { extra, note ->
                onMarkAttendance(worker, selectedDate, AttendanceStatus.OVERTIME, extra, note)
                overtimeWorker = null
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Date Bar & Calendar Toggle
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { onSelectDate(HisabCalculator.shiftIsoDate(selectedDate, -1)) },
                            modifier = Modifier.testTag("prev_day_button"),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showCalendar = !showCalendar }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = if (selectedDate == state.todayDate) {
                                    "Aaj • ${HisabCalculator.formatDisplayDate(selectedDate)}"
                                } else {
                                    HisabCalculator.formatDisplayDate(selectedDate)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (showCalendar) "Hide Calendar" else "Tap for Calendar View",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onSelectDate(HisabCalculator.shiftIsoDate(selectedDate, 1)) },
                                modifier = Modifier.testTag("next_day_button"),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
                            }
                            IconButton(
                                onClick = { showCalendar = !showCalendar },
                                modifier = Modifier.testTag("toggle_calendar_button"),
                            ) {
                                Icon(
                                    Icons.Filled.CalendarMonth,
                                    contentDescription = "Toggle Calendar",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    if (selectedDate != state.todayDate) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            TextButton(onClick = { onSelectDate(state.todayDate) }) {
                                Text("Jump to Today (Aaj)")
                            }
                        }
                    }

                    AnimatedVisibility(visible = showCalendar) {
                        MonthAttendanceCalendar(
                            yearMonth = calendarMonth,
                            selectedDate = selectedDate,
                            markedDates = datesWithAttendance,
                            onPrevMonth = { calendarMonth = HisabCalculator.shiftYearMonth(calendarMonth, -1) },
                            onNextMonth = { calendarMonth = HisabCalculator.shiftYearMonth(calendarMonth, 1) },
                            onDateSelected = {
                                onSelectDate(it)
                                showCalendar = false
                            },
                        )
                    }
                }
            }
        }

        // 2. Search + Add Staff Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search staff...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = onAddWorkerClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("staff_add_button"),
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = "Add staff")
                    Spacer(Modifier.width(6.dp))
                    Text("Add")
                }
            }
        }

        if (filteredWorkers.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_workers),
                            contentDescription = "No workers illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                        Text(
                            text = if (state.workers.isEmpty()) "Abhi koi staff nahi hai" else "Koi matching worker nahi mila",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (state.workers.isEmpty()) {
                            Button(onClick = onAddWorkerClick) {
                                Text("Add New Labour")
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredWorkers, key = { it.id }) { worker ->
                val record = recordsByWorker[worker.id]
                val summary = state.workerSummaries[worker.id]
                WorkerAttendanceCard(
                    worker = worker,
                    record = record,
                    remainingBalance = summary?.remainingBalance ?: 0L,
                    onTapStatus = { status ->
                        if (status == AttendanceStatus.OVERTIME) {
                            overtimeWorker = worker
                        } else {
                            onMarkAttendance(worker, selectedDate, status, 0L, "")
                        }
                    },
                    onOpenDetail = { onOpenWorkerDetail(worker.id) },
                )
            }
        }
    }
}

@Composable
private fun WorkerAttendanceCard(
    worker: Worker,
    record: AttendanceRecord?,
    remainingBalance: Long,
    onTapStatus: (AttendanceStatus) -> Unit,
    onOpenDetail: () -> Unit,
) {
    val hisabColors = LocalHisabColors.current
    val currentStatus = record?.let { AttendanceStatus.fromCode(it.status) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attendance_card_${worker.id}"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenDetail),
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = worker.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Column {
                        Text(
                            text = worker.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${worker.role} • ${HisabCalculator.formatInr(worker.dailyWage)}/day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (record != null) {
                            "+${HisabCalculator.formatInr(record.earnedAmount)}"
                        } else {
                            "Not Marked"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (record != null) hisabColors.income else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Baki: ${HisabCalculator.formatInr(remainingBalance)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (remainingBalance > 0L) hisabColors.warning else hisabColors.income,
                    )
                }
            }

            // One-Tap Attendance Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AttendanceTapChip(
                    label = "P (1x)",
                    selected = currentStatus == AttendanceStatus.PRESENT,
                    activeBg = hisabColors.income,
                    activeFg = Color.White,
                    onClick = { onTapStatus(AttendanceStatus.PRESENT) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mark_present_${worker.id}"),
                )
                AttendanceTapChip(
                    label = "½ (0.5x)",
                    selected = currentStatus == AttendanceStatus.HALF_DAY,
                    activeBg = hisabColors.halfDay,
                    activeFg = Color.White,
                    onClick = { onTapStatus(AttendanceStatus.HALF_DAY) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mark_half_${worker.id}"),
                )
                AttendanceTapChip(
                    label = "A (0x)",
                    selected = currentStatus == AttendanceStatus.ABSENT,
                    activeBg = hisabColors.expense,
                    activeFg = Color.White,
                    onClick = { onTapStatus(AttendanceStatus.ABSENT) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mark_absent_${worker.id}"),
                )
                AttendanceTapChip(
                    label = if (currentStatus == AttendanceStatus.OVERTIME && record != null) {
                        "OT +₹${record.overtimeAmount}"
                    } else {
                        "OT (+₹)"
                    },
                    selected = currentStatus == AttendanceStatus.OVERTIME,
                    activeBg = hisabColors.warning,
                    activeFg = Color.White,
                    onClick = { onTapStatus(AttendanceStatus.OVERTIME) },
                    modifier = Modifier
                        .weight(1.15f)
                        .testTag("mark_overtime_${worker.id}"),
                )
            }
        }
    }
}

@Composable
private fun AttendanceTapChip(
    label: String,
    selected: Boolean,
    activeBg: Color,
    activeFg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) activeBg else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) activeFg else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun MonthAttendanceCalendar(
    yearMonth: String,
    selectedDate: String,
    markedDates: Set<String>,
    dayStatusColorProvider: ((String) -> Color?)? = null,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (String) -> Unit,
) {
    val days = remember(yearMonth) { HisabCalculator.buildMonthDays(yearMonth) }
    val offset = remember(yearMonth) { HisabCalculator.firstDayWeekdayOffset(yearMonth) }
    val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(
                text = HisabCalculator.formatMonthTitle(yearMonth),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { d ->
                Text(
                    text = d,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        val totalCells = offset + days.size
        val rows = (totalCells + 6) / 7
        for (r in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (c in 0 until 7) {
                    val idx = r * 7 + c
                    val dayIdx = idx - offset
                    if (dayIdx !in days.indices) {
                        Spacer(modifier = Modifier.weight(1f).height(40.dp))
                    } else {
                        val dateIso = days[dayIdx]
                        val dayNum = dateIso.takeLast(2).toIntOrNull()?.toString() ?: ""
                        val isSelected = dateIso == selectedDate
                        val customColor = dayStatusColorProvider?.invoke(dateIso)
                        val isMarked = dateIso in markedDates

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                customColor != null -> customColor.copy(alpha = if (isSelected) 1f else 0.22f)
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> Color.Transparent
                            },
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clickable { onDateSelected(dateIso) },
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = dayNum,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isSelected && customColor == null -> MaterialTheme.colorScheme.onPrimary
                                        isSelected && customColor != null -> Color.White
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                )
                                if (isMarked && customColor == null) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.tertiary
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
