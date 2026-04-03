package com.reskyu.consumer.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import com.reskyu.consumer.data.model.Claim
import com.reskyu.consumer.utils.Constants
import kotlinx.coroutines.tasks.await

/**
 * ClaimRepository
 *
 * Handles the creation and retrieval of claim documents in `/claims`.
 *
 * ⚠️ CRITICAL: [createClaim] MUST use a Firestore atomic transaction to:
 *   1. Read the listing document and verify mealsLeft > 0
 *   2. Decrement mealsLeft by 1
 *   3. Write the new claim document
 *   All three steps succeed or fail together — this prevents overselling.
 *
 * Key Methods:
 *  - createClaim()        : Atomic transaction — claim a meal
 *  - getClaimsForUser()   : Fetch all claims for a given user (My Orders)
 */
class ClaimRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val claimsRef = db.collection(Constants.COLLECTION_CLAIMS)
    private val listingsRef = db.collection(Constants.COLLECTION_LISTINGS)

    /**
     * Creates a claim atomically.
     * Decrements listing.mealsLeft and writes the claim in one transaction.
     *
     * @param claim  The [Claim] object to write (id field will be auto-generated)
     * @return       The auto-generated claim document ID on success
     * @throws       Exception if mealsLeft == 0 or transaction fails
     */
    suspend fun createClaim(claim: Claim): String {
        val newClaimRef = claimsRef.document() // Pre-generate ID

        db.runTransaction { transaction: Transaction ->
            val listingSnap = transaction.get(listingsRef.document(claim.listingId))
            val mealsLeft = listingSnap.getLong("mealsLeft")?.toInt()
                ?: throw Exception("Listing not found")

            if (mealsLeft <= 0) {
                throw Exception("Sorry, this listing is sold out!")
            }

            // Decrement mealsLeft
            transaction.update(listingsRef.document(claim.listingId), "mealsLeft", FieldValue.increment(-1))

            // If this was the last meal, mark listing as SOLD_OUT
            if (mealsLeft == 1) {
                transaction.update(listingsRef.document(claim.listingId), "status", "SOLD_OUT")
            }

            // Write the claim document
            transaction.set(newClaimRef, claim.copy(id = newClaimRef.id))

        }.await()

        return newClaimRef.id
    }

    /**
     * Fetches all claims for a specific user, ordered by most recent first.
     * Used to populate the My Orders screen.
     *
     * @param userId  The Firebase Auth UID of the consumer
     * @return        List of [Claim] documents
     */
    suspend fun getClaimsForUser(userId: String): List<Claim> {
        return claimsRef
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(Claim::class.java)
    }
}
