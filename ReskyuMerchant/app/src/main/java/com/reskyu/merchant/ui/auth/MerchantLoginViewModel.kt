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

    private val authRepository     = MerchantAuthRepository()
    private val merchantRepository = MerchantRepository()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    /** True for one emission after a password-reset email is dispatched. */
    private val _passwordResetSent = MutableStateFlow(false)
    val passwordResetSent: StateFlow<Boolean> = _passwordResetSent

    // ── Sign In ───────────────────────────────────────────────────────────────

    fun signIn(email: String, password: String) {
        val error = validateSignIn(email, password)
        if (error != null) { _loginState.value = LoginState.Error(error); return }

        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val uid      = authRepository.signIn(email, password)
                val merchant = merchantRepository.getMerchant(uid)
                _loginState.value = LoginState.Success(merchant)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(parseFirebaseError(e))
            }
        }
    }

    // ── Register ──────────────────────────────────────────────────────────────

    fun register(email: String, password: String, confirmPassword: String) {
        val error = validateRegister(email, password, confirmPassword)
        if (error != null) { _loginState.value = LoginState.Error(error); return }

        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            try {
                authRepository.register(email, password)
                // New user — no merchant doc yet → will go to Onboarding
                _loginState.value = LoginState.Success(merchant = null)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(parseFirebaseError(e))
            }
        }
    }

    // ── Password reset ────────────────────────────────────────────────────────

    fun sendPasswordReset(email: String) {
        if (!isEmailValid(email)) {
            _loginState.value = LoginState.Error("Please enter a valid email address")
            return
        }
        viewModelScope.launch {
            try {
                authRepository.sendPasswordReset(email)
                _passwordResetSent.value = true
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(parseFirebaseError(e))
            }
        }
    }

    fun resetPasswordResetSent() { _passwordResetSent.value = false }

    fun resetState() { _loginState.value = LoginState.Idle }

    // ── Validation ────────────────────────────────────────────────────────────

    private fun validateSignIn(email: String, password: String): String? = when {
        !isEmailValid(email) -> "Please enter a valid email address"
        password.isBlank()   -> "Please enter your password"
        else                 -> null
    }

    private fun validateRegister(email: String, password: String, confirmPassword: String): String? = when {
        !isEmailValid(email)        -> "Please enter a valid email address"
        password.length < 6         -> "Password must be at least 6 characters"
        password != confirmPassword -> "Passwords don't match"
        else                        -> null
    }

    private fun isEmailValid(email: String) =
        email.isNotBlank() && email.contains("@") && email.contains(".")

    // ── Firebase error → user-friendly message ────────────────────────────────

    private fun parseFirebaseError(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            "INVALID_LOGIN_CREDENTIALS"    in msg -> "Incorrect email or password"
            "invalid-credential"           in msg -> "Incorrect email or password"
            "EMAIL_NOT_FOUND"              in msg -> "No account found with this email"
            "WRONG_PASSWORD"               in msg -> "Incorrect password"
            "EMAIL_EXISTS"                 in msg -> "An account already exists with this email"
            "email-already-in-use"         in msg -> "An account already exists with this email"
            "WEAK_PASSWORD"                in msg -> "Password is too weak (min 6 characters)"
            "INVALID_EMAIL"                in msg -> "Please enter a valid email address"
            "TOO_MANY_REQUESTS"            in msg -> "Too many attempts. Please try again later"
            "NETWORK_ERROR"                in msg -> "Network error. Check your connection"
            "network"      in msg.lowercase()     -> "Network error. Check your connection"
            else                                  -> "Something went wrong. Please try again"
        }
    }
}
