package com.reskyu.merchant.data.model

/**
 * UI state model for the Dashboard screen.
 * Aggregated on demand from Firestore /listings and /claims.
 */
data class DashboardStats(
    val totalMealsPosted: Int = 0,
    val totalMealsRescued: Int = 0,
    val totalRevenue: Double = 0.0,
    val activeListings: Int = 0,
    val pendingClaims: Int = 0
)
