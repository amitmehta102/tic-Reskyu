package com.reskyu.merchant.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.reskyu.merchant.data.model.EsgStats
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * Aggregates ESG (Environmental, Social, Governance) impact metrics
 * from Firestore /claims for the merchant Analytics screen.
 *
 * Estimates used:
 *  - CO₂ saved:       ~2.5 kg per meal rescued
 *  - Food waste saved: ~0.4 kg per meal rescued
 *
 * Weekly chart: real 7-day bucketing from claim timestamps (Day 0 = 7 days ago).
 */
class EsgRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val claimsCollection = firestore.collection("claims")

    /**
     * Computes aggregate [EsgStats] for the given merchant from their COMPLETED claims.
     *
     * Weekly data is derived from claim [timestamp] (Unix ms) — one bar per day
     * for the last 7 days, starting from midnight 7 days ago (local time).
     */
    suspend fun getEsgStats(merchantId: String): EsgStats {
        val snapshot = claimsCollection
            .whereEqualTo("merchantId", merchantId)
            .whereEqualTo("status", "COMPLETED")
            .get()
            .await()

        val docs = snapshot.documents
        val totalMeals  = docs.size
        val totalRevenue = docs.sumOf { it.getDouble("amount") ?: 0.0 }
        val co2Saved          = totalMeals * 2.5
        val foodWasteReduced  = totalMeals * 0.4

        // ── 7-day bucketing ───────────────────────────────────────────────────
        // startOfDay7DaysAgo = midnight (00:00:00) of the day 7 days ago (local tz)
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -6)   // today is day 6 → 7 days total
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val weekStartMs = cal.timeInMillis
        val dayMs       = 24L * 60 * 60 * 1000

        val weeklyData = FloatArray(7) { 0f }
        docs.forEach { doc ->
            val ts = doc.getLong("timestamp") ?: return@forEach
            if (ts >= weekStartMs) {
                val dayIndex = ((ts - weekStartMs) / dayMs).toInt().coerceIn(0, 6)
                weeklyData[dayIndex] += 1f
            }
        }

        return EsgStats(
            totalMealsRescued  = totalMeals,
            co2SavedKg         = co2Saved,
            foodWasteReducedKg = foodWasteReduced,
            totalRevenue       = totalRevenue,
            weeklyData         = weeklyData.toList()
        )
    }
}
