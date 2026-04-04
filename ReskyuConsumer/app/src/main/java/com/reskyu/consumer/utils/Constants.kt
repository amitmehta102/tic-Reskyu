package com.reskyu.consumer.utils

/**
 * Constants
 *
 * Central registry for hardcoded values used across the app.
 * Keeping these here prevents magic strings scattered in code.
 *
 * Categories:
 *  - Firestore collection names (match exactly what's in your DB)
 *  - SharedPreferences keys
 *  - Navigation argument keys
 */
object Constants {

    // ─── Firestore Collection Names ──────────────────────────────────────────
    // These MUST match the collection names in your Firestore database exactly.

    /** Consumer user profiles: /users/{uid} */
    const val COLLECTION_USERS = "users"

    /** Merchant profiles: /merchants/{uid} — READ ONLY from consumer side */
    const val COLLECTION_MERCHANTS = "merchants"

    /** Food drop listings: /listings/{listingId} */
    const val COLLECTION_LISTINGS = "listings"

    /** Orders/Claims: /claims/{claimId} */
    const val COLLECTION_CLAIMS = "claims"

    // ─── GeoHash ──────────────────────────────────────────────────────────────

    /** GeoHash precision matching the merchant app (4 = ~40km cell, neighbors cover 2km area) */
    const val GEOHASH_PRECISION = 4

    /** Search radius in kilometers for the Alerts screen and map display */
    const val DEFAULT_SEARCH_RADIUS_KM = 2.0

    // ─── Razorpay ─────────────────────────────────────────────────────────────

    /** Razorpay Test Key — replace with live key for production */
    // TODO: Store in local.properties and reference via BuildConfig, NOT hardcoded here
    const val RAZORPAY_KEY_ID = "rzp_test_YOUR_KEY_HERE"

    // ─── SharedPreferences Keys ───────────────────────────────────────────────

    const val PREFS_NAME = "reskyu_prefs"
    const val PREFS_KEY_NOTIFICATIONS = "local_notifications"

    // ─── Navigation Argument Keys ─────────────────────────────────────────────

    const val NAV_ARG_LISTING_ID = "listingId"
    const val NAV_ARG_CLAIM_ID = "claimId"

    // ─── CO₂ Impact Calculation ───────────────────────────────────────────────

    /** Approximate kg of CO₂ saved per meal rescued (source: WRAP report) */
    const val CO2_PER_MEAL_KG = 2.5
}
