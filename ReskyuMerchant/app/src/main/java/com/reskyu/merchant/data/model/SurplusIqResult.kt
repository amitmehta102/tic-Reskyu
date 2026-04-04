package com.reskyu.merchant.data.model

/**
 * Result returned by [SurplusIqRepository] after calling the Gemini Flash API.
 *
 * Cached in Firestore /merchants/{uid}:
 *   lastPredictionDate, lastPredictionMeals, lastPredictionReason,
 *   lastPredictionBestTime, lastPredictionPricingHint,
 *   lastPredictionActionTip, lastPredictionConfidence
 *
 * All new fields have blank defaults so existing cached results are backward-compatible.
 */
data class SurplusIqResult(
    val predictedMeals  : Int    = 0,
    val reasoning       : String = "",   // Short AI reason (<15 words)
    val confidence      : Float  = 0f,   // 0.0 – 1.0 from Gemini
    val cachedDate      : String = "",   // "YYYY-MM-DD"
    // ── New rich outputs ──────────────────────────────────────────────────────
    val bestTimeToList  : String = "",   // e.g. "6–7 PM"
    val pricingHint     : String = "",   // e.g. "Try ₹79 — sweet spot for Fridays"
    val actionTip       : String = ""    // e.g. "Baked goods sell 2× faster on weekends"
)
