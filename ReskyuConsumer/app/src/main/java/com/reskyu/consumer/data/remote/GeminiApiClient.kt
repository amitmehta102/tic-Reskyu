package com.reskyu.consumer.data.remote

/**
 * GeminiApiClient
 *
 * Client wrapper for interacting with the Gemini 2.5 Flash API.
 * On the CONSUMER side, Gemini is used for features like:
 *  - Generating personalised meal suggestions based on dietary preferences
 *  - Answering questions about a listing's ingredients/allergens
 *
 * On the merchant side (separate app), Gemini handles pricing predictions.
 *
 * TODO: Add the `generativeai` dependency to build.gradle.kts and
 *       replace API_KEY with a value read from BuildConfig or local.properties.
 *
 * Dependency (add to build.gradle.kts):
 *   implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
 */
object GeminiApiClient {

    // TODO: Replace with your actual Gemini API key (store in local.properties, NOT source control)
    private const val API_KEY = "YOUR_GEMINI_API_KEY"

    private const val MODEL_NAME = "gemini-2.5-flash"

    // TODO: Uncomment after adding the generativeai dependency
    // val model: GenerativeModel by lazy {
    //     GenerativeModel(
    //         modelName = MODEL_NAME,
    //         apiKey = API_KEY
    //     )
    // }

    /**
     * Example usage — generate a short description for a listing.
     *
     * @param heroItem    The name of the food item
     * @param dietaryTag  Dietary classification (VEG, VEGAN, etc.)
     * @return            AI-generated description string
     */
    suspend fun generateListingInsight(heroItem: String, dietaryTag: String): String {
        // TODO: Implement using model.generateContent(...)
        return "AI insight for $heroItem ($dietaryTag) — coming soon."
    }
}
