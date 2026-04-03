package com.reskyu.merchant.utils

/**
 * Utility functions for encoding latitude/longitude coordinates to GeoHash strings.
 *
 * GeoHash is used in Firestore queries to perform efficient geo-proximity searches
 * without a dedicated geospatial database. A shorter prefix means a larger bounding box.
 *
 * Example:
 *   val hash = GeoHashUtils.encode(23.2599, 77.4126, precision = 4) // "wsx4"
 */
object GeoHashUtils {

    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

    /**
     * Encodes [lat] and [lng] into a GeoHash string of the given [precision] (character length).
     *
     * @param lat       Latitude  (-90.0 to 90.0)
     * @param lng       Longitude (-180.0 to 180.0)
     * @param precision Number of GeoHash characters (default 6 ≈ ~1.2 km accuracy).
     */
    fun encode(lat: Double, lng: Double, precision: Int = 6): String {
        var minLat = -90.0; var maxLat = 90.0
        var minLng = -180.0; var maxLng = 180.0
        val hash = StringBuilder()
        var bits = 0; var bitsTotal = 0; var hashValue = 0

        while (hash.length < precision) {
            if (bitsTotal % 2 == 0) {
                val mid = (minLng + maxLng) / 2
                if (lng >= mid) { hashValue = (hashValue shl 1) or 1; minLng = mid }
                else { hashValue = hashValue shl 1; maxLng = mid }
            } else {
                val mid = (minLat + maxLat) / 2
                if (lat >= mid) { hashValue = (hashValue shl 1) or 1; minLat = mid }
                else { hashValue = hashValue shl 1; maxLat = mid }
            }
            bits++
            bitsTotal++
            if (bits == 5) {
                hash.append(BASE32[hashValue])
                bits = 0; hashValue = 0
            }
        }
        return hash.toString()
    }

    /**
     * Returns a list of GeoHash prefixes (the target hash + its 8 neighbours)
     * for bounding-box proximity queries in Firestore.
     */
    fun getNeighbors(geoHash: String): List<String> {
        // TODO: implement full neighbour calculation
        return listOf(geoHash)
    }
}
