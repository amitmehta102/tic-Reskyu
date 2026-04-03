package com.reskyu.merchant.data.model

/**
 * Sealed class representing the result of a login attempt.
 */
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val merchant: Merchant?) : LoginState()  // null → needs onboarding
    data class Error(val message: String) : LoginState()
}
