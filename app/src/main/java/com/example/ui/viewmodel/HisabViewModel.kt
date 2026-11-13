package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.CashbookEntry
import com.example.data.model.CashbookPeriod
import com.example.data.model.CashbookSummary
import com.example.data.model.CashbookType
import com.example.data.model.HisabCalculator
import com.example.data.model.LabourTransaction
import com.example.data.model.LabourTxType
import com.example.data.model.PaymentMode
import com.example.data.model.Worker
import com.example.data.model.WorkerLedgerSummary
import com.example.data.repository.HisabRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HisabUiState {
    data object Loading : HisabUiState
    data class Ready(
        val workers: List<Worker>,
        val workerSummaries: Map<String, WorkerLedgerSummary>,
        val attendanceRecords: List<AttendanceRecord>,
        val labourTransactions: List<LabourTransaction>,
        val cashbookEntries: List<CashbookEntry>,
        val todayDate: String,
        val todayPresentCount: Int,
        val todayHalfDayCount: Int,
        val activeWorkersCount: Int,
        val totalPendingLabourBalance: Long,
        val todayCashbookSummary: CashbookSummary,
        val filteredCashbookEntries: List<CashbookEntry>,
        val filteredCashbookSummary: CashbookSummary,
        val selectedCashbookPeriod: CashbookPeriod,
        val selectedAttendanceDate: String,
        val hasPendingWrites: Boolean,
    ) : HisabUiState

    data class Error(val message: String) : HisabUiState
}

