package com.reskyu.merchant.ui.analytics

import com.reskyu.merchant.data.model.SellEverythingResult

/**
 * UI state machine for the "Sell Everything Mode" section on the ESG Analytics screen.
 *
 * State flow:
 *
 *   [Idle]
 *     ↓ (loadStats called, closing time checked)
 *     ├─→ [NotTriggered(minutesUntilClose)]   merchant is > 90 min from closing
 *     ├─→ [NoListings]                        no active listings to sell out
 *     └─→ [FetchingStrategy]                  within 90 min — calling Gemini
 *              ↓
 *         [Ready(result, mealsLeft, minutesUntilClose)]
 *              ↓ (merchant taps "Activate")
 *         [Confirming(result, mealsLeft, minutesUntilClose)]
 *              ↓ (merchant confirms dialog)
 *         [Applying]
 *              ↓
 *         [Applied(result, bundleListingId)]
 *              or
 *         [Error(message)]
 */
sealed class SellEverythingUiState {

    /** Initial state — preconditions not yet evaluated. */
    object Idle : SellEverythingUiState()

    /** Merchant is more than [TRIGGER_WINDOW_MINUTES] minutes from closing. */
    data class NotTriggered(val minutesUntilClose: Int) : SellEverythingUiState()

    /** No active listings exist — nothing to sell out. */
    object NoListings : SellEverythingUiState()

    /** Gemini is being called to generate the sell-out strategy. */
    object FetchingStrategy : SellEverythingUiState()

    /** AI strategy is ready — awaiting merchant action. */
    data class Ready(
        val result: SellEverythingResult,
        val mealsLeft: Int,
        val minutesUntilClose: Int
    ) : SellEverythingUiState()

    /**
     * Merchant tapped "Activate" — show confirmation dialog before applying changes.
     * Same payload as [Ready].
     */
    data class Confirming(
        val result: SellEverythingResult,
        val mealsLeft: Int,
        val minutesUntilClose: Int
    ) : SellEverythingUiState()

    /** Discounts are being applied to Firestore and bundle listing is being created. */
    object Applying : SellEverythingUiState()

    /** All changes applied successfully. */
    data class Applied(
        val result: SellEverythingResult,
        val bundleListingId: String       // ID of the auto-created bundle listing in Firestore
    ) : SellEverythingUiState()

    /** Something went wrong. */
    data class Error(val message: String) : SellEverythingUiState()

    companion object {
        /** Trigger window in minutes — mode activates within this time from closing. */
        const val TRIGGER_WINDOW_MINUTES = 90
    }
}
