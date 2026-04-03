package com.reskyu.consumer.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.reskyu.consumer.data.model.AuthState
import com.reskyu.consumer.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * AuthRepository
 *
 * Handles all Firebase Authentication operations for the consumer app.
 * Currently structured for Phone (OTP) auth — swap methods for Google Sign-In if needed.
 *
 * Methods:
 *  - observeAuthState()  : Flow that emits on login/logout events
 *  - getCurrentUser()    : Returns the currently signed-in FirebaseUser or null
 *  - signOut()           : Signs the user out of Firebase
 */
class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Emits [AuthState.Authenticated] or [AuthState.Unauthenticated] reactively.
     * Use this in SplashViewModel to determine the start destination.
     */
    fun observeAuthState(): Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                trySend(AuthState.Authenticated(uid = user.uid))
            } else {
                trySend(AuthState.Unauthenticated)
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** Returns the currently signed-in user, or null if unauthenticated. */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /** Returns the current user's UID, or throws if not authenticated. */
    fun requireUid(): String =
        auth.currentUser?.uid ?: error("User is not authenticated")

    /** Signs the current user out of Firebase. */
    fun signOut() = auth.signOut()

    // TODO: Add phone auth methods (verifyPhoneNumber, signInWithCredential)
    // TODO: Add Google Sign-In method if required
}
