package com.reskyu.merchant.data.model

/**
 * Enum for dietary classification shown on a listing.
 * Matches the "dietaryTag" field in Firestore /listings/{listingId}.
 */
enum class DietaryTag {
    VEG,
    NON_VEG,
    VEGAN
}
