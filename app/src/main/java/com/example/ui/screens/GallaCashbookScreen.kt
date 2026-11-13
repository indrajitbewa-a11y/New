package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CashbookEntry
import com.example.data.model.CashbookPeriod
import com.example.data.model.CashbookType
import com.example.data.model.HisabCalculator
import com.example.ui.theme.LocalHisabColors
import com.example.ui.viewmodel.HisabUiState

@Composable
fun GallaCashbookScreen(
    state: HisabUiState.Ready,
    onSelectPeriod: (CashbookPeriod) -> Unit,
    onAddEntryClick: (CashbookType) -> Unit,
    onDeleteEntry: (CashbookEntry) -> Unit,
) {
    val hisabColors = LocalHisabColors.current
    var typeFilter by remember { mutableStateOf<CashbookType?>(null) }

    val visibleEntries = remember(state.filteredCashbookEntries, typeFilter) {
        if (typeFilter == null) {
            state.filteredCashbookEntries
        } else {
            state.filteredCashbookEntries.filter { it.type == typeFilter?.code }
        }
    }

    val summary = state.filteredCashbookSummary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Period Filter (Daily / Weekly / Monthly)
        item {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("galla_period_selector"),
            ) {
                CashbookPeriod.entries.forEachIndexed { idx, period ->
                    SegmentedButton(
                        selected = state.selectedCashbookPeriod == period,
                        onClick = { onSelectPeriod(period) },
                        shape = SegmentedButtonDefaults.itemShape(idx, CashbookPeriod.entries.size),
                    ) {
                        Text(period.hinglishLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // 2. Profit / Loss Summary Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "${state.selectedCashbookPeriod.hinglishLabel} • Net Profit / Loss",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = HisabCalculator.formatInr(summary.netBalance),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (summary.netBalance >= 0L) hisabColors.income else hisabColors.expense,
                                modifier = Modifier.testTag("galla_net_balance"),
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = if (summary.netBalance >= 0L) hisabColors.incomeContainer else hisabColors.expenseContainer,
                        ) {
                            Text(
                                text = if (summary.netBalance >= 0L) "PROFIT • NAFA" else "LOSS • GHATA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.netBalance >= 0L) hisabColors.onIncomeContainer else hisabColors.onExpenseContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            color = hisabColors.incomeContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Total Income (+)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = hisabColors.onIncomeContainer,
                                )
                                Text(
                                    text = HisabCalculator.formatInr(summary.totalIncome),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = hisabColors.income,
                                )
                            }
                        }
                        Surface(
                            color = hisabColors.expenseContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Total Expense (-)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = hisabColors.onExpenseContainer,
                                )
                                Text(
                                    text = HisabCalculator.formatInr(summary.totalExpense),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = hisabColors.expense,
                                )
                            }
                        }
                    }

                    Text(
                        text = "Cash (Nagad): ${HisabCalculator.formatInr(summary.cashNet)}   •   UPI: ${HisabCalculator.formatInr(summary.upiNet)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // 3. Quick Add Income / Expense Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { onAddEntryClick(CashbookType.INCOME) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = hisabColors.income,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("add_income_button"),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Income (+)")
                }
                Button(
                    onClick = { onAddEntryClick(CashbookType.EXPENSE) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = hisabColors.expense,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("add_expense_button"),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Expense (-)")
                }
            }
        }

        // 4. Filter Chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = typeFilter == null,
                    onClick = { typeFilter = null },
                    label = { Text("All (${state.filteredCashbookEntries.size})") },
                )
                FilterChip(
                    selected = typeFilter == CashbookType.INCOME,
                    onClick = { typeFilter = CashbookType.INCOME },
                    label = { Text("Income (+)") },
                )
                FilterChip(
                    selected = typeFilter == CashbookType.EXPENSE,
                    onClick = { typeFilter = CashbookType.EXPENSE },
                    label = { Text("Expense (-)") },
                )
            }
        }

        if (visibleEntries.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Is period mein koi Galla entry nahi hai",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "Upar '+' buttons se aaj ki bikri (income) ya dukaan kharch (expense) add karein.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(visibleEntries, key = { it.id }) { entry ->
                CashbookEntryRow(
                    entry = entry,
                    onDelete = { onDeleteEntry(entry) },
                )
            }
        }
    }
}

@Composable
private fun CashbookEntryRow(
    entry: CashbookEntry,
    onDelete: () -> Unit,
) {
    val hisabColors = LocalHisabColors.current
    val isIncome = entry.type == CashbookType.INCOME.code
    val isLabourSynced = entry.linkedLabourTxId.isNotBlank()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cashbook_entry_${entry.id}"),
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
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isIncome) hisabColors.incomeContainer else hisabColors.expenseContainer
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (isIncome) hisabColors.income else hisabColors.expense,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = entry.category,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = entry.paymentMode,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text = listOfNotNull(
                            HisabCalculator.formatDisplayDate(entry.date),
                            entry.note.takeIf { it.isNotBlank() },
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isLabourSynced) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = "Auto-synced from Labour Ledger",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (isIncome) "+" else "-"}${HisabCalculator.formatInr(entry.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) hisabColors.income else hisabColors.expense,
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete entry",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
