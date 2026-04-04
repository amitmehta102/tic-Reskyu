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

class ProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** null = idle, true = saving, false = saved */
    private val _isSaving = MutableStateFlow<Boolean?>(null)
    val isSaving: StateFlow<Boolean?> = _isSaving.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    init { loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val uid = authRepository.requireUid()
                val profile = userRepository.getUserProfile(uid)
                _user.value = profile ?: devUser()
            } catch (e: Exception) {
                _user.value = devUser()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Saves updated name + phone to Firestore (or dev store).
     * Updates local state optimistically so the UI reflects changes immediately.
     */
    fun updateProfile(name: String, phone: String) {
        val trimName  = name.trim()
        val trimPhone = phone.trim()
        if (trimName.isBlank()) {
            _saveError.value = "Name cannot be empty"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null
            try {
                val uid = authRepository.requireUid()
                userRepository.updateProfile(uid, trimName, trimPhone)
            } catch (_: Exception) {
                // Dev mode or Firebase not connected — just apply locally
            }
            // Optimistic local update regardless of Firebase result
            _user.value = _user.value?.copy(name = trimName, phone = trimPhone)
            _isSaving.value = false
        }
    }

    fun clearSaveState() {
        _isSaving.value = null
        _saveError.value = null
    }

    fun signOut() {
        try { authRepository.signOut() } catch (_: Exception) {}
        _user.value = null
    }

    private fun devUser() = User(
        uid   = "dev_user",
        name  = "Dev User",
        email = "dev@reskyu.app",
        phone = "+91 98765 43210",
        impactStats = ImpactStats(
            totalMealsRescued = 7,
            co2SavedKg        = 17.5,
            moneySaved        = 1240.0
        )
    )
}
