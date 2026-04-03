package com.reskyu.consumer.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * Loads the current user's profile from Firestore and exposes it
 * to [ProfileScreen]. Also handles sign-out.
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
                _user.value = userRepository.getUserProfile(uid)
            } catch (e: Exception) {
                // User not authenticated or profile not found
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Signs the user out of Firebase.
     * Navigation to the Login screen is handled by the UI after this call.
     */
    fun signOut() = authRepository.signOut()
}
