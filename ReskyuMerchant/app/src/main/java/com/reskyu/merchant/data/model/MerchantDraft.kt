package com.reskyu.merchant.data.model

/**
 * Temporary model that accumulates onboarding step data before
 * being persisted to Firestore as a [Merchant] document.
 */
data class MerchantDraft(
    val uid: String = "",
    val businessName: String = "",
    val closingTime: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val geoHash: String = ""
)
