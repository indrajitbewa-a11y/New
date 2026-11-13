const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_shop_owner";
const BOB_UID = "bob_shop_owner";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validWorkerPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    name: "Ramesh Kumar",
    phone: "9876543210",
    role: "Mistri",
    dailyWage: 600,
    joiningDate: "2026-11-01",
    isActive: true,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user cannot read or write workers, attendance, transactions, or cashbook", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("workers").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("attendance").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("labour_transactions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("cashbook").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("workers").doc("w1").set(validWorkerPayload(ALICE_UID))
  );
});

test("Authenticated user can create and read their own worker", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("workers").doc("w1");
  await assertSucceeds(ref.set(validWorkerPayload(ALICE_UID)));
  await assertSucceeds(ref.get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("workers").get());
});

test("Cross-user isolation: Bob cannot read or write Alice's workers or cashbook", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workers").doc("w1").set(validWorkerPayload(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workers").doc("w1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workers").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("workers").doc("w2").set(validWorkerPayload(BOB_UID))
  );
});

test("Dirty Dozen: Reject shadow fields, negative wage, and invalid attendance status", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const workerRef = aliceDb.collection("users").doc(ALICE_UID).collection("workers").doc("w1");

  // Shadow field
  await assertFails(
    workerRef.set({ ...validWorkerPayload(ALICE_UID), isAdmin: true })
  );

  // Zero or negative wage
  await assertFails(
    workerRef.set({ ...validWorkerPayload(ALICE_UID), dailyWage: 0 })
  );

  // Create valid worker first
  await assertSucceeds(workerRef.set(validWorkerPayload(ALICE_UID)));

  // Invalid attendance status
  const now = new Date();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("w1_2026-11-12").set({
      userId: ALICE_UID,
      workerId: "w1",
      date: "2026-11-12",
      status: "INVALID_STATUS",
      dailyWageSnapshot: 600,
      overtimeAmount: 0,
      earnedAmount: 600,
      note: "",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Attendance requires existing worker (existsAfter) and accepts valid attendance", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  // Orphaned attendance fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("missing_2026-11-12").set({
      userId: ALICE_UID,
      workerId: "missing",
      date: "2026-11-12",
      status: "PRESENT",
      dailyWageSnapshot: 600,
      overtimeAmount: 0,
      earnedAmount: 600,
      note: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workers").doc("w1").set(validWorkerPayload(ALICE_UID))
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("w1_2026-11-12").set({
      userId: ALICE_UID,
      workerId: "w1",
      date: "2026-11-12",
      status: "OVERTIME",
      dailyWageSnapshot: 600,
      overtimeAmount: 200,
      earnedAmount: 800,
      note: "Extra 2 hours",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Labour payout requires atomic linked cashbook entry via batch", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workers").doc("w1").set(validWorkerPayload(ALICE_UID))
  );

  // Standalone labour_transaction without cashbook entry fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("labour_transactions").doc("tx1").set({
      userId: ALICE_UID,
      workerId: "w1",
      workerName: "Ramesh Kumar",
      type: "ADVANCE",
      amount: 300,
      date: "2026-11-12",
      paymentMode: "CASH",
      note: "Weekly advance",
      linkedCashbookId: "cb_tx1",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Atomic batch with linked cashbook entry succeeds
  const batch = aliceDb.batch();
  batch.set(
    aliceDb.collection("users").doc(ALICE_UID).collection("labour_transactions").doc("tx1"),
    {
      userId: ALICE_UID,
      workerId: "w1",
      workerName: "Ramesh Kumar",
      type: "ADVANCE",
      amount: 300,
      date: "2026-11-12",
      paymentMode: "CASH",
      note: "Weekly advance",
      linkedCashbookId: "cb_tx1",
      createdAt: now,
      updatedAt: now,
    }
  );
  batch.set(
    aliceDb.collection("users").doc(ALICE_UID).collection("cashbook").doc("cb_tx1"),
    {
      userId: ALICE_UID,
      type: "EXPENSE",
      category: "Labour Payout",
      amount: 300,
      paymentMode: "CASH",
      date: "2026-11-12",
      note: "Advance - Ramesh Kumar",
      linkedWorkerId: "w1",
      linkedLabourTxId: "tx1",
      createdAt: now,
      updatedAt: now,
    }
  );
  await assertSucceeds(batch.commit());
});
