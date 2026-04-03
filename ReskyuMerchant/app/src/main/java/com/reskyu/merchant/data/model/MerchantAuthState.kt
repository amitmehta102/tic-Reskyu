package com.reskyu.merchant.data.model

/**
 * Sealed class representing the authentication state of the merchant.
 * Drives the NavGraph to send the user to the correct start destination.
 */
sealed class MerchantAuthState {
    /** Firebase Auth is authenticated AND the /merchants/{uid} document exists. */
    data class Authenticated(val merchant: Merchant) : MerchantAuthState()

    /** Firebase Auth is authenticated BUT no /merchants/{uid} document exists yet. */
    object NeedsOnboarding : MerchantAuthState()

    /** No authenticated Firebase user. */
    object Unauthenticated : MerchantAuthState()

    /** Auth state is being determined (initial loading). */
    object Loading : MerchantAuthState()
}
