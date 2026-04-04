package com.reskyu.consumer.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.reskyu.consumer.data.model.DietaryTag
import com.reskyu.consumer.data.model.Listing
import com.reskyu.consumer.data.model.ListingStatus
import com.reskyu.consumer.utils.Constants
import com.reskyu.consumer.utils.GeoHashUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * ListingRepository
 *
 * Handles all Firestore reads from the `/listings` collection.
 *
 * Key methods:
 *  [observeNearbyListings]  Real-time Flow — snapshot listener across 9 GeoHash cells
 *  [getListingById]         One-shot read — used by ClaimScreen and DetailScreen
 *
 * Uses a safe [DocumentSnapshot.toListing()] extension instead of toObject() /
 * toObjects() to handle mixed field types from the merchant app (e.g. expiresAt
 * stored as a Long millisecond value instead of a Firestore Timestamp).
 */
class ListingRepository {

    private val db = FirebaseFirestore.getInstance()
    private val listingsRef = db.collection(Constants.COLLECTION_LISTINGS)

    /**
     * Real-time Flow of OPEN listings near the user's location.
     *
     * Strategy:
     *  1. Compute 9 neighbor GeoHash cells at precision=5 (~5km radius).
     *  2. For each cell, attach a Firestore snapshotListener for the range query.
     *  3. On any snapshot update, merge all results, deduplicate by ID,
     *     post-filter by exact Haversine distance, sort by discountedPrice ASC.
     *  4. Emit the merged list downstream.
     */
    fun observeNearbyListings(
        lat: Double,
        lng: Double,
        radiusKm: Double = Constants.DEFAULT_SEARCH_RADIUS_KM
    ): Flow<List<Listing>> = callbackFlow {

        val bbox = GeoHashUtils.getBoundingBoxQueries(lat, lng)
        val cellResults = mutableMapOf<String, List<Listing>>()
        val registrations = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

        fun mergeAndEmit() {
            val merged = cellResults.values
                .flatten()
                .distinctBy { it.id }
                .filter { listing ->
                    listing.status == "OPEN" &&
                    listing.mealsLeft > 0 &&   // hide "0 left" listings — can't be claimed anyway
                    // Exact 2km filter when listing has real coordinates (lat/lng from merchant)
                    // Falls back to GeoHash-only match for older listings without coordinates
                    (listing.lat == 0.0 && listing.lng == 0.0 ||
                     GeoHashUtils.distanceKm(lat, lng, listing.lat, listing.lng) <= radiusKm)
                }
                .sortedBy { it.discountedPrice }
            trySend(merged)
        }

        bbox.forEach { (lower, upper) ->
            val cellKey = lower
            val reg = listingsRef
                .whereGreaterThanOrEqualTo("geoHash", lower)
                .whereLessThan("geoHash", upper)
                // status filtered client-side — avoids composite index requirement
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    cellResults[cellKey] = snapshot.documents.mapNotNull { it.toListing() }
                    mergeAndEmit()
                }
            registrations.add(reg)
        }

        awaitClose { registrations.forEach { it.remove() } }
    }

    /**
     * One-shot fetch of a single listing by document ID.
     */
    suspend fun getListingById(listingId: String): Listing? {
        return listingsRef
            .document(listingId)
            .get()
            .await()
            .toListing()
    }

    /**
     * Fallback: all OPEN listings without geo filter.
     */
    suspend fun getAllOpenListings(): List<Listing> {
        return listingsRef
            .whereEqualTo("status", "OPEN")
            .orderBy("discountedPrice", Query.Direction.ASCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { it.toListing() }
    }
}

/**
 * Safe DocumentSnapshot → Listing mapper.
 *
 * The merchant app stores expiresAt as either:
 *  - A Firestore Timestamp (correct)
 *  - A Long (Unix milliseconds) — tolerated here
 *
 * Using this instead of toObject() prevents silent deserialization failures
 * that would cause the entire listing to be dropped from the UI.
 */
private fun DocumentSnapshot.toListing(): Listing? {
    return try {
        val expiresAt: Timestamp = when (val raw = get("expiresAt")) {
            is Timestamp -> raw
            is Long      -> Timestamp(raw / 1000, ((raw % 1000) * 1_000_000).toInt())
            is Number    -> Timestamp(raw.toLong() / 1000, 0)
            else         -> Timestamp.now()
        }

        Listing(
            id             = getString("id")             ?: id,  // fallback to doc ID
            merchantId     = getString("merchantId")     ?: "",
            businessName   = getString("businessName")   ?: "",
            heroItem       = getString("heroItem")       ?: "",
            dietaryTag     = getString("dietaryTag")     ?: DietaryTag.VEG.name,
            mealsLeft      = getLong("mealsLeft")?.toInt() ?: 0,
            originalPrice  = getDouble("originalPrice")  ?: 0.0,
            discountedPrice= getDouble("discountedPrice")?: 0.0,
            imageUrl       = getString("imageUrl")       ?: "",
            geoHash        = getString("geoHash")        ?: "",
            lat            = getDouble("lat")            ?: 0.0,
            lng            = getDouble("lng")            ?: 0.0,
            expiresAt      = expiresAt,
            status         = getString("status")         ?: ListingStatus.OPEN.name,

            // ── Mystery Box fields — MUST be listed here; this mapper bypasses toObject() ──
            listingType    = getString("listingType")    ?: "STANDARD",
            boxType        = getString("boxType")        ?: "",
            priceRangeMin  = getDouble("priceRangeMin")  ?: getLong("priceRangeMin")?.toDouble() ?: 0.0,
            priceRangeMax  = getDouble("priceRangeMax")  ?: getLong("priceRangeMax")?.toDouble() ?: 0.0,
            itemCount      = getLong("itemCount")?.toInt() ?: 0
        )
    } catch (e: Exception) {
        null   // skip malformed documents rather than crashing
    }
}

