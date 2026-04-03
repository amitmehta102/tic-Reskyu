package com.reskyu.merchant.data.model

/**
 * Firestore: /claims/{claimId}
 *
 * Represents a consumer's order / claim against a listing.
 * businessName and heroItem are denormalized for fast Order UI rendering.
 */
data class MerchantClaim(
    val id: String = "",
    val userId: String = "",
    val merchantId: String = "",
    val listingId: String = "",
    val businessName: String = "",          // Denormalized
    val heroItem: String = "",              // Denormalized
    val paymentId: String = "",             // Razorpay payment ID
    val amount: Double = 0.0,
    val timestamp: Long = 0L,              // Unix timestamp in milliseconds
    val status: String = "PENDING_PICKUP"  // PENDING_PICKUP | COMPLETED | DISPUTED
)
