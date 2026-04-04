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
            // orderBy removed — compound filter + orderBy requires a composite index.
            // Sorting newest-first is done client-side below.
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val sorted = (snapshot?.toObjects(MerchantClaim::class.java) ?: emptyList())
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
            // orderBy removed — compound filter + orderBy requires a composite index.
            .get().await()
        return snapshot.toObjects(MerchantClaim::class.java)
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

    /**
     * Fetches the consumer [User] profile from /users/{uid}.
     * Returns null if the document does not exist.
     * Used when the merchant needs context (e.g. name) on a disputed claim.
     */
    suspend fun getUserForClaim(userId: String): User? {
        val snapshot = usersCollection.document(userId).get().await()
        return snapshot.toObject(User::class.java)
    }
}
