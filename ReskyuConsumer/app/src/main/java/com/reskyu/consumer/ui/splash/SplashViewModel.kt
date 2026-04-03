package com.reskyu.consumer.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.consumer.data.model.AuthState
import com.reskyu.consumer.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * SplashViewModel
 *
 * Observes the Firebase Auth state and exposes it as a [StateFlow].
 * SplashScreen collects this and navigates accordingly.
 *
 * The null initial value means "still determining auth state" —
 * the splash UI stays visible until we get the first emission.
 */
class SplashViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    /**
     * Emits the current [AuthState] from Firebase.
     * Null = loading (first emission not yet received).
     */
    val authState: StateFlow<AuthState?> = authRepository
        .observeAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
}
