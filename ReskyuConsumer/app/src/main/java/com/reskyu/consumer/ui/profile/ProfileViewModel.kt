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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow<Boolean?>(null)
    val isSaving: StateFlow<Boolean?> = _isSaving.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    private val _privacyPolicy = MutableStateFlow<String?>(null)
    val privacyPolicy: StateFlow<String?> = _privacyPolicy.asStateFlow()

    private val _isPolicyLoading = MutableStateFlow(false)
    val isPolicyLoading: StateFlow<Boolean> = _isPolicyLoading.asStateFlow()

    init {
        _user.value = devUser()   // pre-seed so UI is never blank
        subscribeToProfile()
    }

    private fun subscribeToProfile() {
        val uid = try { authRepository.requireUid() } catch (_: Exception) {
            _isLoading.value = false
            return
        }
        viewModelScope.launch {
            userRepository
                .observeUserProfile(uid)
                .catch { _isLoading.value = false }
                .collect { profile ->
                    _isLoading.value = false
                    if (profile != null) _user.value = profile
                }
        }
    }

    fun updateProfile(name: String, phone: String) {
        val trimName  = name.trim()
        val trimPhone = phone.trim()
        if (trimName.isBlank()) { _saveError.value = "Name cannot be empty"; return }

        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null
            try {
                val uid = authRepository.requireUid()
                userRepository.updateProfile(uid, trimName, trimPhone)
            } catch (_: Exception) {  }
            _user.value = _user.value?.copy(name = trimName, phone = trimPhone)
            _isSaving.value = false
        }
    }

    fun clearSaveState() {
        _isSaving.value = null
        _saveError.value = null
    }

    fun updateNotificationPrefs(prefs: List<String>) {
        viewModelScope.launch {
            try {
                val uid = authRepository.requireUid()
                userRepository.updateNotificationPrefs(uid, prefs)
            } catch (_: Exception) {  }
            _user.value = _user.value?.copy(notificationPrefs = prefs)
        }
    }

    fun updateDiscoveryRadius(radiusKm: Int) {
        viewModelScope.launch {
            try {
                val uid = authRepository.requireUid()
                userRepository.updateDiscoveryRadius(uid, radiusKm)
            } catch (_: Exception) {  }
            _user.value = _user.value?.copy(discoveryRadiusKm = radiusKm)
        }
    }

    fun loadPrivacyPolicy() {
        if (_isPolicyLoading.value) return   // debounce concurrent taps
        viewModelScope.launch {
            _isPolicyLoading.value = true
            _privacyPolicy.value = null      // reset so stale content is cleared
            try {
                val result = userRepository.fetchPrivacyPolicy()
                android.util.Log.d("ProfileVM", "fetchPrivacyPolicy result: '$result'")
                _privacyPolicy.value = result ?: ""
            } catch (e: Exception) {
                android.util.Log.e("ProfileVM", "fetchPrivacyPolicy failed", e)
                _privacyPolicy.value = ""
            }
            _isPolicyLoading.value = false
        }
    }

    fun signOut() {
        try { authRepository.signOut() } catch (_: Exception) {}
        _user.value = null
    }

    fun uploadPrivacyPolicy() {
        viewModelScope.launch {
            try {
                val policy = """
Privacy Policy — Reskyu
Last updated: April 2025

1. Information We Collect
We collect your name, email address, and phone number when you register. We collect your device location to show nearby food listings. We store a device token to send you order and pickup notifications.

2. Payment Information
Payments are processed securely by Razorpay. We do not store your card or bank details — only a payment reference ID is saved to confirm your order.

3. How We Use Your Information
- To display food listings near you
- To confirm and track your food rescue orders
- To send pickup reminders and order updates
- To calculate your environmental impact (meals rescued, CO2 saved)

4. Data Sharing
We do not sell your personal data. Your order details are shared only with the merchant you place an order with. Payment data is handled by Razorpay under their privacy policy.

5. Data Storage
Your data is stored securely on Google Firebase (Firestore). Location data is used in real-time and is not stored permanently.

6. Your Rights
You can request deletion of your account and data by contacting us at reskyu123@gmail.com. Deleting your account removes all personal information from our systems within 30 days.

7. Contact
For any privacy concerns, email us at reskyu123@gmail.com.
                """.trimIndent()
                userRepository.uploadPrivacyPolicy(policy)
            } catch (_: Exception) {}
        }
    }

    private fun devUser() = User(
        uid   = "dev_user",
        name  = "Dev User",
        email = "dev@reskyu.app",
        phone = "+91 98765 43210",
        consumerType      = "INDIVIDUAL",
        notificationPrefs = emptyList(),
        discoveryRadiusKm = 2,
        impactStats = ImpactStats(
            totalMealsRescued = 7,
            co2SavedKg        = 17.5,
            moneySaved        = 1240.0
        )
    )
}
