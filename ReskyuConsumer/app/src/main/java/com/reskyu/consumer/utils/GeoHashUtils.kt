package com.reskyu.consumer.utils

import kotlin.math.*

/**
 * GeoHashUtils
 *
 * Utilities for GeoHash-based proximity search against Firestore.
 *
 * Firestore does not support native geospatial queries, so we use
 * GeoHash string prefix queries as a bounding-box approximation:
 *
 *   1. Encode the user's lat/lng into a GeoHash string.
 *   2. Compute the 8 neighboring GeoHash cells (bounding box).
 *   3. For each neighbor, run a Firestore range query:
 *      .whereGreaterThanOrEqualTo("geoHash", neighbor)
 *      .whereLessThan("geoHash", neighborEnd)
 *
 * This matches the `geoHash` field stored in both `/merchants` and `/listings`.
 *
 * Reference: https://firebase.google.com/docs/firestore/solutions/geoqueries
 */
object GeoHashUtils {

    // GeoHash base32 character set
    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"
    private const val DEFAULT_PRECISION = 5 // ~5km precision

    /**
     * Encodes a latitude/longitude pair into a GeoHash string.
     *
     * @param lat       Latitude (-90 to 90)
     * @param lng       Longitude (-180 to 180)
     * @param precision Number of characters (higher = more precise)
     * @return          GeoHash string (e.g., "wsx4t")
     */
    fun encode(lat: Double, lng: Double, precision: Int = DEFAULT_PRECISION): String {
        var minLat = -90.0; var maxLat = 90.0
        var minLng = -180.0; var maxLng = 180.0
        val hash = StringBuilder()
        var isEven = true
        var bit = 0
        var ch = 0

        while (hash.length < precision) {
            val mid: Double
            if (isEven) {
                mid = (minLng + maxLng) / 2
                if (lng > mid) { ch = ch or (1 shl (4 - bit)); minLng = mid } else maxLng = mid
            } else {
                mid = (minLat + maxLat) / 2
                if (lat > mid) { ch = ch or (1 shl (4 - bit)); minLat = mid } else maxLat = mid
            }
            isEven = !isEven
            if (bit < 4) { bit++ } else { hash.append(BASE32[ch]); bit = 0; ch = 0 }
        }
        return hash.toString()
    }

    /**
     * Returns a list of GeoHash bounding-box pairs for querying nearby listings.
     * Each pair is [lowerBound, upperBound] for a Firestore range query.
     *
     * @param lat         User's latitude
     * @param lng         User's longitude
     * @param precision   GeoHash precision level
     * @return            List of [lowerBound, upperBound] string pairs
     */
    fun getBoundingBoxQueries(
        lat: Double,
        lng: Double,
        precision: Int = DEFAULT_PRECISION
    ): List<Pair<String, String>> {
        val center = encode(lat, lng, precision)
        // TODO: Implement neighbor calculation for a full bounding-box query.
        // For now, returns a single prefix query on the center hash.
        // Upgrade: use the geofire-android-common library for production.
        val lower = center
        val upper = center + "~" // '~' is the last ASCII char in the valid GeoHash range
        return listOf(Pair(lower, upper))
    }

    /**
     * Calculates the approximate distance in kilometers between two lat/lng points
     * using the Haversine formula. Used to filter/sort results after GeoHash query.
     *
     * @return Distance in kilometers
     */
    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)
        return 2 * R * asin(sqrt(a))
    }
}
