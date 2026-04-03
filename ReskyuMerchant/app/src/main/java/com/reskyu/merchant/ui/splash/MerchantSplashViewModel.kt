package com.reskyu.merchant.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.merchant.data.model.MerchantAuthState
import com.reskyu.merchant.data.repository.MerchantAuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MerchantSplashViewModel : ViewModel() {

    private val authRepository = MerchantAuthRepository()

    /** Emits the real-time auth state to drive splash → correct start destination navigation. */
    val authState: StateFlow<MerchantAuthState> = authRepository.observeAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MerchantAuthState.Loading
        )
}
