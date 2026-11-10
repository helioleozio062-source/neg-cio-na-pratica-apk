package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FirebaseSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createBusiness_authenticatedOwner_createsDocumentSuccessfully(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val bizId = UUID.randomUUID().toString()

        val data = hashMapOf<String, Any>(
            "userId" to aliceUid,
            "name" to "Mercearia Aliança",
            "activityType" to "Mercearia / Retalho",
            "currencySymbol" to "MT",
            "defaultUnit" to "un",
            "createdAt" to FieldValue.serverTimestamp()
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("businesses").document(bizId).set(data).await()
        }

        val snapshot = withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("businesses").document(bizId).get().await()
        }

        assertTrue(snapshot.exists())
        assertEquals("Mercearia Aliança", snapshot.getString("name"))
        assertEquals(aliceUid, snapshot.getString("userId"))
    }

    @Test
    fun getBusinesses_authenticatedOwner_queriesOwnBusinesses(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val bizId = UUID.randomUUID().toString()

        val data = hashMapOf<String, Any>(
            "userId" to aliceUid,
            "name" to "Boutique Elegance",
            "activityType" to "Roupas & Calçado",
            "currencySymbol" to "MT",
            "defaultUnit" to "un",
            "createdAt" to FieldValue.serverTimestamp()
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("businesses").document(bizId).set(data).await()
        }

        val querySnapshot = withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("businesses")
                .whereEqualTo("userId", aliceUid)
                .get()
                .await()
        }

        assertTrue(querySnapshot.documents.any { it.id == bizId })
    }

    @Test
    fun getBusiness_crossUserAccess_failsWithPermissionDenied(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val bizId = UUID.randomUUID().toString()

        val data = hashMapOf<String, Any>(
            "userId" to aliceUid,
            "name" to "Restaurante Sabor",
            "activityType" to "Restaurante & Bar",
            "currencySymbol" to "MT",
            "defaultUnit" to "un",
            "createdAt" to FieldValue.serverTimestamp()
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("businesses").document(bizId).set(data).await()
        }

        // Bob signs in
        signInTestUser(BOB_EMAIL)

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("businesses").document(bizId).get().await()
            }
            fail("Expected PERMISSION_DENIED on cross-user read")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun getBusinesses_unauthenticatedUser_failsWithPermissionDenied(): Unit = runBlocking {
        // Sign out to simulate unauthenticated caller
        auth.signOut()

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("businesses").get().await()
            }
            fail("Expected PERMISSION_DENIED on unauthenticated query")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
    }
}
