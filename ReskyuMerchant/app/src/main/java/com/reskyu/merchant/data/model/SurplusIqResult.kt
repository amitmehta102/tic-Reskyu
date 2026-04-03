package com.reskyu.merchant.data.model

/**
 * Result returned by [SurplusIqRepository] after calling the Gemini Flash API.
 * Also cached in Firestore /merchants/{uid} as lastPredictionDate & lastPredictionMeals.
 */
data class SurplusIqResult(
    val predictedMeals: Int = 0,
    val reasoning: String = "",             // Human-readable explanation from Gemini
    val confidence: Float = 0f,             // 0.0 – 1.0
    val cachedDate: String = ""             // "YYYY-MM-DD" – matches lastPredictionDate
)
