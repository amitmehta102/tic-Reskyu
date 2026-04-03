package com.reskyu.merchant.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.merchant.data.model.Merchant
import com.reskyu.merchant.data.model.SaveState
import com.reskyu.merchant.data.repository.MerchantAuthRepository
import com.reskyu.merchant.data.repository.MerchantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MerchantProfileViewModel : ViewModel() {

    private val authRepository = MerchantAuthRepository()
    private val merchantRepository = MerchantRepository()

    private val _merchant = MutableStateFlow<Merchant?>(null)
    val merchant: StateFlow<Merchant?> = _merchant

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

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

    fun signOut() {
        authRepository.signOut()
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}
