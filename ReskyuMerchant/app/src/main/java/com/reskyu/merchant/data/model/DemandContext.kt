package com.reskyu.merchant.data.model

/**
 * Summarised local-demand snapshot built entirely from Firestore data.
 * NO AI is involved in computing this — it's pure local aggregation.
 *
 * Passed to [GeminiApiService.interpretDemand] for AI interpretation only.
 *
 * Scoring logic (client-side, no AI):
 *   ≥ 5 active listings nearby → demandScore = HIGH
 *   2–4 active listings nearby → demandScore = MEDIUM
 *   0–1 active listings nearby → demandScore = LOW
 */
data class DemandContext(
    /** Open listings within ~2 km posted in the last 2 hours. */
    val totalActiveListings : Int    = 0,

    // ── Dietary split (client-side count) ────────────────────────────────────
    val vegListings         : Int    = 0,
    val nonVegListings      : Int    = 0,

    // ── Price bucket (client-side count) ─────────────────────────────────────
    /** Listings with discountedPrice ≤ ₹99 */
    val budgetListings      : Int    = 0,
    /** Listings with discountedPrice > ₹99 */
    val premiumListings     : Int    = 0,

    // ── Local heuristic score (no AI) ─────────────────────────────────────────
    /** "LOW" | "MEDIUM" | "HIGH" — computed locally from active listing count */
    val demandScore         : String = "LOW",

    // ── Calendar context ──────────────────────────────────────────────────────
    val dayOfWeek           : String = "",   // e.g. "FRIDAY"
    val hourOfDay           : Int    = 0,    // 0–23

    // ── Merchant's own dietary category ───────────────────────────────────────
    /** e.g. "mostly veg", "non-veg" — derived from the merchant's listing history */
    val merchantCategory    : String = ""
)
