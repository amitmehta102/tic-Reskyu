package com.reskyu.consumer.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.consumer.data.model.ImpactStats
import com.reskyu.consumer.data.model.User
import com.reskyu.consumer.data.repository.AuthRepository
import com.reskyu.consumer.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ProfileViewModel
 *
 * Loads the current user's profile from Firestore.
 * Falls back to a dev-mode User object when Firebase isn't configured,
 * so the Profile screen is fully visible during development.
 */
class ProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val uid = authRepository.requireUid()
                val profile = userRepository.getUserProfile(uid)
                _user.value = profile ?: devUser()
            } catch (e: Exception) {
                // Firebase not configured or user not authenticated → show dev profile
                _user.value = devUser()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        try {
            authRepository.signOut()
        } catch (_: Exception) { /* no-op in dev */ }
        _user.value = null
    }

    /** Placeholder profile shown during development */
    private fun devUser() = User(
        uid = "dev_user",
        name = "Dev User",
        email = "dev@reskyu.app",
        phone = "",
        impactStats = ImpactStats(
            totalMealsRescued = 7,
            co2SavedKg = 17.5,
            moneySaved = 1240.0
        )
    )
}
