package com.reskyu.merchant.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.reskyu.merchant.data.model.MerchantClaim
import kotlinx.coroutines.tasks.await

/**
 * Manages merchant-side claim operations in Firestore /claims.
 * The merchant can view, complete, or dispute consumer claims.
 */
class MerchantClaimRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val claimsCollection = firestore.collection("claims")

    /**
     * Fetches all claims for the given merchant, ordered by timestamp descending.
     */
    suspend fun getClaimsForMerchant(merchantId: String): List<MerchantClaim> {
        val snapshot = claimsCollection
            .whereEqualTo("merchantId", merchantId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.toObjects(MerchantClaim::class.java)
    }

    /**
     * Marks a claim as COMPLETED (consumer has picked up their order).
     */
    suspend fun completeClaim(claimId: String) {
        claimsCollection.document(claimId).update("status", "COMPLETED").await()
    }

    /**
     * Raises a dispute on a claim, setting status to DISPUTED.
     */
    suspend fun disputeClaim(claimId: String) {
        claimsCollection.document(claimId).update("status", "DISPUTED").await()
    }
}
