package com.reskyu.merchant.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.reskyu.merchant.data.model.ListingType
import com.reskyu.merchant.data.model.MerchantClaim
import com.reskyu.merchant.data.model.SurplusIqContext
import com.reskyu.merchant.data.model.SurplusIqResult
import com.reskyu.merchant.data.remote.GeminiApiService
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.util.Calendar

/**
 * One-prediction-per-day-per-user SurplusIQ orchestrator.
 *
 * Cache stored in Firestore /merchants/{uid}:
 *   lastPredictionDate        → "YYYY-MM-DD"
 *   lastPredictionMeals       → Int
 *   lastPredictionReason      → String
 *   lastPredictionConfidence  → Double
 *   lastPredictionBestTime    → String
 *   lastPredictionPricingHint → String
 *   lastPredictionActionTip   → String
 *
 * If [today == lastPredictionDate] → return cached result instantly (no Gemini call).
 * Otherwise → build rich [SurplusIqContext] from claims + listings + merchant,
 *             call Gemini once, write result back to cache.
 */
object SurplusIqRepository {

    private val db            = FirebaseFirestore.getInstance()
    private val merchantsCol  = db.collection("merchants")
    private val claimsCol     = db.collection("claims")
    private val listingsCol   = db.collection("listings")

    // ── Public entry point ────────────────────────────────────────────────────

    /**
     * Returns today's SurplusIQ prediction.
     * Safe to call from multiple ViewModels — the Firestore cache ensures only
     * ONE Gemini call per merchant per calendar day.
     */
    suspend fun getPrediction(
        uid          : String,
        salesHistory : List<Int>        = emptyList(),    // legacy param, kept for compat
        context      : SurplusIqContext? = null           // if provided, skips auto-build
    ): SurplusIqResult {
        val today = LocalDate.now().toString()   // "2026-04-04"

        // ── 1. Cache check ─────────────────────────────────────────────────────
        val cached = runCatching { merchantsCol.document(uid).get().await() }.getOrNull()
        val cachedDate  = cached?.getString("lastPredictionDate") ?: ""

        if (cachedDate == today) {
            val meals   = cached?.getLong("lastPredictionMeals")?.toInt()       ?: 0
            val reason  = cached?.getString("lastPredictionReason")             ?: ""
            val conf    = cached?.getDouble("lastPredictionConfidence")?.toFloat() ?: 0.85f
            val best    = cached?.getString("lastPredictionBestTime")           ?: ""
            val price   = cached?.getString("lastPredictionPricingHint")        ?: ""
            val tip     = cached?.getString("lastPredictionActionTip")          ?: ""

            if (meals > 0) {
                return SurplusIqResult(
                    predictedMeals = meals,
                    reasoning      = reason,
                    confidence     = conf,
                    cachedDate     = today,
                    bestTimeToList = best,
                    pricingHint    = price,
                    actionTip      = tip
                )
            }
        }

        // ── 2. Build rich context if not provided ──────────────────────────────
        val richContext = context ?: buildContext(uid, cached, salesHistory)

        // ── 3. Call Gemini ─────────────────────────────────────────────────────
        val result = GeminiApiService.predict(richContext).copy(cachedDate = today)

        // ── 4. Write full result to Firestore cache ────────────────────────────
        runCatching {
            merchantsCol.document(uid).set(
                mapOf(
                    "lastPredictionDate"        to today,
                    "lastPredictionMeals"       to result.predictedMeals,
                    "lastPredictionReason"      to result.reasoning,
                    "lastPredictionConfidence"  to result.confidence.toDouble(),
                    "lastPredictionBestTime"    to result.bestTimeToList,
                    "lastPredictionPricingHint" to result.pricingHint,
                    "lastPredictionActionTip"   to result.actionTip
                ),
                SetOptions.merge()
            ).await()
        }

        return result
    }

    // ── Context builder ───────────────────────────────────────────────────────

