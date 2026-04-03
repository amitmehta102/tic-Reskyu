package com.reskyu.merchant.data.model

/**
 * Firestore: /merchants/{uid}/templates/{templateId}
 *
 * Stores the merchant's saved default info for quick listing creation.
 */
data class ListingTemplate(
    val templateId: String = "",
    val heroItem: String = "",          // e.g. "Assorted Pastries"
    val basePrice: Double = 0.0,
    val pricingFloor: Double = 0.0,
    val pricingCeiling: Double = 0.0
)
