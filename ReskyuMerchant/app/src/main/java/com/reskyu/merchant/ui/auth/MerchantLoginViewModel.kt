package com.reskyu.merchant.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.merchant.data.model.LoginState
import com.reskyu.merchant.data.repository.MerchantAuthRepository
import com.reskyu.merchant.data.repository.MerchantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MerchantLoginViewModel : ViewModel() {

    private val authRepository = MerchantAuthRepository()
    private val merchantRepository = MerchantRepository()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    /**
     * Attempts Firebase email/password sign-in then checks for a merchant document.
     */
    fun signIn(email: String, password: String) {
        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val uid = authRepository.signIn(email, password)
                val merchant = merchantRepository.getMerchant(uid)
                _loginState.value = LoginState.Success(merchant)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.localizedMessage ?: "Login failed")
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}
