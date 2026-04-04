package com.reskyu.consumer.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import com.reskyu.consumer.data.model.Claim
import com.reskyu.consumer.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * ClaimRepository
 *
 * Handles creation and retrieval of claim documents in `/claims`.
 *
 * ⚠️ NOTE on indexing:
 * whereEqualTo("userId") + orderBy("timestamp") requires a composite index.
 * To avoid that requirement, we query by userId only and sort client-side.
 */
class ClaimRepository {

    private val db           = FirebaseFirestore.getInstance()
    private val claimsRef    = db.collection(Constants.COLLECTION_CLAIMS)
    private val listingsRef  = db.collection(Constants.COLLECTION_LISTINGS)

    suspend fun createClaim(claim: Claim): String {
        val newClaimRef = claimsRef.document()

        db.runTransaction { transaction: Transaction ->
            val listingSnap = transaction.get(listingsRef.document(claim.listingId))
            val mealsLeft   = listingSnap.getLong("mealsLeft")?.toInt()
                ?: throw Exception("Listing not found")

            val qty = claim.quantity.coerceAtLeast(1)
            if (mealsLeft <= 0) throw Exception("Sorry, this listing is sold out!")
            if (mealsLeft < qty) throw Exception("Only $mealsLeft portion${if (mealsLeft == 1) "" else "s"} left — reduce your quantity.")

            transaction.update(listingsRef.document(claim.listingId), "mealsLeft", FieldValue.increment(-qty.toLong()))

            if (mealsLeft <= qty) {
                transaction.update(listingsRef.document(claim.listingId), "status", "SOLD_OUT")
            }

            transaction.set(newClaimRef, claim.copy(id = newClaimRef.id))
        }.await()

        return newClaimRef.id
    }

    /**
     * Real-time Flow of all claims for a user.
     *
     * Sorts client-side (timestamp DESC) to avoid requiring a composite index
     * on userId + timestamp — which would cause a silent Firestore error if
     * the index hasn't been created in the Firebase Console.
     */
    fun observeClaimsForUser(userId: String): Flow<List<Claim>> = callbackFlow {
        val reg = claimsRef
            .whereEqualTo("userId", userId)
            // NO orderBy — sort client-side to avoid composite index requirement
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val claims = snapshot.toObjects(Claim::class.java)
                    .sortedByDescending { it.timestamp?.seconds ?: 0L }
                trySend(claims)
            }
        awaitClose { reg.remove() }
    }

    suspend fun getClaimsForUser(userId: String): List<Claim> {
        return claimsRef
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .toObjects(Claim::class.java)
            .sortedByDescending { it.timestamp?.seconds ?: 0L }
    }

    /**
     * Saves a star rating (1–5) for a completed order.
     *
     * Two independent writes (NOT a transaction):
     *  1. /claims/{claimId}       → rating: stars  (always succeeds if authenticated)
     *  2. /merchants/{merchantId} → ratingSum/ratingCount increments (best-effort)
     *
     * Separating them ensures the visible rating is ALWAYS saved on the claim,
     * even if Firestore security rules block the consumer from writing to /merchants.
     */
    suspend fun submitRating(claimId: String, merchantId: String, stars: Int) {
        val claimRef = claimsRef.document(claimId)

        // Guard: prevent double-rating (read current value first)
        val existing = claimRef.get().await().getLong("rating")?.toInt() ?: 0
        if (existing > 0) return   // already rated, do nothing

        // ── 1. Save rating on the claim document ──────────────────────────────
        claimRef.update("rating", stars).await()

        // ── 2. Update merchant aggregate (best-effort) ────────────────────────
        // This may fail if Firestore security rules don't allow consumer writes to /merchants.
        // That's OK — the claim rating is already saved above.
        // Fix: add the rule below to firestore.rules:
        //   match /merchants/{uid} {
        //     allow update: if request.auth != null
        //       && request.resource.data.diff(resource.data).affectedKeys()
        //            .hasOnly(['ratingSum', 'ratingCount']);
        //   }
        try {
            db.collection("merchants").document(merchantId).update(
                "ratingSum",   FieldValue.increment(stars.toLong()),
                "ratingCount", FieldValue.increment(1L)
            ).await()
        } catch (_: Exception) {
            // Silently ignore — claim rating is already persisted
        }
    }

    suspend fun getClaimById(claimId: String): Claim? {
        return claimsRef.document(claimId).get().await().toObject(Claim::class.java)
    }
}
