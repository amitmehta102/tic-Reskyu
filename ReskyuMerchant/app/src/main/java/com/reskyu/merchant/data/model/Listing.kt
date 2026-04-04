package com.reskyu.merchant.data.model

/**
 * Firestore: /listings/{listingId}
 *
 * Represents a food-drop listing posted by a merchant.
 * Can be a REGULAR listing or a MYSTERY_BOX.
 *
 * Note: [mealsLeft] must ONLY be modified via Firestore atomic transactions.
 *
 * Mystery Box fields (priceRangeMin, priceRangeMax, itemCount, boxType)
 * all default to safe values so existing regular listings are unaffected.
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
    val geoHash: String = "",               // Copied from merchant for geo queries
    val lat: Double = 0.0,                  // Merchant latitude  — for map pins & distance
    val lng: Double = 0.0,                  // Merchant longitude — for map pins & distance
    val expiresAt: Long = 0L,               // Unix timestamp in milliseconds
    val status: String = ListingStatus.OPEN.name,
    // ── Mystery Box fields ───────────────────────────────────────────────────
    val listingType: String = ListingType.REGULAR.name,   // "REGULAR" or "MYSTERY_BOX"
    val boxType: String = "",                              // MysteryBoxType name
    val priceRangeMin: Double = 0.0,                      // Min ₹ value of box contents
    val priceRangeMax: Double = 0.0,                      // Max ₹ value of box contents
    val itemCount: Int = 0                                 // Total items in mystery box
)

