package com.reskyu.consumer.utils

import kotlin.math.*

/**
 * GeoHashUtils
 *
 * Utilities for GeoHash-based proximity search against Firestore.
 * Firestore has no native geospatial query, so we use GeoHash prefix range
 * queries as a bounding-box approximation.
 *
 * Algorithm:
 *  1. Encode user's lat/lng → GeoHash string at chosen precision.
 *  2. Compute the 8 neighboring GeoHash cells (N, NE, E, SE, S, SW, W, NW + center).
 *  3. For each of the 9 cells, run a Firestore range query:
 *       .whereGreaterThanOrEqualTo("geoHash", cell)
 *       .whereLessThan("geoHash", cell + "~")
 *  4. Merge results and post-filter by exact Haversine distance.
 *
 * GeoHash precision guide:
 *   precision=4 → ~40km × 20km  (city-level)
 *   precision=5 → ~5km × 5km    (neighbourhood — DEFAULT)
 *   precision=6 → ~1km × 0.6km  (block-level)
 */
object GeoHashUtils {

    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"
    const val DEFAULT_PRECISION = 5

    // ── Encoding ──────────────────────────────────────────────────────────────

    /**
     * Encodes a lat/lng into a GeoHash string.
     */
    fun encode(lat: Double, lng: Double, precision: Int = DEFAULT_PRECISION): String {
        var minLat = -90.0;  var maxLat = 90.0
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
     * Decodes a GeoHash into the center lat/lng of the cell,
     * plus the error margin (±lat, ±lng).
     *
     * @return  Triple(lat, lng, Pair(latErr, lngErr))
     */
    fun decode(hash: String): Triple<Double, Double, Pair<Double, Double>> {
        var minLat = -90.0;  var maxLat = 90.0
        var minLng = -180.0; var maxLng = 180.0
        var isEven = true

        for (c in hash) {
            val cd = BASE32.indexOf(c)
            for (mask in intArrayOf(16, 8, 4, 2, 1)) {
                val bit = cd and mask != 0
                if (isEven) {
                    val mid = (minLng + maxLng) / 2
                    if (bit) minLng = mid else maxLng = mid
                } else {
                    val mid = (minLat + maxLat) / 2
                    if (bit) minLat = mid else maxLat = mid
                }
                isEven = !isEven
            }
        }
        val lat    = (minLat + maxLat) / 2
        val lng    = (minLng + maxLng) / 2
        val latErr = (maxLat - minLat) / 2
        val lngErr = (maxLng - minLng) / 2
        return Triple(lat, lng, Pair(latErr, lngErr))
    }

    // ── Neighbor Calculation ──────────────────────────────────────────────────

    /**
     * Computes the GeoHash string for a single neighboring cell.
     * Direction-based neighbor is computed via re-encoding the adjacent cell center.
     */
    private fun neighbor(hash: String, direction: Pair<Int, Int>): String {
        val (_, lat, lng, errs) = decodeWithBounds(hash)
        val neighborLat = lat + direction.first  * errs.first  * 2.0
        val neighborLng = lng + direction.second * errs.second * 2.0
        // Clamp to valid earth bounds
        val clampedLat = neighborLat.coerceIn(-90.0, 90.0)
        val clampedLng = when {
            neighborLng > 180.0  -> neighborLng - 360.0
            neighborLng < -180.0 -> neighborLng + 360.0
            else -> neighborLng
        }
        return encode(clampedLat, clampedLng, hash.length)
    }

    private data class DecodedHash(
        val hash: String, val lat: Double, val lng: Double, val errs: Pair<Double, Double>
    )
    private fun decodeWithBounds(hash: String): DecodedHash {
        val (lat, lng, errs) = decode(hash)
        return DecodedHash(hash, lat, lng, errs)
    }

    /**
     * Returns all 9 neighbor GeoHash strings (center + 8 cardinal/diagonal directions)
     * for a bounding-box query.
     */
    fun getNeighbors(lat: Double, lng: Double, precision: Int = DEFAULT_PRECISION): List<String> {
        val center = encode(lat, lng, precision)
        val directions = listOf(
            Pair(0,  0),   // center
            Pair(1,  0),   // N
            Pair(1,  1),   // NE
            Pair(0,  1),   // E
            Pair(-1, 1),   // SE
            Pair(-1, 0),   // S
            Pair(-1,-1),   // SW
            Pair(0, -1),   // W
            Pair(1, -1)    // NW
        )
        return directions
            .map { dir -> if (dir == Pair(0,0)) center else neighbor(center, dir) }
            .distinct()
    }

    /**
     * Returns list of (lowerBound, upperBound) GeoHash string pairs for Firestore
     * range queries across all 9 neighboring cells.
     *
     * Usage in Firestore:
     *   .whereGreaterThanOrEqualTo("geoHash", lower)
     *   .whereLessThan("geoHash", upper)
     */
    fun getBoundingBoxQueries(
        lat: Double,
        lng: Double,
        precision: Int = DEFAULT_PRECISION
    ): List<Pair<String, String>> {
        return getNeighbors(lat, lng, precision).map { cell ->
            Pair(cell, cell + "~")    // '~' is last valid ASCII in GeoHash range
        }
    }

    // ── Distance ──────────────────────────────────────────────────────────────

    /**
     * Haversine distance in kilometres between two lat/lng points.
     */
    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)
        return 2 * R * asin(sqrt(a))
    }
}
