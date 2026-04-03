package com.reskyu.consumer.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.consumer.data.model.LoginState
import com.reskyu.consumer.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * LoginViewModel
 *
 * Manages the Firebase Auth (OTP) flow for the Login screen.
 * Exposes a [loginState] StateFlow that the UI collects.
 *
 * Auth Flow:
 *  1. User enters phone number → [sendOtp] called
 *  2. Firebase sends OTP SMS → UI shows OTP input field
 *  3. User enters OTP → [verifyOtp] called
 *  4. On success → [loginState] emits [LoginState.Success]
 */
class LoginViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    // Stores the verification ID returned by Firebase after sendOtp
    private var verificationId: String? = null

    /**
     * Initiates Firebase phone auth by sending an OTP to [phoneNumber].
     * TODO: Implement the actual Firebase verifyPhoneNumber call here.
     *
     * @param phoneNumber  E.164 formatted phone number (e.g., "+919876543210")
     */
    fun sendOtp(phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            _loginState.value = LoginState.Error("Please enter a valid phone number")
            return
        }

        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            try {
                // TODO: Call authRepository.sendOtp(phoneNumber) when implemented
                // verificationId = authRepository.sendOtp(phoneNumber)
                // _loginState.value = LoginState.OtpSent (add this state if needed)

                // Placeholder — remove when real auth is implemented
                _loginState.value = LoginState.Error("OTP auth not yet implemented")
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Failed to send OTP")
            }
        }
    }

    /**
     * Verifies the OTP entered by the user and signs them in.
     *
     * @param otp  The 6-digit code received via SMS
     */
    fun verifyOtp(otp: String) {
        val id = verificationId ?: run {
            _loginState.value = LoginState.Error("Please request an OTP first")
            return
        }

        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            try {
                // TODO: Call authRepository.signInWithOtp(id, otp) when implemented
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Invalid OTP")
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}
