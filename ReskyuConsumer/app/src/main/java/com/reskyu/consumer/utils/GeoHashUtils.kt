package com.reskyu.consumer.utils

import kotlin.math.*

object GeoHashUtils {

    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"
    const val DEFAULT_PRECISION = 5

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

    private fun neighbor(hash: String, direction: Pair<Int, Int>): String {
        val (_, lat, lng, errs) = decodeWithBounds(hash)
        val neighborLat = lat + direction.first  * errs.first  * 2.0
        val neighborLng = lng + direction.second * errs.second * 2.0
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

    fun getBoundingBoxQueries(
        lat: Double,
        lng: Double,
        precision: Int = DEFAULT_PRECISION
    ): List<Pair<String, String>> {
        return getNeighbors(lat, lng, precision).map { cell ->
            Pair(cell, cell + "~")    // '~' is last valid ASCII in GeoHash range
        }
    }

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
