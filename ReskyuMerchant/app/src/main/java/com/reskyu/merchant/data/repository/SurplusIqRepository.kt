package com.reskyu.merchant.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.reskyu.merchant.data.model.SurplusIqResult
import com.reskyu.merchant.data.remote.GeminiApiService
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/**
 * One-prediction-per-day SurplusIQ cache + Gemini call orchestrator.
 *
 * Cache lives in Firestore: /merchants/{uid}.lastPredictionDate + lastPredictionMeals
 * If today's date matches, the cached value is returned instantly (no Gemini call).
 * Otherwise, Gemini is called and the result is written back to the cache.
 */
object SurplusIqRepository {

    private val merchantsCol = FirebaseFirestore.getInstance().collection("merchants")

    /**
     * Returns today's SurplusIQ prediction.
     *
     * @param uid          Merchant Firebase UID.
     * @param salesHistory Last 7 days of meal counts (oldest first).
     * @return [SurplusIqResult]
     */
    suspend fun getPrediction(uid: String, salesHistory: List<Int>): SurplusIqResult {
        val today = LocalDate.now().toString()   // "2026-04-04"

        // ── 1. Check Firestore cache ───────────────────────────────────────────
        val cached = runCatching {
            merchantsCol.document(uid).get().await()
        }.getOrNull()

        val cachedDate  = cached?.getString("lastPredictionDate")  ?: ""
        val cachedMeals = cached?.getLong("lastPredictionMeals")?.toInt() ?: 0

        if (cachedDate == today && cachedMeals > 0) {
            return SurplusIqResult(
                predictedMeals = cachedMeals,
                reasoning      = "Today's cached prediction",
                confidence     = 0.85f,
                cachedDate     = today
            )
        }

        // ── 2. Call Gemini ────────────────────────────────────────────────────
        val (meals, reason) = GeminiApiService.predict(
            salesHistory = salesHistory.ifEmpty { listOf(4, 6, 5, 8, 7, 9, 6) }
        )

        // ── 3. Write result to Firestore cache ────────────────────────────────
        runCatching {
            merchantsCol.document(uid).set(
                mapOf(
                    "lastPredictionDate"  to today,
                    "lastPredictionMeals" to meals
                ),
                SetOptions.merge()
            ).await()
        }

        return SurplusIqResult(
            predictedMeals = meals,
            reasoning      = reason,
            confidence     = 0.82f,
            cachedDate     = today
        )
    }
}
