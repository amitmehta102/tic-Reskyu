package com.reskyu.merchant.data.remote

import com.reskyu.merchant.data.model.SurplusIqResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Calls the Google Gemini 2.0 Flash API to predict surplus meal counts.
 *
 * ▼▼▼ Add your API key below ▼▼▼
 * Get one free at: https://aistudio.google.com/app/apikey
 *
 * TODO: Once the SDK 36.1 Java-compile issue is resolved, migrate the key
 *       to BuildConfig via local.properties + buildConfigField.
 */
class GeminiApiService {

    companion object {
        // ▼▼▼ Paste your Gemini API key here ▼▼▼
        private const val GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE"
        // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/" +
                "gemini-2.0-flash:generateContent?key=$GEMINI_API_KEY"

    /**
     * Asks Gemini to predict surplus meals for today.
     *
     * @param merchantId   Merchant UID (included in prompt for context).
     * @param salesHistory Past daily meal counts ordered oldest→newest.
     * @return [SurplusIqResult] with predictedMeals, reasoning, and confidence.
     * @throws IOException on HTTP error or parse failure.
     */
    suspend fun predictSurplus(
        merchantId: String,
        salesHistory: List<Int>
    ): SurplusIqResult {
        val request = Request.Builder()
            .url(endpoint)
            .post(buildRequestBody(merchantId, salesHistory).toRequestBody("application/json".toMediaType()))
            .build()

        val rawJson = withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Gemini API returned ${response.code}: ${response.message}")
                }
                response.body?.string()
                    ?: throw IOException("Empty body from Gemini API")
            }
        }

        return parseGeminiResponse(rawJson)
    }

    // ── Prompt + request body ─────────────────────────────────────────────────

    private fun buildRequestBody(merchantId: String, salesHistory: List<Int>): String {
        val historyStr = salesHistory.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "no data"

        val prompt = """
You are SurplusIQ, an AI for Reskyu — a platform that rescues surplus restaurant food.

Merchant ID: $merchantId
Past ${salesHistory.size} days of meals sold (oldest first): [$historyStr]

Task: Predict the optimal number of surplus meals this merchant should prepare TODAY
to minimise waste while maximising customer sales.

Rules:
- Base your prediction on the trend in the sales history.
- If history is sparse, suggest a conservative number (3–6).
- Keep reasoning concise (under 15 words, no jargon).
- Confidence should reflect prediction reliability (0.0–1.0).

Respond ONLY with valid JSON — no markdown, no code fences:
{"predictedMeals": <integer>, "reasoning": "<string under 15 words>", "confidence": <float>}
        """.trimIndent()

        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("response_mime_type", "application/json")
                put("temperature", 0.4)
                put("maxOutputTokens", 128)
            })
        }.toString()
    }

    // ── Response parsing ──────────────────────────────────────────────────────

    private fun parseGeminiResponse(rawJson: String): SurplusIqResult {
        return try {
            val text = JSONObject(rawJson)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()

            val result = JSONObject(text)
            SurplusIqResult(
                predictedMeals = result.getInt("predictedMeals"),
                reasoning      = result.optString("reasoning", ""),
                confidence     = result.optDouble("confidence", 0.75).toFloat()
            )
        } catch (e: Exception) {
            throw IOException("Failed to parse Gemini response: ${e.message}")
        }
    }
}
