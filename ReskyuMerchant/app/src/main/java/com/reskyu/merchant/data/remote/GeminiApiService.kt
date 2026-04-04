package com.reskyu.merchant.data.remote

import com.reskyu.merchant.BuildConfig
import com.reskyu.merchant.data.model.DemandContext
import com.reskyu.merchant.data.model.DemandResult
import com.reskyu.merchant.data.model.SellEverythingContext
import com.reskyu.merchant.data.model.SellEverythingResult
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
    // ── Demand interpretation ─────────────────────────────────────────────

    /**
     * Interprets a locally-pre-aggregated [DemandContext] and returns a short
     * actionable [DemandResult] — Gemini is used ONLY for this interpretation
     * layer; all aggregation / scoring was done without AI in [DemandRepository].
     */
    suspend fun interpretDemand(ctx: DemandContext): DemandResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        check(apiKey.isNotBlank()) { "GEMINI_API_KEY is not set in local.properties" }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/" +
                  "gemini-2.0-flash:generateContent?key=$apiKey"

        val prompt = buildDemandPrompt(ctx)

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
                put("maxOutputTokens", 128)
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
            parseDemandResponse(raw)
        }
    }

    private fun buildDemandPrompt(ctx: DemandContext): String {
        val priceBreakdown = when {
            ctx.budgetListings > 0 && ctx.premiumListings > 0 ->
                "${ctx.budgetListings} budget (≤₹99), ${ctx.premiumListings} premium (>₹99)"
            ctx.budgetListings > 0 -> "${ctx.budgetListings} budget (≤₹99)"
            ctx.premiumListings > 0 -> "${ctx.premiumListings} premium (>₹99)"
            else -> "unknown"
        }
        val dietaryBreakdown = when {
            ctx.vegListings > 0 && ctx.nonVegListings > 0 ->
                "${ctx.vegListings} veg, ${ctx.nonVegListings} non-veg"
            ctx.vegListings > 0 -> "${ctx.vegListings} veg"
            ctx.nonVegListings > 0 -> "${ctx.nonVegListings} non-veg"
            else -> "mixed"
        }
        return """
You are a local food-market analyst embedded in a merchant app in India.
You receive pre-aggregated demand data (computed WITHOUT AI) and must
interpret it into a short actionable recommendation for the merchant.

━━━ LOCAL DEMAND DATA (pre-aggregated, no AI) ━━━
Day / Hour        : ${ctx.dayOfWeek}, ${ctx.hourOfDay}:00
Nearby open food listings (within 2 km, last 2 hours): ${ctx.totalActiveListings}
Dietary breakdown : $dietaryBreakdown
Price breakdown   : $priceBreakdown
Local demand score: ${ctx.demandScore}
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Respond ONLY with valid JSON — no markdown, no code fences, no commentary.
Use Indian meal-time context (breakfast 7-10 AM, lunch 12-2 PM, snack 4-6 PM, dinner 7-9 PM).

Strict JSON format:
{
  "demand_level": "LOW | MEDIUM | HIGH",
  "best_action": "<1 sentence — what should the merchant do RIGHT NOW>",
  "best_listing_window": "next X minutes",
  "reason": "<max 12 words — why>"
}
        """.trimIndent()
    }

    private fun parseDemandResponse(raw: String): DemandResult {
        return try {
            val text = JSONObject(raw)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

            val json   = JSONObject(text)
            DemandResult(
                demandLevel       = json.optString("demand_level",        "MEDIUM"),
                bestAction        = json.optString("best_action",          "List your surplus food now"),
                bestListingWindow = json.optString("best_listing_window",  "next 30 minutes"),
                reason            = json.optString("reason",               "Moderate local activity")
            )
        } catch (e: Exception) {
            // Graceful fallback — never crash
            DemandResult(
                demandLevel       = "MEDIUM",
                bestAction        = "Consider listing your surplus food now",
                bestListingWindow = "next 30 minutes",
                reason            = "Demand data available"
            )
        }
    }

    // ── Sell Everything Mode ───────────────────────────────────────────────

    /**
     * Generates an AI-powered sell-out strategy for a merchant approaching closing time.
     *
     * Sends [SellEverythingContext] to Gemini 2.0 Flash and receives:
     *  - discount_percentage  : how aggressively to discount existing listings
     *  - bundle_price         : ₹ price for the auto-created bundle listing
     *  - bundle_meals         : how many meals per bundle unit (typically 2)
     *  - bundle_strategy      : human-readable explanation shown to merchant
     *  - urgency_message      : short emoji-rich message for consumer push notifications
     *  - push_required        : whether a push notification is warranted
     */
    suspend fun planSellEverything(ctx: SellEverythingContext): SellEverythingResult =
        withContext(Dispatchers.IO) {
            val apiKey = BuildConfig.GEMINI_API_KEY
            check(apiKey.isNotBlank()) { "GEMINI_API_KEY is not set in local.properties" }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/" +
                      "gemini-2.0-flash:generateContent?key=$apiKey"

            val prompt = buildSellEverythingPrompt(ctx)

            val body = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature",     0.30)
                    put("maxOutputTokens", 200)
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
                parseSellEverythingResponse(raw, ctx)
            }
        }

    private fun buildSellEverythingPrompt(ctx: SellEverythingContext): String {
        val sellOutRatePct = (ctx.historicalSellOutRate * 100).toInt()
        val avgPriceStr    = "₹${ctx.originalPriceAvg.toInt()}"
        return """
You are a food-rescue pricing expert for an Indian merchant app (Reskyu).
A merchant needs to sell ALL remaining food before they close. Help them.

━━━ SITUATION ━━━
Merchant         : ${ctx.merchantName.ifBlank { "Unknown" }}
Meals remaining  : ${ctx.mealsLeft}
Minutes to close : ${ctx.minutesUntilClose}
Avg item price   : $avgPriceStr
Top item         : ${ctx.topHeroItem.ifBlank { "Mixed items" }}
Historic sell-out: $sellOutRatePct% of listings sold out when active
━━━━━━━━━━━━━━━━━

Rules:
- Be aggressive: closing in ${ctx.minutesUntilClose} minutes — no time to be conservative.
- Discount must make it irresistible. Suggest 30-60% off.
- Bundle should combine 2 meals into a single deal at a compelling price.
- bundle_meals should be 2 (always pair them).
- urgency_message must be short, punchy, emoji-rich, under 12 words.
- push_required is true if mealsLeft > 3.
- Use Indian Rupee (₹) and Indian food culture context.

Respond ONLY with valid JSON — no markdown, no code fences, no commentary:
{
  "discount_percentage": <int 30-60>,
  "bundle_price": <float — ₹ price for a 2-meal bundle, lower than 2x discounted price>,
  "bundle_meals": 2,
  "bundle_strategy": "<1 sentence — what the bundle is and why buy it>",
  "urgency_message": "<emoji-rich, max 12 words, for push notification to consumers>",
  "push_required": <true|false>
}
        """.trimIndent()
    }

    private fun parseSellEverythingResponse(
        raw: String,
        ctx: SellEverythingContext
    ): SellEverythingResult {
        return try {
            val text = JSONObject(raw)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

            val json            = JSONObject(text)
            val discountPct     = json.optInt("discount_percentage", 40).coerceIn(10, 70)
            val bundlePrice     = json.optDouble("bundle_price",
                ctx.originalPriceAvg * 1.6 * (1.0 - discountPct / 100.0)).coerceAtLeast(1.0)
            val bundleMeals     = json.optInt("bundle_meals",    2).coerceIn(2, 5)
            val bundleStrategy  = json.optString("bundle_strategy",  "Bundle deal — great value!")
            val urgencyMessage  = json.optString("urgency_message",   "🔥 Last chance — closing soon!")
            val pushRequired    = json.optBoolean("push_required",    ctx.mealsLeft > 3)

            SellEverythingResult(
                discountPercentage = discountPct,
                bundlePrice        = bundlePrice,
                bundleMeals        = bundleMeals,
                bundleStrategy     = bundleStrategy,
                urgencyMessage     = urgencyMessage,
                pushRequired       = pushRequired
            )
        } catch (e: Exception) {
            // Safe fallback — 40% off, ₹2 bundle at avg_price * 1.5 * 0.6
            val fallbackBundle = (ctx.originalPriceAvg * 1.5 * 0.6).coerceAtLeast(39.0)
            SellEverythingResult(
                discountPercentage = 40,
                bundlePrice        = fallbackBundle,
                bundleMeals        = 2,
                bundleStrategy     = "2-meal bundle deal — grab it before we close!",
                urgencyMessage     = "🔥 Closing soon — grab our last meals cheap!",
                pushRequired       = ctx.mealsLeft > 3
            )
        }
    }
}
