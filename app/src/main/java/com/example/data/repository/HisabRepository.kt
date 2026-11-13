package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.CashbookEntry
import com.example.data.model.CashbookType
import com.example.data.model.HisabCalculator
import com.example.data.model.LabourTransaction
import com.example.data.model.LabourTxType
import com.example.data.model.PaymentMode
import com.example.data.model.Worker
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class HisabRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
    )

    private val _hasPendingWrites = MutableStateFlow(false)
    val hasPendingWrites: StateFlow<Boolean> = _hasPendingWrites.asStateFlow()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun workersCollection(uid: String) =
        db.collection("users").document(uid).collection("workers")

    private fun attendanceCollection(uid: String) =
        db.collection("users").document(uid).collection("attendance")

    private fun labourTransactionsCollection(uid: String) =
        db.collection("users").document(uid).collection("labour_transactions")

    private fun cashbookCollection(uid: String) =
        db.collection("users").document(uid).collection("cashbook")

    fun observeWorkers(userId: String): Flow<List<Worker>> = flow {
        val col = workersCollection(userId)
        emitAll(
            col.snapshots()
                .map { snapshot ->
                    _hasPendingWrites.value = snapshot.metadata.hasPendingWrites()
                    snapshot.toObjects(
                        Worker::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, col.path, userId)
                }
        )
    }

    fun observeAttendance(userId: String): Flow<List<AttendanceRecord>> = flow {
        val col = attendanceCollection(userId)
        emitAll(
            col.snapshots()
                .map { snapshot ->
                    if (snapshot.metadata.hasPendingWrites()) {
                        _hasPendingWrites.value = true
                    }
                    snapshot.toObjects(
                        AttendanceRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.date }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, col.path, userId)
                }
        )
    }

    fun observeLabourTransactions(userId: String): Flow<List<LabourTransaction>> = flow {
        val col = labourTransactionsCollection(userId)
        emitAll(
            col.snapshots()
                .map { snapshot ->
                    if (snapshot.metadata.hasPendingWrites()) {
                        _hasPendingWrites.value = true
                    }
                    snapshot.toObjects(
                        LabourTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<LabourTransaction> { it.date }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, col.path, userId)
                }
        )
    }

    fun observeCashbook(userId: String): Flow<List<CashbookEntry>> = flow {
        val col = cashbookCollection(userId)
        emitAll(
            col.snapshots()
                .map { snapshot ->
                    if (snapshot.metadata.hasPendingWrites()) {
                        _hasPendingWrites.value = true
                    }
                    snapshot.toObjects(
                        CashbookEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<CashbookEntry> { it.date }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, col.path, userId)
                }
        )
    }

    private fun handleFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String,
    ): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    suspend fun addWorker(
        name: String,
        phone: String,
        role: String,
        dailyWage: Long,
        joiningDate: String,
    ): Result<String> {
        val uid = requireUserId()
        val docRef = workersCollection(uid).document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim(),
            "phone" to phone.trim(),
            "role" to role.trim(),
            "dailyWage" to dailyWage,
            "joiningDate" to joiningDate,
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateWorker(
        worker: Worker,
        name: String,
        phone: String,
        role: String,
        dailyWage: Long,
        joiningDate: String,
        isActive: Boolean,
    ): Result<Unit> {
        val uid = requireUserId()
        require(worker.id.isNotBlank()) { "Worker ID must not be blank" }
        val docRef = workersCollection(uid).document(worker.id)
        val updates = mapOf(
            "name" to name.trim(),
            "phone" to phone.trim(),
            "role" to role.trim(),
            "dailyWage" to dailyWage,
            "joiningDate" to joiningDate,
            "isActive" to isActive,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteWorker(workerId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = workersCollection(uid).document(workerId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markAttendance(
        worker: Worker,
        date: String,
        status: AttendanceStatus,
        overtimeAmount: Long = 0L,
        note: String = "",
    ): Result<Unit> {
        val uid = requireUserId()
        require(worker.id.isNotBlank()) { "Worker ID must not be blank" }
        val attendanceId = "${worker.id}_$date"
        val docRef = attendanceCollection(uid).document(attendanceId)
        val cleanOvertime = if (status == AttendanceStatus.OVERTIME) overtimeAmount.coerceAtLeast(0L) else 0L
        val earned = HisabCalculator.calculateEarnedForAttendance(
            dailyWage = worker.dailyWage,
            status = status,
            overtimeAmount = cleanOvertime,
        )

        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updates = mapOf(
                    "status" to status.code,
                    "dailyWageSnapshot" to worker.dailyWage,
                    "overtimeAmount" to cleanOvertime,
                    "earnedAmount" to earned,
                    "note" to note.trim(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "workerId" to worker.id,
                    "date" to date,
                    "status" to status.code,
                    "dailyWageSnapshot" to worker.dailyWage,
                    "overtimeAmount" to cleanOvertime,
                    "earnedAmount" to earned,
                    "note" to note.trim(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Atomically records a worker Kharcha/Advance/Settlement payout AND syncs it as a
     * "Labour Payout" expense in the Shop Cashbook (Galla) in the same WriteBatch.
     */
    suspend fun addLabourTransaction(
        worker: Worker,
        type: LabourTxType,
        amount: Long,
        date: String,
        paymentMode: PaymentMode,
        note: String,
    ): Result<String> {
        val uid = requireUserId()
        require(worker.id.isNotBlank()) { "Worker ID must not be blank" }
        require(amount > 0L) { "Amount must be positive" }

        val txRef = labourTransactionsCollection(uid).document()
        val cashbookRef = cashbookCollection(uid).document()

        val cleanNote = note.trim().ifBlank { type.label }
        val cashbookNote = "${type.label} - ${worker.name}" +
            if (note.isNotBlank()) " (${note.trim()})" else ""

        val txPayload = mapOf(
            "userId" to uid,
            "workerId" to worker.id,
            "workerName" to worker.name,
            "type" to type.code,
            "amount" to amount,
            "date" to date,
            "paymentMode" to paymentMode.code,
            "note" to cleanNote.take(200),
            "linkedCashbookId" to cashbookRef.id,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )

        val cashbookPayload = mapOf(
            "userId" to uid,
            "type" to CashbookType.EXPENSE.code,
            "category" to "Labour Payout",
            "amount" to amount,
            "paymentMode" to paymentMode.code,
            "date" to date,
            "note" to cashbookNote.take(200),
            "linkedWorkerId" to worker.id,
            "linkedLabourTxId" to txRef.id,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )

        return try {
            val batch = db.batch()
            batch.set(txRef, txPayload)
            batch.set(cashbookRef, cashbookPayload)
            batch.commit().await()
            Result.success(txRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, txRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteLabourTransaction(tx: LabourTransaction): Result<Unit> {
        val uid = requireUserId()
        require(tx.id.isNotBlank()) { "Transaction ID must not be blank" }
        val txRef = labourTransactionsCollection(uid).document(tx.id)
        return try {
            val batch = db.batch()
            batch.delete(txRef)
            if (tx.linkedCashbookId.isNotBlank()) {
                val cbRef = cashbookCollection(uid).document(tx.linkedCashbookId)
                batch.delete(cbRef)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, txRef.path)
            Result.failure(e)
        }
    }

    suspend fun addCashbookEntry(
        type: CashbookType,
        category: String,
        amount: Long,
        paymentMode: PaymentMode,
        date: String,
        note: String,
    ): Result<String> {
        val uid = requireUserId()
        require(amount > 0L) { "Amount must be positive" }
        val docRef = cashbookCollection(uid).document()
        val payload = mapOf(
            "userId" to uid,
            "type" to type.code,
            "category" to category.trim(),
            "amount" to amount,
            "paymentMode" to paymentMode.code,
            "date" to date,
            "note" to note.trim().take(200),
            "linkedWorkerId" to "",
            "linkedLabourTxId" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCashbookEntry(entry: CashbookEntry): Result<Unit> {
        val uid = requireUserId()
        require(entry.id.isNotBlank()) { "Entry ID must not be blank" }
        val cbRef = cashbookCollection(uid).document(entry.id)
        return try {
            val batch = db.batch()
            batch.delete(cbRef)
            if (entry.linkedLabourTxId.isNotBlank()) {
                val txRef = labourTransactionsCollection(uid).document(entry.linkedLabourTxId)
                batch.delete(txRef)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, cbRef.path)
            Result.failure(e)
        }
    }
}
