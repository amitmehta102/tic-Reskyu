package com.reskyu.merchant.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.reskyu.merchant.data.model.Listing
import com.reskyu.merchant.data.model.ListingForm
import com.reskyu.merchant.data.model.ListingStatus
import kotlinx.coroutines.tasks.await

/**
 * Manages CRUD operations for listings in Firestore /listings.
 *
 * ⚠️ [mealsLeft] decrements MUST be done via atomic transactions — never plain writes.
 */
class ListingRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val listingsCollection = firestore.collection("listings")

    /**
     * Creates a new listing document from [form].
     * Returns the generated listing ID.
     */
    suspend fun postListing(form: ListingForm, merchantId: String, businessName: String, geoHash: String): String {
        val docRef = listingsCollection.document()
        val expiresAt = System.currentTimeMillis() + (form.expiresInMinutes * 60 * 1000L)

        val listing = Listing(
            id = docRef.id,
            merchantId = merchantId,
            businessName = businessName,
            heroItem = form.heroItem,
            dietaryTag = form.dietaryTag.name,
            mealsLeft = form.mealsAvailable,
            originalPrice = form.originalPrice,
            discountedPrice = form.discountedPrice,
            imageUrl = form.imageUrl,
            geoHash = geoHash,
            expiresAt = expiresAt,
            status = ListingStatus.OPEN.name
        )
        docRef.set(listing).await()
        return docRef.id
    }

    /**
     * Updates editable fields of an existing listing.
     */
    suspend fun editListing(listingId: String, updates: Map<String, Any>) {
        listingsCollection.document(listingId).update(updates).await()
    }

    /**
     * Cancels a listing by setting its status to CANCELLED.
     */
    suspend fun cancelListing(listingId: String) {
        listingsCollection.document(listingId)
            .update("status", ListingStatus.CANCELLED.name)
            .await()
    }

    /**
     * Fetches all active (OPEN / CLOSING) listings for a given merchant.
     */
    suspend fun getActiveListings(merchantId: String): List<Listing> {
        val snapshot = listingsCollection
            .whereEqualTo("merchantId", merchantId)
            .whereIn("status", listOf(ListingStatus.OPEN.name, ListingStatus.CLOSING.name))
            .get()
            .await()
        return snapshot.toObjects(Listing::class.java)
    }

    /**
     * Atomically decrements [mealsLeft] when a consumer claims a meal.
     * Sets status to SOLD_OUT if mealsLeft reaches 0.
     */
    suspend fun atomicDecrementMeals(listingId: String) {
        firestore.runTransaction { transaction ->
            val ref = listingsCollection.document(listingId)
            val snapshot = transaction.get(ref)
            val mealsLeft = snapshot.getLong("mealsLeft")?.toInt() ?: 0

            if (mealsLeft <= 0) throw Exception("No meals left to claim.")

            val newMeals = mealsLeft - 1
            val newStatus = if (newMeals == 0) ListingStatus.SOLD_OUT.name else ListingStatus.OPEN.name

            transaction.update(ref, mapOf("mealsLeft" to newMeals, "status" to newStatus))
        }.await()
    }
}
