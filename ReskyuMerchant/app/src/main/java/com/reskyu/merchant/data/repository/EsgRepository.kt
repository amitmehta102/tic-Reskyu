package com.reskyu.merchant.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.reskyu.merchant.data.model.EsgStats
import kotlinx.coroutines.tasks.await

/**
 * Aggregates ESG (Environmental, Social, Governance) impact metrics
 * from Firestore /claims for the Analytics screen.
 *
 * CO₂ estimate: ~2.5 kg CO₂ saved per meal rescued.
 * Food waste estimate: ~0.4 kg food waste avoided per meal rescued.
 */
class EsgRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val claimsCollection = firestore.collection("claims")

    /**
     * Computes aggregate [EsgStats] for the given merchant from their completed claims.
     */
    suspend fun getEsgStats(merchantId: String): EsgStats {
        val snapshot = claimsCollection
            .whereEqualTo("merchantId", merchantId)
            .whereEqualTo("status", "COMPLETED")
            .get()
            .await()

        val completedClaims = snapshot.documents

        val totalMeals = completedClaims.size
        val totalRevenue = completedClaims.sumOf { it.getDouble("amount") ?: 0.0 }
        val co2Saved = totalMeals * 2.5
        val foodWasteReduced = totalMeals * 0.4

        // TODO: Build weekly chart data from timestamps for MPAndroidChart
        val weeklyData = List(7) { 0f } // Placeholder for 7-day bar data

        return EsgStats(
            totalMealsRescued = totalMeals,
            co2SavedKg = co2Saved,
            foodWasteReducedKg = foodWasteReduced,
            totalRevenue = totalRevenue,
            weeklyData = weeklyData
        )
    }
}
