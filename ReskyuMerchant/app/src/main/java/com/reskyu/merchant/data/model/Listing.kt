package com.reskyu.merchant.data.model

/**
 * Firestore: /listings/{listingId}
 *
 * Represents a food-drop listing posted by a merchant.
 * Note: [mealsLeft] must ONLY be modified via Firestore atomic transactions.
 */
data class Listing(
    val id: String = "",
    val merchantId: String = "",
    val businessName: String = "",          // Denormalized for fast consumer display
    val heroItem: String = "",
    val dietaryTag: String = DietaryTag.VEG.name,
    val mealsLeft: Int = 0,                 // ⚠️ Use atomic transaction to update
    val originalPrice: Double = 0.0,
    val discountedPrice: Double = 0.0,
    val imageUrl: String = "",
    val geoHash: String = "",               // Copied from merchant for map/list queries
    val expiresAt: Long = 0L,               // Unix timestamp in milliseconds
    val status: String = ListingStatus.OPEN.name
)
