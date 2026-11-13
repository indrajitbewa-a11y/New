package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AttendanceStatus
import com.example.data.model.CashbookType
import com.example.data.model.HisabCalculator
import com.example.data.model.LabourTxType
import com.example.data.model.PaymentMode
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HisabRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addWorker_andMarkAttendance_andAddLabourTransaction_syncsToCashbookAndCalculatesBalance() = runBlocking {
        val uid = signInTestUser(OWNER_A_EMAIL)
        val repo = HisabRepository(firestore, auth)

        val workerId = withTimeout(TIMEOUT_MS) {
            repo.addWorker(
                name = "Raju Mistri",
                phone = "9876543210",
                role = "Mistri",
                dailyWage = 600L,
                joiningDate = "2026-11-01"
            ).getOrThrow()
        }
        assertTrue(workerId.isNotBlank())

        val workers = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeWorkers(uid).first { list -> list.any { it.id == workerId } }
        }
        val worker = workers.first { it.id == workerId }

        withTimeout(TIMEOUT_MS) {
            repo.markAttendance(
                worker = worker,
                date = "2026-11-10",
                status = AttendanceStatus.PRESENT
            ).getOrThrow()
            repo.markAttendance(
                worker = worker,
                date = "2026-11-11",
                status = AttendanceStatus.HALF_DAY
            ).getOrThrow()
            repo.markAttendance(
                worker = worker,
                date = "2026-11-12",
                status = AttendanceStatus.OVERTIME,
                overtimeAmount = 200L,
                note = "Late loading"
            ).getOrThrow()
        }

        // Total earned: 600 + 300 + (600 + 200) = 1700
        val attendanceList = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeAttendance(uid).first { list -> list.count { it.workerId == workerId } == 3 }
        }

        val txId = withTimeout(TIMEOUT_MS) {
            repo.addLabourTransaction(
                worker = worker,
                type = LabourTxType.ADVANCE,
                amount = 500L,
                date = "2026-11-12",
                paymentMode = PaymentMode.CASH,
                note = "Weekly advance"
            ).getOrThrow()
        }
        assertTrue(txId.isNotBlank())

        val txList = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeLabourTransactions(uid).first { list -> list.any { it.id == txId } }
        }

        val cashbookList = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCashbook(uid).first { list -> list.any { it.linkedLabourTxId == txId } }
        }
        val syncedExpense = cashbookList.first { it.linkedLabourTxId == txId }
        assertEquals(CashbookType.EXPENSE.code, syncedExpense.type)
        assertEquals(500L, syncedExpense.amount)
        assertEquals("Labour Payout", syncedExpense.category)

        val summary = HisabCalculator.computeWorkerSummary(worker, attendanceList, txList)
        assertEquals(1700L, summary.totalEarned)
        assertEquals(500L, summary.totalPaid)
        assertEquals(1200L, summary.remainingBalance)
    }

    @Test
    fun observeWorkers_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(OWNER_A_EMAIL)
        val aliceRepo = HisabRepository(firestore, auth)
        val workerId = withTimeout(TIMEOUT_MS) {
            aliceRepo.addWorker("Mohan", "9999999999", "Helper", 400L, "2026-11-01").getOrThrow()
        }
        assertTrue(workerId.isNotBlank())

        signInTestUser(OWNER_B_EMAIL)
        val bobRepo = HisabRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeWorkers(aliceUid).first()
            }
        } catch (e: Exception) {
            caught = e
        }
        assertNotNull(caught)
        val firestoreEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeWorkers_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = HisabRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeWorkers("some_uid").first()
            }
        } catch (e: Exception) {
            caught = e
        }
        assertNotNull(caught)
        val firestoreEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    private companion object {
        const val OWNER_A_EMAIL = "owner_a@hisabtest.com"
        const val OWNER_B_EMAIL = "owner_b@hisabtest.com"
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
