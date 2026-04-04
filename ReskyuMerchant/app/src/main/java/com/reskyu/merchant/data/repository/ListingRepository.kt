package com.reskyu.merchant.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.reskyu.merchant.data.model.Listing
import com.reskyu.merchant.data.model.ListingForm
import com.reskyu.merchant.data.model.ListingStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Manages CRUD operations for listings in Firestore /listings.
 *
 * ⚠️ [mealsLeft] decrements MUST be done via atomic Firestore transactions — never plain writes.
 *
 * Use [observeActiveListings] for real-time UI updates (snapshot listener).
 * Use [getActiveListings] for one-shot fetch (e.g. background aggregation).
 */
class ListingRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val listingsCollection = firestore.collection("listings")

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Creates a new listing document from [form].
     * @return Generated Firestore document ID.
     */
    suspend fun postListing(
        form:         ListingForm,
        merchantId:   String,
        businessName: String,
        geoHash:      String,
        lat:          Double = 0.0,
        lng:          Double = 0.0
    ): String {
        val docRef    = listingsCollection.document()
        val expiresAt = System.currentTimeMillis() + (form.expiresInMinutes * 60 * 1000L)

        val listing = Listing(
            id              = docRef.id,
            merchantId      = merchantId,
            businessName    = businessName,
            heroItem        = form.heroItem,
            dietaryTag      = form.dietaryTag.name,
            mealsLeft       = form.mealsAvailable,
            originalPrice   = form.originalPrice,
            discountedPrice = form.discountedPrice,
            imageUrl        = form.imageUrl,
            geoHash         = geoHash,
            lat             = lat,
            lng             = lng,
            expiresAt       = expiresAt,
            status          = ListingStatus.OPEN.name
        )
        docRef.set(listing).await()
        return docRef.id
    }

    // ── Read — snapshot listener (real-time) ──────────────────────────────────

    /**
     * Emits the merchant's active listings (OPEN / CLOSING) in real-time.
     * The Flow stays open until the caller's coroutine is cancelled.
     */
    fun observeActiveListings(merchantId: String): Flow<List<Listing>> = callbackFlow {
        val activeStatuses = setOf(ListingStatus.OPEN.name, ListingStatus.CLOSING.name)
        val registration = listingsCollection
            .whereEqualTo("merchantId", merchantId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val sorted = (snapshot?.documents?.mapNotNull { mapListing(it) } ?: emptyList())
                    .filter  { it.status in activeStatuses }
                    .sortedBy { it.expiresAt }
                trySend(sorted)
            }
        awaitClose { registration.remove() }
    }

    // ── Read — one-shot ───────────────────────────────────────────────────────

    /** One-shot fetch — use for background aggregation or tests. */
    suspend fun getActiveListings(merchantId: String): List<Listing> {
        val activeStatuses = setOf(ListingStatus.OPEN.name, ListingStatus.CLOSING.name)
        val snapshot = listingsCollection
            .whereEqualTo("merchantId", merchantId)
            .get().await()
        return snapshot.documents.mapNotNull { mapListing(it) }
            .filter { it.status in activeStatuses }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /** Updates editable fields on an existing listing. */
    suspend fun editListing(listingId: String, updates: Map<String, Any>) {
        listingsCollection.document(listingId).update(updates).await()
    }

    /** Permanently deletes the listing document from Firestore. */
    suspend fun cancelListing(listingId: String) {
        listingsCollection.document(listingId)
            .delete()
            .await()
    }

    // ── Atomic transaction ────────────────────────────────────────────────────

    /**
     * Atomically decrements [mealsLeft] by 1.
     * Sets status to SOLD_OUT if mealsLeft reaches 0.
     * Throws if no meals remain.
     */
    suspend fun atomicDecrementMeals(listingId: String) {
        firestore.runTransaction { transaction ->
            val ref      = listingsCollection.document(listingId)
            val snapshot = transaction.get(ref)
            val mealsLeft = snapshot.getLong("mealsLeft")?.toInt() ?: 0

            if (mealsLeft <= 0) throw Exception("No meals left to claim.")

            val newMeals  = mealsLeft - 1
            val newStatus = if (newMeals == 0) ListingStatus.SOLD_OUT.name else ListingStatus.OPEN.name
            transaction.update(ref, mapOf("mealsLeft" to newMeals, "status" to newStatus))
        }.await()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Safely maps a Firestore document to [Listing].
     * Handles [expiresAt] stored as Firestore Timestamp or Long (Unix ms).
     * Unknown extra fields from consumer-side writes are silently ignored.
     */
    private fun mapListing(doc: com.google.firebase.firestore.DocumentSnapshot): Listing? {
        return try {
            val rawTs = doc.get("expiresAt")
            val expiresAtMs: Long = when (rawTs) {
                is com.google.firebase.Timestamp -> rawTs.toDate().time
                is Long   -> rawTs
                is Number -> rawTs.toLong()
                else      -> 0L
            }
            Listing(
                id              = doc.id,
                merchantId      = doc.getString("merchantId")      ?: "",
                businessName    = doc.getString("businessName")    ?: "",
                heroItem        = doc.getString("heroItem")        ?: "",
                dietaryTag      = doc.getString("dietaryTag")      ?: com.reskyu.merchant.data.model.DietaryTag.VEG.name,
                mealsLeft       = doc.getLong("mealsLeft")?.toInt() ?: 0,
                originalPrice   = doc.getDouble("originalPrice")   ?: 0.0,
                discountedPrice = doc.getDouble("discountedPrice") ?: 0.0,
                imageUrl        = doc.getString("imageUrl")        ?: "",
                geoHash         = doc.getString("geoHash")         ?: "",
                lat             = doc.getDouble("lat")             ?: 0.0,
                lng             = doc.getDouble("lng")             ?: 0.0,
                expiresAt       = expiresAtMs,
                status          = doc.getString("status")          ?: ListingStatus.OPEN.name
            )
        } catch (e: Exception) {
            null
        }
    }
}
