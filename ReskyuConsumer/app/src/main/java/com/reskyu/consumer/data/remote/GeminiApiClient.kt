package com.reskyu.consumer.data.remote

import com.google.ai.client.generativeai.GenerativeModel
import com.reskyu.consumer.BuildConfig

/**
 * GeminiApiClient
 *
 * Singleton wrapper for the Google Generative AI SDK (Gemini 2.5 Flash).
 * Used on the consumer side to generate short, enthusiastic food-rescue insights
 * shown on the Listing Detail screen.
 *
 * The API key is read from BuildConfig (sourced from local.properties → GEMINI_API_KEY).
 * The key is never hardcoded in source.
 */
object GeminiApiClient {

    // gemini-2.5-flash: best quality for insight text, and user has Gemini Pro quota
    // Cache-first strategy in ListingDetailViewModel keeps actual calls minimal
    private const val MODEL_NAME = "gemini-2.5-flash"

    private val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey    = BuildConfig.GEMINI_API_KEY
        )
    }

    /**
     * Generates a short, enthusiastic 1-sentence insight for a food listing.
     * Shown in a shimmer AI chip on the Listing Detail screen.
     *
     * Fails silently — returns null if the API is unavailable or key is missing.
     *
     * @param heroItem    The name of the food item
     * @param dietaryTag  e.g. "VEG", "VEGAN", "NON_VEG", "JAIN"
     * @param businessName The restaurant / bakery name
     * @return            1-sentence AI insight, or null on error
     */
    suspend fun generateListingInsight(
        heroItem: String,
        dietaryTag: String,
        businessName: String = ""
    ): String? {
        if (BuildConfig.GEMINI_API_KEY.isBlank() ||
            BuildConfig.GEMINI_API_KEY == "YOUR_GEMINI_API_KEY_HERE") return null

        return try {
            val prompt = """
                In exactly one short, enthusiastic sentence, describe why rescuing this food is a great idea.
                Food: "$heroItem" from "$businessName" (dietary: $dietaryTag).
                Be positive, mention the environmental impact, and end with a relevant emoji.
                No hashtags, no quotation marks, just one sentence.
            """.trimIndent()

            val response = model.generateContent(prompt)
            response.text?.trim()?.take(160)
        } catch (_: Exception) {
            null
        }
    }
}
