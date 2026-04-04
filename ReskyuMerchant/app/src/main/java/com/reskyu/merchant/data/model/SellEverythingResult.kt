package com.reskyu.merchant.data.model

/**
 * AI-generated sell-out strategy returned by Gemini 2.0 Flash.
 *
 * Used by [SellEverythingRepository] to:
 *  1. Apply [discountPercentage] to all active listings
 *  2. Auto-create a MYSTERY_BOX bundle listing priced at [bundlePrice]
 *  3. Store [urgencyMessage] and [pushRequired] on the merchant doc
 */
data class SellEverythingResult(
    val discountPercentage: Int       = 30,          // e.g. 40 → 40% off original price
    val bundlePrice: Double           = 0.0,         // ₹ price for the auto-created bundle
    val bundleMeals: Int              = 2,           // how many meals per bundle unit
    val bundleStrategy: String        = "",          // human-readable strategy explanation
    val urgencyMessage: String        = "",          // shown on consumer side via push
    val pushRequired: Boolean         = true
)
