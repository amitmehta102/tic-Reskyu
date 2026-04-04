package com.reskyu.merchant.ui.analytics

import com.reskyu.merchant.data.model.SurplusIqResult

/**
 * UI states for the SurplusIQ prediction card.
 *
 * State machine:
 *
 *   [Loading] → [Success]         (Gemini returned a prediction)
 *   [Loading] → [NewRestaurant]   (fewer than MIN_LISTINGS meals in history — skip Gemini)
 *   [Loading] → [Error]           (Gemini error, only shown when user explicitly retries)
 */
sealed class SurplusIqUiState {

    /** Gemini call is in-flight. */
    object Loading : SurplusIqUiState()

    /** Gemini returned a valid prediction or cached result is available. */
    data class Success(val result: SurplusIqResult) : SurplusIqUiState()

    /**
     * Not enough listing history to generate a meaningful AI prediction.
     *
     * @param mealsRescued  How many meals the merchant has rescued so far.
     * @param required      Threshold before Gemini is consulted (default 5).
     */
    data class NewRestaurant(
        val mealsRescued: Int,
        val required: Int = GEMINI_THRESHOLD
    ) : SurplusIqUiState()

    /** Explicit Gemini error — only surfaced when the user taps Retry. */
    data class Error(val message: String) : SurplusIqUiState()

    companion object {
        /** Minimum real meals rescued before we call Gemini. */
        const val GEMINI_THRESHOLD = 5
    }
}
