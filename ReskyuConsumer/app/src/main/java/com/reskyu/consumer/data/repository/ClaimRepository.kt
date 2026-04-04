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

    fun observeClaimsForUser(userId: String): Flow<List<Claim>> = callbackFlow {
        val reg = claimsRef
            .whereEqualTo("userId", userId)
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

    suspend fun submitRating(claimId: String, merchantId: String, stars: Int) {
        val claimRef = claimsRef.document(claimId)

        val existing = claimRef.get().await().getLong("rating")?.toInt() ?: 0
        if (existing > 0) return   // already rated, do nothing

        claimRef.update("rating", stars).await()

        try {
            db.collection("merchants").document(merchantId).update(
                "ratingSum",   FieldValue.increment(stars.toLong()),
                "ratingCount", FieldValue.increment(1L)
            ).await()
        } catch (_: Exception) {
        }
    }

    suspend fun getClaimById(claimId: String): Claim? {
        return claimsRef.document(claimId).get().await().toObject(Claim::class.java)
    }
}
