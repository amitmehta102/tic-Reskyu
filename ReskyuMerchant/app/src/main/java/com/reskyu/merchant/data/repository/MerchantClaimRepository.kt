package com.reskyu.merchant.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.reskyu.merchant.data.model.MerchantClaim
import com.reskyu.merchant.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Manages merchant-side claim operations in Firestore /claims.
 *
 * Use [observeClaimsForMerchant] for real-time order management UI.
 * Use [getClaimsForMerchant] for one-shot fetches (ESG aggregation, etc.).
 */
class MerchantClaimRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val claimsCollection = firestore.collection("claims")
    private val usersCollection  = firestore.collection("users")

    // ── Read — snapshot listener (real-time) ──────────────────────────────────

    /**
     * Emits the merchant's claims in real-time, ordered newest first.
     * The Flow stays open until the caller's coroutine is cancelled.
     */
    fun observeClaimsForMerchant(merchantId: String): Flow<List<MerchantClaim>> = callbackFlow {
        val registration = claimsCollection
            .whereEqualTo("merchantId", merchantId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val sorted = (snapshot?.documents?.mapNotNull { mapClaim(it) } ?: emptyList())
                    .sortedByDescending { it.timestamp }
                trySend(sorted)
            }
        awaitClose { registration.remove() }
    }

    // ── Read — one-shot ───────────────────────────────────────────────────────

    /** One-shot fetch — ordered by timestamp descending. */
    suspend fun getClaimsForMerchant(merchantId: String): List<MerchantClaim> {
        val snapshot = claimsCollection
            .whereEqualTo("merchantId", merchantId)
            .get().await()
        return snapshot.documents.mapNotNull { mapClaim(it) }
            .sortedByDescending { it.timestamp }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /** Marks a claim as COMPLETED — consumer has picked up their order. */
    suspend fun completeClaim(claimId: String) {
        claimsCollection.document(claimId).update("status", "COMPLETED").await()
    }

    /** Raises a dispute on a claim. */
    suspend fun disputeClaim(claimId: String) {
        claimsCollection.document(claimId).update("status", "DISPUTED").await()
    }

    // ── Cross-collection lookup ───────────────────────────────────────────────

    /** Fetches the consumer User profile from /users/{uid}. */
    suspend fun getUserForClaim(userId: String): User? {
        val snapshot = usersCollection.document(userId).get().await()
        return snapshot.toObject(User::class.java)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Safely maps a Firestore document to [MerchantClaim].
     *
     * Handles timestamp stored as either:
     *  - com.google.firebase.Timestamp (consumer app / auto-Firestore format)
     *  - Long (Unix ms — written by our seeder or older versions)
     */
    private fun mapClaim(doc: com.google.firebase.firestore.DocumentSnapshot): MerchantClaim? {
        return try {
            val rawTs = doc.get("timestamp")
            val timestampMs: Long = when (rawTs) {
                is com.google.firebase.Timestamp -> rawTs.toDate().time
                is Long                          -> rawTs
                is Number                        -> rawTs.toLong()
                else                             -> 0L
            }
            MerchantClaim(
                id           = doc.id,
                userId       = doc.getString("userId")       ?: doc.getString("consumerId") ?: "",
                merchantId   = doc.getString("merchantId")   ?: "",
                listingId    = doc.getString("listingId")    ?: "",
                businessName = doc.getString("businessName") ?: "",
                heroItem     = doc.getString("heroItem")     ?: "",
                paymentId    = doc.getString("paymentId")    ?: "",
                amount       = doc.getDouble("amount")       ?: 0.0,
                timestamp    = timestampMs,
                status       = doc.getString("status")       ?: "PENDING_PICKUP"
            )
        } catch (e: Exception) {
            null   // skip malformed documents silently
        }
    }
}
