package com.reskyu.merchant.data.model

/**
 * Firestore: /merchants/{uid}
 *
 * Represents a merchant business profile. The document ID equals the Firebase Auth UID.
 */
data class Merchant(
    val uid: String = "",
    val businessName: String = "",
    val closingTime: String = "",           // e.g. "22:00"
    val trustScore: Int = 100,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val geoHash: String = "",
    val lastPredictionDate: String = "",    // AI Cache: "2026-04-03"
    val lastPredictionMeals: Int = 0        // AI Cache: predicted meal count
)
