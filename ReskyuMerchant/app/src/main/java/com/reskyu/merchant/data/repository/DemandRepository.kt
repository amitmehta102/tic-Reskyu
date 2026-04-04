package com.reskyu.merchant.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.reskyu.merchant.data.model.DemandContext
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import kotlin.math.*

/**
 * Fetches nearby active listings from Firestore and computes a local demand
 * score — NO AI is used here. This is pure Kotlin aggregation.
 *
 * Data source: Firestore /listings
 *   - status == "OPEN"
 *   - expiresAt >= (now - 2 hours)   ← filters to recent activity
 *   - Client-side haversine filter:  ≤ 2 km from merchant location
 *
 * Demand scoring (all local, no AI):
 *   ≥ 5 active nearby listings → HIGH
 *   2–4 active nearby listings → MEDIUM
 *   0–1 active nearby listings → LOW
 */
object DemandRepository {

    private val db          = FirebaseFirestore.getInstance()
    private val listingsCol = db.collection("listings")

    /**
     * Builds a [DemandContext] from live Firestore data.
     *
     * Safe to call from coroutines — all Firestore ops are suspend.
     * Returns a zero-field default if any fetch errors occur (no-crash guarantee).
     *
     * @param merchantId  Used to exclude the merchant's own listings from the count.
     * @param merchantLat Merchant latitude  — centre of 2 km radius.
     * @param merchantLng Merchant longitude — centre of 2 km radius.
     */
    suspend fun fetchDemandContext(
        merchantId  : String,
        merchantLat : Double,
        merchantLng : Double
    ): DemandContext {
        val now         = System.currentTimeMillis()
        val twoHoursAgo = now - 2 * 60 * 60 * 1000L

        // ── 1. Fetch open listings active in the last 2 hours ─────────────────
        val listings = runCatching {
            listingsCol
                .whereEqualTo("status", "OPEN")
                .whereGreaterThanOrEqualTo("expiresAt", twoHoursAgo)
                .get()
                .await()
                .documents
        }.getOrElse { emptyList() }

        // ── 2. Filter out own listings + apply 2 km distance filter ───────────
        val nearby = listings.filter { doc ->
            val ownListing = doc.getString("merchantId") == merchantId
            if (ownListing) return@filter false

            // Only apply geo-filter when we have a valid merchant location
            if (merchantLat == 0.0 && merchantLng == 0.0) return@filter true

            val lat = doc.getDouble("lat") ?: return@filter false
            val lng = doc.getDouble("lng") ?: return@filter false
            haversineKm(merchantLat, merchantLng, lat, lng) <= 2.0
        }

        // ── 3. Group by dietary tag (client-side, no AI) ──────────────────────
        val vegCount    = nearby.count { doc ->
            val tag = doc.getString("dietaryTag") ?: ""
            tag.contains("VEG", ignoreCase = true) && !tag.contains("NON", ignoreCase = true)
        }
        val nonVegCount = nearby.count { doc ->
            val tag = doc.getString("dietaryTag") ?: ""
            tag.contains("NON_VEG", ignoreCase = true)
        }

        // ── 4. Group by price bucket (client-side, no AI) ────────────────────
        val budgetCount  = nearby.count { (it.getDouble("discountedPrice") ?: 0.0) <= 99.0 }
        val premiumCount = nearby.count { (it.getDouble("discountedPrice") ?: 0.0) > 99.0  }

        // ── 5. Compute heuristic demand score (no AI) ─────────────────────────
        val demandScore = when {
            nearby.size >= 5 -> "HIGH"
            nearby.size >= 2 -> "MEDIUM"
            else             -> "LOW"
        }

        // ── 6. Calendar context ───────────────────────────────────────────────
        val cal       = Calendar.getInstance()
        val dayOfWeek = java.time.DayOfWeek
            .of(if (cal.get(Calendar.DAY_OF_WEEK) == 1) 7 else cal.get(Calendar.DAY_OF_WEEK) - 1)
            .name
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        return DemandContext(
            totalActiveListings = nearby.size,
            vegListings         = vegCount,
            nonVegListings      = nonVegCount,
            budgetListings      = budgetCount,
            premiumListings     = premiumCount,
            demandScore         = demandScore,
            dayOfWeek           = dayOfWeek,
            hourOfDay           = hour
        )
    }

    // ── Haversine distance formula ─────────────────────────────────────────────

    /**
     * Returns the great-circle distance in kilometres between two lat/lng points.
     * Accurate to ~0.5% for distances up to ~500 km.
     */
    private fun haversineKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r    = 6371.0          // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a    = sin(dLat / 2).pow(2) +
                   cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                   sin(dLon / 2).pow(2)
        return r * 2 * asin(sqrt(a))
    }
}
