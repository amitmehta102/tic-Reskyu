package com.reskyu.merchant.ui.analytics

import com.reskyu.merchant.data.model.DemandResult

/**
 * UI states for the Local Demand Intelligence card.
 *
 * State machine:
 *   [Idle]    → initial state before any load is triggered
 *   [Loading] → Firestore fetch + Gemini call in progress
 *   [Success] → AI returned a valid DemandResult
 *   [Error]   → network or AI failure (shows a retry option)
 */
sealed class DemandUiState {

    /** No load has been triggered yet. */
    object Idle : DemandUiState()

    /** Fetching listings + calling Gemini. */
    object Loading : DemandUiState()

    /** Demand data successfully fetched and interpreted. */
    data class Success(val result: DemandResult) : DemandUiState()

    /** An error occurred — message is user-friendly. */
    data class Error(val message: String) : DemandUiState()
}
