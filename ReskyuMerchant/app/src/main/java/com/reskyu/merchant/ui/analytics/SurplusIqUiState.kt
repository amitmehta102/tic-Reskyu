package com.reskyu.merchant.ui.analytics

import com.reskyu.merchant.data.model.SurplusIqResult

/** Three UI states for the SurplusIQ prediction card in EsgAnalyticsScreen. */
sealed class SurplusIqUiState {
    /** Gemini call is in-flight — show spinner. */
    object Loading : SurplusIqUiState()

    /** Gemini returned a valid prediction. */
    data class Success(val result: SurplusIqResult) : SurplusIqUiState()

    /** Network error, API key missing, or parse failure. */
    data class Error(val message: String) : SurplusIqUiState()
}
