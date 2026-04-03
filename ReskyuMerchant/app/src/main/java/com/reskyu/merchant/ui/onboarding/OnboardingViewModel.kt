package com.reskyu.merchant.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.merchant.data.model.MerchantDraft
import com.reskyu.merchant.data.model.SaveState
import com.reskyu.merchant.data.repository.MerchantAuthRepository
import com.reskyu.merchant.data.repository.MerchantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel : ViewModel() {

    private val authRepository = MerchantAuthRepository()
    private val merchantRepository = MerchantRepository()

    private val _draft = MutableStateFlow(MerchantDraft())
    val draft: StateFlow<MerchantDraft> = _draft

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    fun updateBusinessName(name: String) {
        _draft.value = _draft.value.copy(businessName = name)
    }

    fun updateClosingTime(time: String) {
        _draft.value = _draft.value.copy(closingTime = time)
    }

    fun updateLocation(lat: Double, lng: Double, geoHash: String) {
        _draft.value = _draft.value.copy(lat = lat, lng = lng, geoHash = geoHash)
    }

    /**
     * Finalises onboarding by committing the [MerchantDraft] to Firestore.
     */
    fun completeOnboarding() {
        val uid = authRepository.getCurrentUid() ?: return
        _saveState.value = SaveState.Saving

        viewModelScope.launch {
            try {
                val finalDraft = _draft.value.copy(uid = uid)
                merchantRepository.completeMerchantOnboarding(finalDraft)
                _saveState.value = SaveState.Saved
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.localizedMessage ?: "Onboarding failed")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}
