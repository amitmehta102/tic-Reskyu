package com.reskyu.merchant.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.reskyu.merchant.data.model.Merchant
import com.reskyu.merchant.data.model.SaveState
import com.reskyu.merchant.data.repository.MerchantAuthRepository
import com.reskyu.merchant.data.repository.MerchantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MerchantProfileViewModel : ViewModel() {

    private val authRepository     = MerchantAuthRepository()
    private val merchantRepository = MerchantRepository()
    private val firestore          = FirebaseFirestore.getInstance()

    private val _merchant   = MutableStateFlow<Merchant?>(null)
    val merchant: StateFlow<Merchant?> = _merchant

    private val _saveState  = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    // ── Bottom sheet flags ────────────────────────────────────────────────────
    private val _showPrivacySheet = MutableStateFlow(false)
    val showPrivacySheet: StateFlow<Boolean> = _showPrivacySheet

    private val _showSupportSheet = MutableStateFlow(false)
    val showSupportSheet: StateFlow<Boolean> = _showSupportSheet

    // ── Privacy policy content (lazy-loaded from Firestore) ───────────────────
    private val _privacyContent  = MutableStateFlow<String?>(null)
    val privacyContent: StateFlow<String?> = _privacyContent

    private val _privacyLoading  = MutableStateFlow(false)
    val privacyLoading: StateFlow<Boolean> = _privacyLoading

    // ─────────────────────────────────────────────────────────────────────────

    fun loadProfile() {
        val uid = authRepository.getCurrentUid() ?: return
        viewModelScope.launch {
            _merchant.value = merchantRepository.getMerchant(uid)
        }
    }

    fun updateClosingTime(time: String) {
        val uid = authRepository.getCurrentUid() ?: return
        _saveState.value = SaveState.Saving
        viewModelScope.launch {
            try {
                merchantRepository.updateProfile(uid, mapOf("closingTime" to time))
                _merchant.value = _merchant.value?.copy(closingTime = time)
                _saveState.value = SaveState.Saved
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.localizedMessage ?: "Update failed")
            }
        }
    }

    fun signOut()        { authRepository.signOut() }
    fun resetSaveState() { _saveState.value = SaveState.Idle }

    // ── Sheet actions ─────────────────────────────────────────────────────────

    fun openPrivacyPolicy() {
        _showPrivacySheet.value = true
        if (_privacyContent.value == null && !_privacyLoading.value) fetchPrivacyPolicy()
    }
    fun closePrivacyPolicy() { _showPrivacySheet.value = false }

    fun openSupport()  { _showSupportSheet.value = true  }
    fun closeSupport() { _showSupportSheet.value = false }

    // ── Firestore fetch ───────────────────────────────────────────────────────

    private fun fetchPrivacyPolicy() {
        _privacyLoading.value = true
        viewModelScope.launch {
            try {
                val doc = firestore
                    .collection("config")
                    .document("privacy_policy")
                    .get()
                    .await()
                _privacyContent.value = doc.getString("content")
                    ?: "Privacy policy not available. Please contact reskyu123@gmail.com for details."
            } catch (e: Exception) {
                _privacyContent.value = "Could not load privacy policy. Please try again later."
            } finally {
                _privacyLoading.value = false
            }
        }
    }
}
