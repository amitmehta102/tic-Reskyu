package com.reskyu.consumer.data.model

/**
 * DietaryTag
 *
 * Enum representing the dietary classification of a food listing.
 * Stored as a String in Firestore (e.g., "VEG", "NON_VEG").
 *
 * Usage in Listing: dietaryTag: String = DietaryTag.VEG.name
 */
enum class DietaryTag {
    VEG,
    NON_VEG,
    VEGAN,
    JAIN,       // kept for Firestore backward-compat; hidden from UI chips
    BAKERY,
    SWEETS
}
