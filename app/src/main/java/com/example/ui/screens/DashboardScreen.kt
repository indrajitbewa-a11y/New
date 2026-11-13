package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CashbookType
import com.example.data.model.HisabCalculator
import com.example.data.model.WorkerLedgerSummary
import com.example.ui.theme.LocalHisabColors
import com.example.ui.viewmodel.HisabUiState

@Composable
fun DashboardScreen(
    state: HisabUiState.Ready,
    onMarkAttendanceClick: () -> Unit,
    onAddCashbookClick: () -> Unit,
    onAddLabourClick: () -> Unit,
    onOpenWorkerDetail: (String) -> Unit,
    onOpenGallaTab: () -> Unit,
) {
    val hisabColors = LocalHisabColors.current
    val pendingWorkers = state.workerSummaries.values
        .filter { it.remainingBalance > 0L }
        .sortedByDescending { it.remainingBalance }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. Top Quick Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Present Staff Card
                SummaryMetricCard(
                    title = "Aaj ka Staff",
                    subtitle = "Present Today",
                    value = "${state.todayPresentCount} / ${state.activeWorkersCount}",
                    caption = if (state.todayHalfDayCount > 0) {
                        "+ ${state.todayHalfDayCount} Half-Day"
                    } else {
                        "Tap for Haziri"
                    },
                    icon = Icons.Filled.Groups,
                    containerColor = hisabColors.incomeContainer,
                    contentColor = hisabColors.onIncomeContainer,
                    accentColor = hisabColors.income,
                    onClick = onMarkAttendanceClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_present_card"),
                )

                // Total Pending Labour Balance Card
                SummaryMetricCard(
                    title = "Total Baki",
                    subtitle = "Labour Payable",
                    value = HisabCalculator.formatInr(state.totalPendingLabourBalance),
                    caption = "${pendingWorkers.size} staff pending",
                    icon = Icons.Filled.Payments,
                    containerColor = hisabColors.warningContainer,
                    contentColor = hisabColors.onWarningContainer,
                    accentColor = hisabColors.warning,
                    onClick = {
                        val topWorker = pendingWorkers.firstOrNull()?.worker?.id
                        if (topWorker != null) onOpenWorkerDetail(topWorker) else onAddLabourClick()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_pending_balance_card"),
                )
            }
        }

        // 2. Today's Shop Cashbook (Galla) Hero Card
        item {
            val galla = state.todayCashbookSummary
            Card(
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenGallaTab)
                    .testTag("dashboard_galla_card"),
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
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "AAJ KA GALLA • TODAY'S CASHBOOK",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.78f),
                                )
                                Text(
                                    text = HisabCalculator.formatDisplayDate(state.todayDate),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(99.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = "Open Galla",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = "Net Cash in Hand (Bachat)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f),
                            )
                            Text(
                                text = HisabCalculator.formatInr(galla.netBalance),
                                style = MaterialTheme.typography.displaySmall,
                                color = if (galla.netBalance >= 0) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            GallaPill(
                                label = "Income (+)",
                                amount = HisabCalculator.formatInr(galla.totalIncome),
                                icon = Icons.Filled.TrendingUp,
                                tint = Color(0xFF6EE7B7),
                                modifier = Modifier.weight(1f),
                            )
                            GallaPill(
                                label = "Expense (-)",
                                amount = HisabCalculator.formatInr(galla.totalExpense),
                                icon = Icons.Filled.TrendingDown,
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Text(
                            text = "Cash Net: ${HisabCalculator.formatInr(galla.cashNet)}   •   UPI Net: ${HisabCalculator.formatInr(galla.upiNet)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        // 3. Quick Action Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Quick Actions • Turant Kaam",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Button(
                    onClick = onMarkAttendanceClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("quick_mark_attendance_button"),
                ) {
                    Icon(Icons.Filled.EventAvailable, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Mark Attendance • Aaj ki Haziri Lagayein",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilledTonalButton(
                        onClick = onAddCashbookClick,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("quick_add_cashbook_button"),
                    ) {
                        Icon(Icons.Filled.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Shop Income/Exp", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    FilledTonalButton(
                        onClick = onAddLabourClick,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("quick_add_labour_button"),
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add New Labour", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // 4. Empty state OR Worker Baki Hisab List
        if (state.workers.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_workers),
                            contentDescription = "No staff added illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                        Text(
                            text = "Apna Pehla Staff Member Jodein",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Mistri, Helper, ya Sales staff add karein aur daily haziri + kharcha manage karein.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = onAddLabourClick) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add First Worker")
                        }
                    }
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Staff Baki Hisab • Payable",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(onClick = onMarkAttendanceClick) {
                        Text("All Staff (${state.workers.size})")
                    }
                }
            }

            val topList = state.workerSummaries.values
                .sortedByDescending { it.remainingBalance }
                .take(5)

            items(topList, key = { it.worker.id }) { summary ->
                DashboardWorkerBalanceRow(
                    summary = summary,
                    onClick = { onOpenWorkerDetail(summary.worker.id) },
                )
            }
        }

        // 5. Recent Today Cashbook Activity
        val todayEntries = state.cashbookEntries.filter { it.date == state.todayDate }.take(4)
        if (todayEntries.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Aaj ke Galla Entries",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(onClick = onOpenGallaTab) {
                        Text("See All")
                    }
                }
            }
            items(todayEntries, key = { it.id }) { entry ->
                val isIncome = entry.type == CashbookType.INCOME.code
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isIncome) hisabColors.incomeContainer else hisabColors.expenseContainer
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = if (isIncome) hisabColors.income else hisabColors.expense,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = entry.category,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = listOfNotNull(
                                        entry.paymentMode,
                                        entry.note.takeIf { it.isNotBlank() },
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Text(
                            text = "${if (isIncome) "+" else "-"}${HisabCalculator.formatInr(entry.amount)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncome) hisabColors.income else hisabColors.expense,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricCard(
    title: String,
    subtitle: String,
    value: String,
    caption: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor,
            )
            Text(
                text = "$subtitle • $caption",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GallaPill(
    label: String,
    amount: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun DashboardWorkerBalanceRow(
    summary: WorkerLedgerSummary,
    onClick: () -> Unit,
) {
    val hisabColors = LocalHisabColors.current
    val worker = summary.worker
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("worker_card_${worker.id}"),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
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
                        style = MaterialTheme.typography.titleSmall,
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
                val bal = summary.remainingBalance
                Text(
                    text = HisabCalculator.formatInr(bal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        bal > 0L -> hisabColors.warning
                        bal < 0L -> hisabColors.expense
                        else -> hisabColors.income
                    },
                )
                Text(
                    text = when {
                        bal > 0L -> "Baki Dena"
                        bal < 0L -> "Extra Adv"
                        else -> "Clear ✓"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
