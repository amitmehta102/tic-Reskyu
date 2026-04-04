package com.reskyu.merchant.data.model

/**
 * Firestore: /users/{uid}
 *
 * Consumer profile — read by the merchant app when viewing dispute context.
 * Written exclusively by the consumer app.
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    // Aggregate impact stats — computed when the consumer views their profile
    val impactStats: Map<String, Any> = emptyMap()
) {
    // Convenience accessors for impactStats map
    val totalMealsRescued: Int get() = (impactStats["totalMealsRescued"] as? Long)?.toInt() ?: 0
    val co2SavedKg: Double    get() = (impactStats["co2SavedKg"] as? Double) ?: 0.0
    val moneySaved: Double    get() = (impactStats["moneySaved"] as? Double) ?: 0.0
}
