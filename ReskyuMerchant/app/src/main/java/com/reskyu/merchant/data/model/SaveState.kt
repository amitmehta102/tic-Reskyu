package com.reskyu.merchant.data.model

/**
 * Sealed class for onboarding step save operations.
 */
sealed class SaveState {
    object Idle : SaveState()
    object Saving : SaveState()
    object Saved : SaveState()
    data class Error(val message: String) : SaveState()
}
