package com.reskyu.consumer.data.remote

import com.google.ai.client.generativeai.GenerativeModel
import com.reskyu.consumer.BuildConfig

object GeminiApiClient {

    private const val MODEL_NAME = "gemini-2.5-flash"

    private val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey    = BuildConfig.GEMINI_API_KEY
        )
    }

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
