package com.reskyu.merchant.data.model

/**
 * Input context sent to Gemini for Sell Everything Mode planning.
 *
 * Aggregated from:
 *  - Active listings  → mealsLeft, originalPriceAvg
 *  - Merchant profile → minutesUntilClose
 *  - Claims history   → historicalSellOutRate (% of listings that sold out)
 */
data class SellEverythingContext(
    val mealsLeft: Int,
    val minutesUntilClose: Int,
    val originalPriceAvg: Double,           // ₹ average original price across active listings
    val historicalSellOutRate: Float,       // 0.0 – 1.0 (what % of past listings sold out)
    val topHeroItem: String = "",           // Most common hero item in active listings
    val merchantName: String = ""
)
