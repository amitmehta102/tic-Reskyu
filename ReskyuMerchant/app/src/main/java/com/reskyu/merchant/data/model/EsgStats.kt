package com.reskyu.merchant.data.model

/**
 * ESG (Environmental, Social, Governance) impact metrics for the Analytics screen.
 * Aggregated from Firestore /claims for the authenticated merchant.
 */
data class EsgStats(
    val totalMealsRescued: Int = 0,
    val co2SavedKg: Double = 0.0,           // Estimated: ~2.5 kg CO₂ per meal saved
    val foodWasteReducedKg: Double = 0.0,
    val totalRevenue: Double = 0.0,
    val weeklyData: List<Float> = emptyList() // For the MPAndroidChart bar chart
)
