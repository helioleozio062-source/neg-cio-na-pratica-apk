const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot read businesses", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("businesses").get());
});

test("Authenticated user: can create their own business", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("businesses").doc("biz_1").set({
      userId: ALICE_UID,
      name: "Mercearia Central",
      activityType: "Mercearia / Retalho",
      currencySymbol: "MT",
      defaultUnit: "un",
      phone: "+244 923 000 000",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot read another user's business", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("businesses").doc("biz_alice").set({
      userId: ALICE_UID,
      name: "Alice Shop",
      activityType: "Retalho",
      currencySymbol: "MT",
      defaultUnit: "un",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("businesses").doc("biz_alice").get());
});

test("Authenticated user: can query their own businesses with userId filter", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("businesses").doc("biz_alice").set({
      userId: ALICE_UID,
      name: "Alice Shop",
      activityType: "Retalho",
      currencySymbol: "MT",
      defaultUnit: "un",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("businesses").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: cannot spoof userId on product creation", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("products").doc("prod_1").set({
      userId: ALICE_UID, // Spoofed!
      businessId: "biz_1",
      name: "Arroz 1kg",
      purchasePrice: 1000,
      sellingPrice: 1500,
      currentStock: 20,
      unit: "kg",
      createdAt: new Date(),
    })
  );
});
