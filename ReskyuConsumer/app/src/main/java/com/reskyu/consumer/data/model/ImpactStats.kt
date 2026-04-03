package com.reskyu.consumer.data.model

/**
 * ImpactStats
 *
 * Embedded map inside the `/users/{uid}` Firestore document.
 * Aggregated and displayed on the consumer's Profile screen.
 *
 * Fields:
 *  - totalMealsRescued : Total number of meals claimed by this user
 *  - co2SavedKg        : Estimated CO₂ saved in kilograms
 *  - moneySaved        : Total INR saved vs. original prices
 */
data class ImpactStats(
    val totalMealsRescued: Int = 0,
    val co2SavedKg: Double = 0.0,
    val moneySaved: Double = 0.0
) {
    /** No-arg constructor required by Firestore deserialization */
    constructor() : this(0, 0.0, 0.0)
}
