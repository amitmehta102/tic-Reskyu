package com.reskyu.merchant.data.model

/**
 * Form state and input holder for the Post Listing screen.
 * This is a UI-layer model — not persisted directly to Firestore.
 */
data class ListingForm(
    val heroItem: String = "",
    val dietaryTag: DietaryTag = DietaryTag.VEG,
    val mealsAvailable: Int = 1,
    val originalPrice: Double = 0.0,
    val discountedPrice: Double = 0.0,
    val imageUri: String = "",              // Local URI before Cloudinary upload
    val imageUrl: String = "",              // Remote URL after Cloudinary upload
    val expiresInMinutes: Int = 60          // How long until this listing expires
)