    /**
     * Assembles a [SurplusIqContext] from all available merchant data in Firestore.
     * Called automatically when no context is provided and no cache exists.
     */
    private suspend fun buildContext(
        uid          : String,
        merchantDoc  : com.google.firebase.firestore.DocumentSnapshot?,
        legacyHistory: List<Int>
    ): SurplusIqContext {
        val dayMs   = 24L * 60 * 60 * 1000L
        val now     = System.currentTimeMillis()
        val zone    = ZoneId.systemDefault()

        // ── Fetch all historical claims ────────────────────────────────────────
        val allClaims = runCatching {
            claimsCol.whereEqualTo("merchantId", uid).get().await()
                .documents.mapNotNull { mapClaim(it) }
        }.getOrElse { emptyList() }

        val completed  = allClaims.filter { it.status == "COMPLETED" }
        val disputed   = allClaims.filter { it.status == "DISPUTED" }
        val total      = allClaims.size

        // ── 7-day daily counts ─────────────────────────────────────────────────
        val weekAgo = now - 7 * dayMs
        val sales7  = (0..6).map { d ->
            val start = weekAgo + d * dayMs
            completed.count { it.timestamp in start until start + dayMs }
        }

        // ── 30-day daily counts ────────────────────────────────────────────────
        val monthAgo = now - 30 * dayMs
        val sales30  = (0..29).map { d ->
            val start = monthAgo + d * dayMs
            completed.count { it.timestamp in start until start + dayMs }
        }

        // ── 7-day daily revenue ────────────────────────────────────────────────
        val revenue7 = (0..6).map { d ->
            val start = weekAgo + d * dayMs
            completed.filter { it.timestamp in start until start + dayMs }.sumOf { it.amount }
        }

        // ── Top selling items ──────────────────────────────────────────────────
        val topItems = completed
            .groupBy { it.heroItem.trim().lowercase() }
            .entries
            .sortedByDescending { it.value.size }
            .take(3)
            .map { it.value.first().heroItem }  // original casing

        // ── Dietary split from listings ────────────────────────────────────────
        val listings = runCatching {
            listingsCol.whereEqualTo("merchantId", uid).get().await()
                .documents.mapNotNull { doc ->
                    Pair(
                        doc.getString("dietaryTag")  ?: "",
                        doc.getString("listingType") ?: ListingType.REGULAR.name
                    )
                }
        }.getOrElse { emptyList() }

        val listingCount   = listings.size.coerceAtLeast(1)
        val vegCount       = listings.count { it.first.contains("VEG", ignoreCase = true) && !it.first.contains("NON", ignoreCase = true) }
        val nonVegCount    = listings.count { it.first.contains("NON_VEG", ignoreCase = true) }
        val mysteryCount   = listings.count { it.second == ListingType.MYSTERY_BOX.name }

        // ── Avg sell-out time ─────────────────────────────────────────────────
        // Heuristic: treat claims where time-to-pickup < 60 min as fast sell-outs
        val quickSales = completed.count { c ->
            // We don't store listing-posted-time, so use claim bunching as proxy
            // Future: derive from listing.expiresAt vs first claim timestamp
            c.amount > 0
        }
        val avgSelloutMins = if (completed.isNotEmpty()) {
            // Proxy: avg spacing between consecutive claims on same day (smaller = faster sells)
            val gaps = completed
                .sortedBy { it.timestamp }
                .zipWithNext { a, b -> b.timestamp - a.timestamp }
                .filter { it in 1..3_600_000 }  // 1 min–60 min gaps (same session)
            if (gaps.isNotEmpty()) (gaps.average() / 60_000).toInt() else 0
        } else 0

        // ── Cancellation / dispute rate ───────────────────────────────────────
        val cancellationRate = if (total > 0) (disputed.size * 100) / total else 0

        // ── Merchant profile fields ───────────────────────────────────────────
        val closingTime = merchantDoc?.getString("closingTime") ?: ""

        // ── Calendar context ──────────────────────────────────────────────────
        val cal        = Calendar.getInstance()
        val dayOfWeek  = DayOfWeek.of(if (cal.get(Calendar.DAY_OF_WEEK) == 1) 7 else cal.get(Calendar.DAY_OF_WEEK) - 1).name
        val monthName  = Month.of(cal.get(Calendar.MONTH) + 1).name
            .lowercase().replaceFirstChar { it.uppercase() }

        return SurplusIqContext(
            salesLast7Days     = if (legacyHistory.isNotEmpty() && sales7.all { it == 0 }) legacyHistory else sales7,
            salesLast30Days    = sales30,
            revenueLast7Days   = revenue7,
            topItems           = topItems,
            vegPercent         = (vegCount * 100) / listingCount,
            nonVegPercent      = (nonVegCount * 100) / listingCount,
            mysteryBoxPercent  = (mysteryCount * 100) / listingCount,
            avgSelloutMinutes  = avgSelloutMins,
            cancellationRate   = cancellationRate,
            closingTime        = closingTime,
            dayOfWeek          = dayOfWeek,
            monthName          = monthName
        )
    }

    // ── Claim mapper (private, mirrors MerchantClaimRepository) ─────────────

    private fun mapClaim(doc: com.google.firebase.firestore.DocumentSnapshot): MerchantClaim? {
        return try {
            val rawTs = doc.get("timestamp")
            val ts: Long = when (rawTs) {
                is com.google.firebase.Timestamp -> rawTs.toDate().time
                is Long   -> rawTs
                is Number -> rawTs.toLong()
                else      -> 0L
            }
            MerchantClaim(
                id           = doc.id,
                userId       = doc.getString("userId")       ?: "",
                merchantId   = doc.getString("merchantId")   ?: "",
                listingId    = doc.getString("listingId")    ?: "",
                businessName = doc.getString("businessName") ?: "",
                heroItem     = doc.getString("heroItem")     ?: "",
                paymentId    = doc.getString("paymentId")    ?: "",
                amount       = doc.getDouble("amount")       ?: 0.0,
                timestamp    = ts,
                status       = doc.getString("status")       ?: "PENDING_PICKUP"
            )
        } catch (e: Exception) { null }
    }
}
