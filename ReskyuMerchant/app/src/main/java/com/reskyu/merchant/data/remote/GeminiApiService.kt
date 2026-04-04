package com.reskyu.merchant.data.remote

import com.reskyu.merchant.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Minimal, from-scratch Gemini 2.0 Flash client for SurplusIQ predictions.
 *
 * Key decisions:
 *  - API key is read LAZILY inside the function (not at class init) to guarantee
 *    BuildConfig has been initialised before we build the URL.
 *  - Uses OkHttp directly — no Retrofit, no code-gen, minimal failure surface.
 *  - Returns a raw Int (predicted meals) for simplicity. Reasoning is parsed if present.
 */
object GeminiApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Calls Gemini to predict today's surplus meal count.
     *
     * @param salesHistory  Last 7 days of meal counts (oldest first). May be empty.
     * @return Pair of (predictedMeals: Int, reasoning: String)
     * @throws Exception if network call fails or key is missing.
     */
    suspend fun predict(salesHistory: List<Int>): Pair<Int, String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        check(apiKey.isNotBlank()) { "GEMINI_API_KEY is not set in local.properties" }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/" +
                  "gemini-2.0-flash:generateContent?key=$apiKey"

        val historyStr = if (salesHistory.isEmpty()) "no prior data"
                         else salesHistory.joinToString(", ")

        val prompt = """
You are SurplusIQ, an AI assistant for a food-rescue app.
Past ${salesHistory.size} days of meals sold (oldest→newest): [$historyStr]
Predict the number of surplus meals for TODAY to minimise waste.
Rules:
- Respond ONLY with valid JSON — no markdown, no code blocks.
- Keep reasoning under 12 words.
- If little data, suggest 5–8.
Format: {"meals": <integer>, "reason": "<string>"}
        """.trimIndent()

        val body = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3)
                put("maxOutputTokens", 64)
            })
        }.toString()

        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Gemini ${response.code}: ${response.message}")
            }
            val raw = response.body?.string()
                ?: throw Exception("Empty response from Gemini")

            parseResponse(raw)
        }
    }

    private fun parseResponse(raw: String): Pair<Int, String> {
        return try {
            // Navigate: candidates[0].content.parts[0].text
            val text = JSONObject(raw)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
                // Strip markdown fences if Gemini ignored our instruction
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

            val json   = JSONObject(text)
            val meals  = json.optInt("meals", json.optInt("predictedMeals", 6))
            val reason = json.optString("reason", json.optString("reasoning", "Based on recent trend"))
            Pair(meals, reason)
        } catch (e: Exception) {
            // If JSON parse fails, try extracting any number from the text
            val fallback = Regex("\\d+").find(raw)?.value?.toIntOrNull() ?: 6
            Pair(fallback, "AI prediction")
        }
    }
}
