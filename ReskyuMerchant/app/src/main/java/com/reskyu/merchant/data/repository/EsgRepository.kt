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
 *  - Recovery rate:   claimed meals / total meals listed per day (estimated from claim volume)
 *
 * Weekly chart: real 7-day bucketing from claim timestamps (Day 0 = 7 days ago).
 * All secondary filters (status) done client-side to avoid composite index requirements.
 */
class EsgRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val claimsCollection = firestore.collection("claims")

    /**
     * Computes aggregate [EsgStats] for the given merchant from their claims.
     *
     * Weekly data is derived from claim [timestamp] (Unix ms) — one bar per day
     * for the last 7 days, starting from midnight 7 days ago (local time).
     */
    suspend fun getEsgStats(merchantId: String): EsgStats {
        // Fetch all claims for this merchant (single-field query, no composite index needed)
        val snapshot = claimsCollection
            .whereEqualTo("merchantId", merchantId)
            .get()
            .await()

        val allDocs       = snapshot.documents
        val completedDocs = allDocs.filter { it.getString("status") == "COMPLETED" }
        val disputedCount = allDocs.count  { it.getString("status") == "DISPUTED" }

        val totalMeals   = completedDocs.size
        val totalRevenue = completedDocs.sumOf { it.getDouble("amount") ?: 0.0 }
        val co2Saved         = totalMeals * 2.5
        val foodWasteReduced = totalMeals * 0.4

        // ── 7-day bucketing window ────────────────────────────────────────────
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -6)   // today is day 6 → 7 days total
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val weekStartMs = cal.timeInMillis
        val dayMs = 24L * 60 * 60 * 1000

        // ── Meals rescued per day ─────────────────────────────────────────────
        val weeklyMeals = FloatArray(7) { 0f }
        completedDocs.forEach { doc ->
            val ts = doc.getLong("timestamp") ?: return@forEach
            if (ts >= weekStartMs) {
                val idx = ((ts - weekStartMs) / dayMs).toInt().coerceIn(0, 6)
                weeklyMeals[idx] += 1f
            }
        }

        // ── Revenue per day ───────────────────────────────────────────────────
        val weeklyRevenue = FloatArray(7) { 0f }
        completedDocs.forEach { doc ->
            val ts = doc.getLong("timestamp") ?: return@forEach
            if (ts >= weekStartMs) {
                val idx = ((ts - weekStartMs) / dayMs).toInt().coerceIn(0, 6)
                weeklyRevenue[idx] += (doc.getDouble("amount") ?: 0.0).toFloat()
            }
        }

        // ── Sales Loss Recovery Rate per day ──────────────────────────────────
        // Recovery rate = (completed claims on that day / total claims on that day) * 100
        // Shows how successfully listings converted into actual rescues each day.
        val totalClaimsByDay = FloatArray(7) { 0f }
        allDocs.forEach { doc ->
            val ts = doc.getLong("timestamp") ?: return@forEach
            if (ts >= weekStartMs) {
                val idx = ((ts - weekStartMs) / dayMs).toInt().coerceIn(0, 6)
                totalClaimsByDay[idx] += 1f
            }
        }
        val recoveryRate = FloatArray(7) { i ->
            val total = totalClaimsByDay[i]
            if (total > 0f) ((weeklyMeals[i] / total) * 100f).coerceIn(0f, 100f) else 0f
        }

        return EsgStats(
            totalMealsRescued  = totalMeals,
            co2SavedKg         = co2Saved,
            foodWasteReducedKg = foodWasteReduced,
            totalRevenue       = totalRevenue,
            weeklyData         = weeklyMeals.toList(),
            weeklyRevenue      = weeklyRevenue.toList(),
            recoveryRateWeekly = recoveryRate.toList(),
            completedOrders    = totalMeals,
            disputedOrders     = disputedCount
        )
    }
}
