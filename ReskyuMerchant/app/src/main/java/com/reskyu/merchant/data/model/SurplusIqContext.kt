package com.reskyu.merchant.data.model

/**
 * Rich context object passed to Gemini for a maximally informed SurplusIQ prediction.
 *
 * Every field is derived from existing Firestore data — no new data collection required.
 * Default values ensure safe usage even when some data is missing.
 */
data class SurplusIqContext(
    // ── Sales history ──────────────────────────────────────────────────────────
    val salesLast7Days    : List<Int>    = emptyList(),  // daily completed claim counts, oldest→newest
    val salesLast30Days   : List<Int>    = emptyList(),  // 30-day window, oldest→newest
    val revenueLast7Days  : List<Double> = emptyList(),  // daily revenue ₹, oldest→newest

    // ── Item intelligence ──────────────────────────────────────────────────────
    val topItems          : List<String> = emptyList(),  // top 3 heroItem names by volume
    val vegPercent        : Int          = 0,            // 0–100
    val nonVegPercent     : Int          = 0,            // 0–100
    val mysteryBoxPercent : Int          = 0,            // 0–100 of total listings that are mystery boxes

    // ── Performance signals ────────────────────────────────────────────────────
    val avgSelloutMinutes : Int          = 0,            // 0 = never sold out; >0 = avg mins to sell out
    val cancellationRate  : Int          = 0,            // 0–100: (disputed/total) %

    // ── Merchant context ───────────────────────────────────────────────────────
    val closingTime       : String       = "",           // e.g. "22:00"
    val dayOfWeek         : String       = "",           // e.g. "FRIDAY"
    val monthName         : String       = ""            // e.g. "April"
)
