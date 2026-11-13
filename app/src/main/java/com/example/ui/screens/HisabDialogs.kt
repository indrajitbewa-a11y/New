package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CashbookType
import com.example.data.model.HisabCalculator
import com.example.data.model.LabourTxType
import com.example.data.model.PaymentMode
import com.example.data.model.Worker

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditWorkerDialog(
    initialWorker: Worker? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, role: String, dailyWage: Long, joiningDate: String, isActive: Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(initialWorker?.name ?: "") }
    var phone by remember { mutableStateOf(initialWorker?.phone ?: "") }
    var role by remember { mutableStateOf(initialWorker?.role ?: "Helper") }
    var dailyWageText by remember { mutableStateOf(initialWorker?.dailyWage?.toString() ?: "500") }
    var joiningDate by remember { mutableStateOf(initialWorker?.joiningDate ?: HisabCalculator.todayIso()) }
    var isActive by remember { mutableStateOf(initialWorker?.isActive ?: true) }
    var error by remember { mutableStateOf<String?>(null) }

    val roleSuggestions = listOf("Helper", "Mistri", "Sales Boy", "Karigar", "Tailor", "Driver", "Cleaner")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialWorker == null) "Add New Labour • Naya Staff" else "Edit Staff Profile",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Worker Name (Naam)*") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_name_input"),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15) },
                    label = { Text("Phone Number (WhatsApp/Call)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_phone_input"),
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it; error = null },
                    label = { Text("Role / Work Type (Kaam)*") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_role_input"),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    roleSuggestions.forEach { suggestion ->
                        FilterChip(
                            selected = role.equals(suggestion, ignoreCase = true),
                            onClick = { role = suggestion },
                            label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                OutlinedTextField(
                    value = dailyWageText,
                    onValueChange = { dailyWageText = it.filter { ch -> ch.isDigit() }; error = null },
                    label = { Text("Daily Wage Rate (Rozana Dihadi ₹)*") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_wage_input"),
                )
                OutlinedTextField(
                    value = joiningDate,
                    onValueChange = { joiningDate = it.take(10) },
                    label = { Text("Joining Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (initialWorker != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Active on Staff", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                    }
                }
                if (!error.isNullOrBlank()) {
                    Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val wage = dailyWageText.toLongOrNull() ?: 0L
                    val validDate = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$").matches(joiningDate)
                    when {
                        name.isBlank() -> error = "Please enter worker name"
                        role.isBlank() -> error = "Please enter work role"
                        wage <= 0L -> error = "Daily wage must be greater than ₹0"
                        !validDate -> error = "Use YYYY-MM-DD format for date"
                        else -> onSave(name.trim(), phone.trim(), role.trim(), wage, joiningDate, isActive)
                    }
                },
                modifier = Modifier.testTag("save_worker_button"),
            ) {
                Text(if (initialWorker == null) "Save Staff" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun OvertimeAmountDialog(
    worker: Worker,
    date: String,
    initialOvertime: Long,
    initialNote: String,
    onDismiss: () -> Unit,
    onConfirm: (overtimeAmount: Long, note: String) -> Unit,
) {
    var amountText by remember {
        mutableStateOf(if (initialOvertime > 0L) initialOvertime.toString() else "200")
    }
    var note by remember { mutableStateOf(initialNote) }
    var error by remember { mutableStateOf<String?>(null) }
    val presets = listOf(100L, 150L, 200L, 300L, 500L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Overtime (+Extra) • ${worker.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Date: ${HisabCalculator.formatDisplayDate(date)} | Base Rate: ${HisabCalculator.formatInr(worker.dailyWage)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = amountText == preset.toString(),
                            onClick = { amountText = preset.toString(); error = null },
                            label = { Text("+₹$preset") },
                        )
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() }; error = null },
                    label = { Text("Extra Overtime Amount (₹)") },
                    prefix = { Text("+₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("overtime_amount_input"),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (e.g. 2 hrs extra loading)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                val extra = amountText.toLongOrNull() ?: 0L
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Total Day Earning: ${HisabCalculator.formatInr(worker.dailyWage + extra)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(10.dp),
                    )
                }
                if (!error.isNullOrBlank()) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val extra = amountText.toLongOrNull() ?: -1L
                    if (extra < 0L) {
                        error = "Enter valid overtime amount"
                    } else {
                        onConfirm(extra, note)
                    }
                },
                modifier = Modifier.testTag("confirm_overtime_button"),
            ) {
                Text("Save Overtime")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddLabourTransactionDialog(
    worker: Worker,
    initialType: LabourTxType = LabourTxType.KHARCHA,
    initialAmount: Long? = null,
    initialNote: String = "",
    onDismiss: () -> Unit,
    onConfirm: (type: LabourTxType, amount: Long, date: String, paymentMode: PaymentMode, note: String) -> Unit,
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var amountText by remember { mutableStateOf(initialAmount?.toString() ?: "") }
    var date by remember { mutableStateOf(HisabCalculator.todayIso()) }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var note by remember { mutableStateOf(initialNote) }
    var error by remember { mutableStateOf<String?>(null) }

    val quickNotes = listOf("Chai / Nashta", "Weekly Advance", "Ghar Kharch", "Medical", "Full Hisab Clear")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialType == LabourTxType.SETTLEMENT) {
                    "Settle Hisab • ${worker.name}"
                } else {
                    "Add Kharcha / Advance • ${worker.name}"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    LabourTxType.entries.forEachIndexed { idx, txType ->
                        SegmentedButton(
                            selected = selectedType == txType,
                            onClick = { selectedType = txType },
                            shape = SegmentedButtonDefaults.itemShape(idx, LabourTxType.entries.size),
                        ) {
                            Text(txType.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() }; error = null },
                    label = { Text("Amount (Rakam ₹)*") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("labour_tx_amount_input"),
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(50L, 100L, 200L, 500L, 1000L).forEach { preset ->
                        FilterChip(
                            selected = amountText == preset.toString(),
                            onClick = { amountText = preset.toString(); error = null },
                            label = { Text("₹$preset") },
                        )
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    PaymentMode.entries.forEachIndexed { idx, mode ->
                        SegmentedButton(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            shape = SegmentedButtonDefaults.itemShape(idx, PaymentMode.entries.size),
                        ) {
                            Text(mode.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it.take(10) },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Reason (Kisliye diya)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("labour_tx_note_input"),
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    quickNotes.forEach { qn ->
                        FilterChip(
                            selected = note == qn,
                            onClick = { note = qn },
                            label = { Text(qn, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Auto-Sync: Yeh kharch automatically aapke Shop Galla (Cashbook) mein bhi jud jayega.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp),
                    )
                }

                if (!error.isNullOrBlank()) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: 0L
                    val validDate = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$").matches(date)
                    when {
                        amt <= 0L -> error = "Amount must be greater than ₹0"
                        !validDate -> error = "Use YYYY-MM-DD date format"
                        else -> onConfirm(selectedType, amt, date, paymentMode, note)
                    }
                },
                modifier = Modifier.testTag("confirm_labour_tx_button"),
            ) {
                Text(if (selectedType == LabourTxType.SETTLEMENT) "Settle & Sync" else "Save & Sync")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCashbookEntryDialog(
    initialType: CashbookType = CashbookType.INCOME,
    onDismiss: () -> Unit,
    onConfirm: (type: CashbookType, category: String, amount: Long, paymentMode: PaymentMode, date: String, note: String) -> Unit,
) {
    var selectedType by remember { mutableStateOf(initialType) }
    val incomeCategories = listOf("Shop Sales (Bikri)", "Udhar Vasooli", "Other Income")
    val expenseCategories = listOf("Stock Purchase (Maal)", "Tea & Snacks", "Rent (Kiraya)", "Electricity (Bijli)", "Transport", "Other Expense")

    var category by remember {
        mutableStateOf(if (initialType == CashbookType.INCOME) incomeCategories.first() else expenseCategories.first())
    }
    var amountText by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var date by remember { mutableStateOf(HisabCalculator.todayIso()) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Galla Entry • Shop Cashbook") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    CashbookType.entries.forEachIndexed { idx, t ->
                        SegmentedButton(
                            selected = selectedType == t,
                            onClick = {
                                selectedType = t
                                category = if (t == CashbookType.INCOME) incomeCategories.first() else expenseCategories.first()
                            },
                            shape = SegmentedButtonDefaults.itemShape(idx, CashbookType.entries.size),
                        ) {
                            Text(
                                text = if (t == CashbookType.INCOME) "+ Income" else "- Expense",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                val cats = if (selectedType == CashbookType.INCOME) incomeCategories else expenseCategories
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    cats.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() }; error = null },
                    label = { Text("Amount (Rakam ₹)*") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cashbook_amount_input"),
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(200L, 500L, 1000L, 2000L, 5000L).forEach { preset ->
                        FilterChip(
                            selected = amountText == preset.toString(),
                            onClick = { amountText = preset.toString(); error = null },
                            label = { Text("₹$preset") },
                        )
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    PaymentMode.entries.forEachIndexed { idx, mode ->
                        SegmentedButton(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            shape = SegmentedButtonDefaults.itemShape(idx, PaymentMode.entries.size),
                        ) {
                            Text(mode.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it.take(10) },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Bill Details (Optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cashbook_note_input"),
                )

                if (!error.isNullOrBlank()) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: 0L
                    val validDate = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$").matches(date)
                    when {
                        amt <= 0L -> error = "Amount must be greater than ₹0"
                        !validDate -> error = "Use YYYY-MM-DD date format"
                        else -> onConfirm(selectedType, category, amt, paymentMode, date, note)
                    }
                },
                modifier = Modifier.testTag("save_cashbook_button"),
            ) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
