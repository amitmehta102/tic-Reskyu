package com.reskyu.merchant.data.model

/**
 * Enum for the lifecycle status of a listing.
 * Matches the "status" field in Firestore /listings/{listingId}.
 */
enum class ListingStatus {
    OPEN,
    CLOSING,    // Low stock warning (e.g., 1 meal left)
    SOLD_OUT,
    CANCELLED,
    EXPIRED
}