class HisabViewModel(
    private val repository: HisabRepository,
    private val currentUserId: String,
) : ViewModel() {

    private val _selectedAttendanceDate = MutableStateFlow(HisabCalculator.todayIso())
    val selectedAttendanceDate: StateFlow<String> = _selectedAttendanceDate.asStateFlow()

    private val _selectedCashbookPeriod = MutableStateFlow(CashbookPeriod.DAILY)
    val selectedCashbookPeriod: StateFlow<CashbookPeriod> = _selectedCashbookPeriod.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val dataFlow = combine(
        repository.observeWorkers(currentUserId),
        repository.observeAttendance(currentUserId),
        repository.observeLabourTransactions(currentUserId),
        repository.observeCashbook(currentUserId),
        repository.hasPendingWrites,
    ) { workers, attendance, transactions, cashbook, pending ->
        RawBundle(workers, attendance, transactions, cashbook, pending)
    }

    val uiState: StateFlow<HisabUiState> = combine(
        dataFlow,
        _selectedAttendanceDate,
        _selectedCashbookPeriod,
    ) { bundle, attendanceDate, period ->
        val today = HisabCalculator.todayIso()
        val summaries = bundle.workers.associate { worker ->
            worker.id to HisabCalculator.computeWorkerSummary(
                worker = worker,
                attendanceRecords = bundle.attendance,
                transactions = bundle.transactions,
            )
        }

        val todayAttendance = bundle.attendance.filter { it.date == today }
        val todayPresent = todayAttendance.count {
            it.status == AttendanceStatus.PRESENT.code || it.status == AttendanceStatus.OVERTIME.code
        }
        val todayHalf = todayAttendance.count { it.status == AttendanceStatus.HALF_DAY.code }
        val activeCount = bundle.workers.count { it.isActive }
        val totalPayable = summaries.values.sumOf { maxOf(0L, it.remainingBalance) }

        val todayCashbookEntries = HisabCalculator.filterCashbookByPeriod(
            entries = bundle.cashbook,
            period = CashbookPeriod.DAILY,
            referenceDateIso = today,
        )
        val todayCashSummary = HisabCalculator.computeCashbookSummary(todayCashbookEntries)

        val filteredEntries = HisabCalculator.filterCashbookByPeriod(
            entries = bundle.cashbook,
            period = period,
            referenceDateIso = today,
        )
        val filteredSummary = HisabCalculator.computeCashbookSummary(filteredEntries)

        HisabUiState.Ready(
            workers = bundle.workers,
            workerSummaries = summaries,
            attendanceRecords = bundle.attendance,
            labourTransactions = bundle.transactions,
            cashbookEntries = bundle.cashbook,
            todayDate = today,
            todayPresentCount = todayPresent,
            todayHalfDayCount = todayHalf,
            activeWorkersCount = activeCount,
            totalPendingLabourBalance = totalPayable,
            todayCashbookSummary = todayCashSummary,
            filteredCashbookEntries = filteredEntries,
            filteredCashbookSummary = filteredSummary,
            selectedCashbookPeriod = period,
            selectedAttendanceDate = attendanceDate,
            hasPendingWrites = bundle.pending,
        ) as HisabUiState
    }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing Hisab data", rootError)
            emit(HisabUiState.Error(rootError.message ?: "Failed to sync data from Firestore"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = HisabUiState.Loading,
        )

    fun setAttendanceDate(isoDate: String) {
        _selectedAttendanceDate.value = isoDate
    }

    fun setCashbookPeriod(period: CashbookPeriod) {
        _selectedCashbookPeriod.value = period
    }

    fun addWorker(
        name: String,
        phone: String,
        role: String,
        dailyWage: Long,
        joiningDate: String,
        onDone: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.addWorker(name, phone, role, dailyWage, joiningDate)
            _isBusy.value = false
            res.onSuccess { id ->
                _events.tryEmit("$name staff mein jud gaye")
                onDone(id)
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to add worker")
            }
        }
    }

    fun updateWorker(
        worker: Worker,
        name: String,
        phone: String,
        role: String,
        dailyWage: Long,
        joiningDate: String,
        isActive: Boolean,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateWorker(worker, name, phone, role, dailyWage, joiningDate, isActive)
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("Profile updated: $name")
                onDone()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Update failed")
            }
        }
    }

    fun deleteWorker(worker: Worker, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.deleteWorker(worker.id)
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("${worker.name} removed")
                onDone()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Delete failed")
            }
        }
    }

    fun markAttendance(
        worker: Worker,
        date: String,
        status: AttendanceStatus,
        overtimeAmount: Long = 0L,
        note: String = "",
    ) {
        viewModelScope.launch {
            val res = repository.markAttendance(worker, date, status, overtimeAmount, note)
            res.onSuccess {
                _events.tryEmit("${worker.name}: ${status.label}")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Could not save attendance")
            }
        }
    }

    fun addLabourTransaction(
        worker: Worker,
        type: LabourTxType,
        amount: Long,
        date: String,
        paymentMode: PaymentMode,
        note: String,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.addLabourTransaction(worker, type, amount, date, paymentMode, note)
            _isBusy.value = false
            res.onSuccess {
                val label = if (type == LabourTxType.SETTLEMENT) "Hisab Settled!" else "${type.label} saved & synced to Galla"
                _events.tryEmit(label)
                onDone()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to record payment")
            }
        }
    }

    fun deleteLabourTransaction(tx: LabourTransaction) {
        viewModelScope.launch {
            val res = repository.deleteLabourTransaction(tx)
            res.onSuccess {
                _events.tryEmit("Entry deleted from ledger & Galla")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Delete failed")
            }
        }
    }

    fun addCashbookEntry(
        type: CashbookType,
        category: String,
        amount: Long,
        paymentMode: PaymentMode,
        date: String,
        note: String,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.addCashbookEntry(type, category, amount, paymentMode, date, note)
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("Galla entry saved (${HisabCalculator.formatInr(amount)})")
                onDone()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to save Galla entry")
            }
        }
    }

    fun deleteCashbookEntry(entry: CashbookEntry) {
        viewModelScope.launch {
            val res = repository.deleteCashbookEntry(entry)
            res.onSuccess {
                _events.tryEmit("Galla entry deleted")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Delete failed")
            }
        }
    }

    private data class RawBundle(
        val workers: List<Worker>,
        val attendance: List<AttendanceRecord>,
        val transactions: List<LabourTransaction>,
        val cashbook: List<CashbookEntry>,
        val pending: Boolean,
    )

    private companion object {
        const val TAG = "HisabViewModel"
    }
}
