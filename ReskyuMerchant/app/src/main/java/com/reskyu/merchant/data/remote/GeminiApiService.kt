package com.reskyu.merchant.data.remote

import com.reskyu.merchant.data.model.SurplusIqResult

/**
 * Service that calls the Gemini Flash API with the merchant's past sales history
 * to generate a surplus meal prediction for the day.
 *
 * POST body: { "salesHistory": [...], "merchantId": "..." }
 * Response  : { "predictedMeals": 5, "reasoning": "...", "confidence": 0.87 }
 *
 * TODO: Inject an OkHttpClient / Retrofit instance via dependency injection.
 */
class GeminiApiService {

    /**
     * Sends [salesHistory] to Gemini Flash and returns a [SurplusIqResult].
     *
     * @param merchantId   The merchant's UID (for context).
     * @param salesHistory List of past daily meal-sold counts, ordered oldest-first.
     * @return [SurplusIqResult] with AI prediction or throws on network/parse error.
     */
    suspend fun predictSurplus(
        merchantId: String,
        salesHistory: List<Int>
    ): SurplusIqResult {
        // TODO: Implement Gemini Flash HTTP call
        // 1. Build JSON body with merchantId and salesHistory
        // 2. POST to Gemini Flash endpoint
        // 3. Parse response JSON → SurplusIqResult
        throw NotImplementedError("GeminiApiService.predictSurplus is not yet implemented")
    }
}
