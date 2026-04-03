package com.reskyu.consumer.data.model

/**
 * AuthState
 *
 * Sealed class representing the user's Firebase Authentication status.
 * Observed by SplashViewModel to decide the initial navigation destination.
 *
 * States:
 *  - Authenticated   : A valid Firebase user session exists. Carries the uid.
 *  - Unauthenticated : No session found; navigate to LoginScreen.
 */
sealed class AuthState {
    data class Authenticated(val uid: String) : AuthState()
    object Unauthenticated : AuthState()
}
