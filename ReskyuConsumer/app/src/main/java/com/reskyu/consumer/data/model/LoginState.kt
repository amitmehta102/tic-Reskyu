package com.reskyu.consumer.data.model

/**
 * LoginState
 *
 * Sealed class representing the UI state of the Login screen during
 * the Firebase Authentication flow (OTP / Google Sign-In).
 *
 * States:
 *  - Idle      : Default state, no action in progress
 *  - Loading   : Auth request is in-flight; show LoadingOverlay
 *  - Success   : Auth succeeded; navigate to HomeScreen
 *  - Error     : Auth failed; show error message to user
 */
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
