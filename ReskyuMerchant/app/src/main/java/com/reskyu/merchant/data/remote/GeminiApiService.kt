package com.reskyu.merchant.data.remote

import com.reskyu.merchant.BuildConfig
import com.reskyu.merchant.data.model.SurplusIqContext
import com.reskyu.merchant.data.model.SurplusIqResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Gemini 2.0 Flash client for SurplusIQ — maximally informed version.
 *
 * Sends the richest possible merchant context (30-day history, revenue,
 * top items, dietary split, mystery box ratio, sell-out speed, cancellation
 * rate, closing time, day of week, month) and receives 6 structured outputs:
 *   meals, confidence, reason, bestTimeToList, pricingHint, actionTip
 *
 * Key decisions:
 *  - API key read lazily (BuildConfig safety)
 *  - OkHttp only — no Retrofit, minimal failure surface
 *  - temperature=0.25 — creative but reliable
 *  - Fenced-markdown strip in parseResponse for robustness
 */
object GeminiApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build()

    /**
     * Calls Gemini with a rich [SurplusIqContext] and returns a [SurplusIqResult].
     * One call per merchant per day — caching is handled by [SurplusIqRepository].
     */
    suspend fun predict(ctx: SurplusIqContext): SurplusIqResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        check(apiKey.isNotBlank()) { "GEMINI_API_KEY is not set in local.properties" }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/" +
                  "gemini-2.0-flash:generateContent?key=$apiKey"

        val prompt = buildPrompt(ctx)

        val body = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature",    0.25)
                put("maxOutputTokens", 256)
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

    // ── Prompt builder ────────────────────────────────────────────────────────

    private fun buildPrompt(ctx: SurplusIqContext): String {
        val history7  = if (ctx.salesLast7Days.isEmpty())  "no data"
                        else ctx.salesLast7Days.joinToString(", ")
        val history30 = if (ctx.salesLast30Days.isEmpty()) "no data"
                        else ctx.salesLast30Days.joinToString(", ")
        val revenue7  = if (ctx.revenueLast7Days.isEmpty()) "no data"
                        else ctx.revenueLast7Days.joinToString(", ") { "₹${it.toInt()}" }
        val topItems  = if (ctx.topItems.isEmpty()) "unknown"
                        else ctx.topItems.take(3).joinToString(", ")

        return """
You are SurplusIQ, an elite AI food-rescue advisor embedded in a merchant app.
Your job: Analyse this merchant's data and give a maximally actionable prediction for TODAY.

━━━ MERCHANT DATA ━━━
Today              : ${ctx.dayOfWeek}, ${ctx.monthName} ${LocalDate.now().dayOfMonth}
Closing time       : ${ctx.closingTime.ifBlank { "unknown" }}

Sales last 7 days  (oldest→newest, completed claims/day): [$history7]
Sales last 30 days (oldest→newest): [$history30]
Revenue last 7 days: [$revenue7]

Top items        : $topItems
Dietary split    : ${ctx.vegPercent}% veg, ${ctx.nonVegPercent}% non-veg
Mystery box ratio: ${ctx.mysteryBoxPercent}% of listings are mystery boxes
Avg sell-out time: ${if (ctx.avgSelloutMinutes > 0) "${ctx.avgSelloutMinutes} mins" else "hasn't sold out yet"}
Cancellation rate: ${ctx.cancellationRate}%
━━━━━━━━━━━━━━━━━━━━━

Respond ONLY with valid JSON — no markdown, no code fences, no commentary.
Base your advice on ${ctx.dayOfWeek} patterns from the 30-day history specifically.
Use Indian context (₹ prices, Indian meal times, Indian food culture).

JSON format:
{
  "meals": <integer — surplus meals to prepare today>,
  "confidence": <float 0.00–1.00>,
  "reason": "<max 15 words — why this number>",
  "bestTimeToList": "<time window e.g. '6–8 PM' or '12–1 PM'>",
  "pricingHint": "<1 sentence — optimal price or discount strategy for today>",
  "actionTip": "<1 sentence — most impactful action to reduce waste today>"
}
        """.trimIndent()
    }

    // ── Response parser ───────────────────────────────────────────────────────

    private fun parseResponse(raw: String): SurplusIqResult {
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
                // Strip markdown fences robustly
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

            val json       = JSONObject(text)
            val meals      = json.optInt("meals",          json.optInt("predictedMeals", 6))
            val confidence = json.optDouble("confidence",  0.80).toFloat().coerceIn(0f, 1f)
            val reason     = json.optString("reason",      "Based on recent trend")
            val bestTime   = json.optString("bestTimeToList",  "")
            val pricing    = json.optString("pricingHint",     "")
            val tip        = json.optString("actionTip",       "")

            SurplusIqResult(
                predictedMeals = meals,
                reasoning      = reason,
                confidence     = confidence,
                bestTimeToList = bestTime,
                pricingHint    = pricing,
                actionTip      = tip
            )
        } catch (e: Exception) {
            val fallback = Regex("\\d+").find(raw)?.value?.toIntOrNull() ?: 6
            SurplusIqResult(
                predictedMeals = fallback,
                reasoning      = "AI prediction",
                confidence     = 0.70f
            )
        }
    }
}
