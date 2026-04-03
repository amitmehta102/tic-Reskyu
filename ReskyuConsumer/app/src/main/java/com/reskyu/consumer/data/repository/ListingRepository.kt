package com.reskyu.consumer.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.reskyu.consumer.data.model.Listing
import com.reskyu.consumer.utils.Constants
import com.reskyu.consumer.utils.GeoHashUtils
import kotlinx.coroutines.tasks.await

/**
 * ListingRepository
 *
 * Handles all Firestore reads from the `/listings` collection.
 * Listings are queried using GeoHash-based proximity search — a
 * bounding-box approach that filters by geoHash prefix for nearby results.
 *
 * Key Methods:
 *  - getNearbyListings()    : Fetch OPEN listings within a ~radius using GeoHash
 *  - getListingById()       : Fetch a single listing document by ID
 */
class ListingRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val listingsRef = db.collection(Constants.COLLECTION_LISTINGS)

    /**
     * Fetches OPEN listings whose geoHash starts with the given prefix.
     * GeoHashUtils.getNeighbors() returns the bounding-box prefixes to query.
     *
     * @param lat     User's current latitude
     * @param lng     User's current longitude
     * @param radiusKm  Approximate radius in km (controls geoHash precision)
     * @return        List of nearby [Listing] objects, sorted by discountedPrice
     */
    suspend fun getNearbyListings(lat: Double, lng: Double, radiusKm: Double = 5.0): List<Listing> {
        // TODO: Implement GeoHash bounding box query using GeoHashUtils.getBoundingBox()
        // For now, returns all OPEN listings as a placeholder.
        return listingsRef
            .whereEqualTo("status", "OPEN")
            .orderBy("discountedPrice", Query.Direction.ASCENDING)
            .get()
            .await()
            .toObjects(Listing::class.java)
    }

    /**
     * Fetches a single listing by its Firestore document ID.
     *
     * @param listingId  The ID of the listing document
     * @return           [Listing] or null if not found
     */
    suspend fun getListingById(listingId: String): Listing? {
        return listingsRef
            .document(listingId)
            .get()
            .await()
            .toObject(Listing::class.java)
    }
}
