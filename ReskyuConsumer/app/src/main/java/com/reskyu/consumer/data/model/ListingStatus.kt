package com.reskyu.consumer.data.model

/**
 * ListingStatus
 *
 * Represents the lifecycle state of a food listing.
 * Stored as a String in Firestore under `listing.status`.
 *
 * States:
 *  - OPEN      : Listing is active and has meals available
 *  - SOLD_OUT  : mealsLeft reached 0 via atomic transactions
 *  - CANCELLED : Merchant cancelled the listing before expiry
 */
enum class ListingStatus {
    OPEN,
    SOLD_OUT,
    CANCELLED
}
