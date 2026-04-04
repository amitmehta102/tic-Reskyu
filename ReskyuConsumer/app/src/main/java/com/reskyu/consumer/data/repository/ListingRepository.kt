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

class ListingRepository {

    private val db = FirebaseFirestore.getInstance()
    private val listingsRef = db.collection(Constants.COLLECTION_LISTINGS)

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
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    cellResults[cellKey] = snapshot.documents.mapNotNull { it.toListing() }
                    mergeAndEmit()
                }
            registrations.add(reg)
        }

        awaitClose { registrations.forEach { it.remove() } }
    }

    suspend fun getListingById(listingId: String): Listing? {
        return listingsRef
            .document(listingId)
            .get()
            .await()
            .toListing()
    }

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
