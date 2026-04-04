package com.reskyu.merchant.data.model

/**
 * Enum for dietary classification shown on a listing.
 * Matches the "dietaryTag" field in Firestore /listings/{listingId}.
 *
 * CONTAINS_MILK is used on Mystery Boxes that include dairy products.
 */
enum class DietaryTag(val label: String, val emoji: String) {
    VEG          ("Veg",           "🥕"),
    NON_VEG      ("Non-Veg",       "🍗"),
    VEGAN        ("Vegan",         "🌿"),
    CONTAINS_MILK("Contains Milk", "🥛")
}
