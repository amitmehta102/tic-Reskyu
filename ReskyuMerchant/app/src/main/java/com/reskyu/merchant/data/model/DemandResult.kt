package com.reskyu.merchant.data.model

/**
 * AI-interpreted demand result from Gemini 2.0 Flash.
 *
 * Gemini receives a [DemandContext] (pre-aggregated locally) and returns
 * a short, actionable interpretation:
 *
 *   demand_level        → "LOW" | "MEDIUM" | "HIGH"
 *   best_action         → 1-sentence merchant recommendation
 *   best_listing_window → "next X minutes" string
 *   reason              → short explanation (≤ 15 words)
 *
 * All fields default to reasonable fallbacks so missing/partial JSON
 * never causes a crash.
 */
data class DemandResult(
    val demandLevel       : String = "MEDIUM",
    val bestAction        : String = "Consider listing your surplus food now",
    val bestListingWindow : String = "next 30 minutes",
    val reason            : String = "Moderate activity in your area"
)
