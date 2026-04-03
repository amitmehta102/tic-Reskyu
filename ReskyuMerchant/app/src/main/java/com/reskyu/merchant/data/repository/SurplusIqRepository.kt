package com.reskyu.merchant.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.reskyu.merchant.data.model.SurplusIqResult
import com.reskyu.merchant.data.remote.GeminiApiService
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/**
 * Orchestrates AI surplus predictions by:
 * 1. Checking the Firestore cache (lastPredictionDate on /merchants/{uid}).
 * 2. If cache is stale, building the sales history and calling [GeminiApiService].
 * 3. Writing the new prediction back to the Firestore cache.
 */
class SurplusIqRepository(
    private val geminiApiService: GeminiApiService = GeminiApiService()
) {

    private val firestore = FirebaseFirestore.getInstance()
    private val merchantsCollection = firestore.collection("merchants")

    /**
     * Returns a [SurplusIqResult] for today.
     * Uses the Firestore-cached value if [lastPredictionDate] matches today's date.
     *
     * @param uid             Merchant UID.
     * @param lastPredDate    The cached date string from /merchants/{uid}.
     * @param lastPredMeals   The cached meal count from /merchants/{uid}.
     * @param salesHistory    Past daily meal-sold counts (used if cache is stale).
     */
    suspend fun getPrediction(
        uid: String,
        lastPredDate: String,
        lastPredMeals: Int,
        salesHistory: List<Int>
    ): SurplusIqResult {
        val today = LocalDate.now().toString()

        // Return cached result if still valid for today
        if (lastPredDate == today && lastPredMeals > 0) {
            return SurplusIqResult(
                predictedMeals = lastPredMeals,
                reasoning = "Cached prediction for today.",
                cachedDate = today
            )
        }

        // Fetch fresh prediction from Gemini Flash
        val result = geminiApiService.predictSurplus(
            merchantId = uid,
            salesHistory = salesHistory
        )

        // Persist the new prediction to Firestore cache
        merchantsCollection.document(uid).update(
            mapOf(
                "lastPredictionDate" to today,
                "lastPredictionMeals" to result.predictedMeals
            )
        ).await()

        return result
    }
}
