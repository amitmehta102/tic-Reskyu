package com.reskyu.consumer.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reskyu.consumer.data.model.LoginState
import com.reskyu.consumer.data.model.User
import com.reskyu.consumer.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * LoginViewModel
 *
 * Handles two authentication methods:
 *
 *  1. Phone (OTP):
 *     [sendOtp] → Firebase sends SMS → [verifyOtp] → sign in
 *     (PhoneAuthProvider callbacks need an Activity ref — see TODO below)
 *
 *  2. Email/Password:
 *     [signInWithEmail]  → Firebase signInWithEmailAndPassword
 *     [signUpWithEmail]  → Firebase createUserWithEmailAndPassword + Firestore profile
 */
class LoginViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> = _otpSent.asStateFlow()

    private var verificationId: String? = null

    // ── Email Auth ────────────────────────────────────────────────────────────

    /**
     * Signs in an existing user with email and password.
     */
    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.length < 6) {
            _loginState.value = LoginState.Error("Enter a valid email and password (min 6 chars)")
            return
        }
        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(
                    when {
                        e.message?.contains("no user record") == true ->
                            "No account found for this email. Sign up instead?"
                        e.message?.contains("password is invalid") == true ->
                            "Incorrect password. Please try again."
                        else -> e.message ?: "Sign in failed"
                    }
                )
            }
        }
    }

    /**
     * Creates a new account with email/password, then writes a Firestore user profile.
     */
    fun signUpWithEmail(name: String, email: String, password: String) {
        if (name.isBlank()) {
            _loginState.value = LoginState.Error("Please enter your name")
            return
        }
        if (email.isBlank()) {
            _loginState.value = LoginState.Error("Please enter a valid email")
            return
        }
        if (password.length < 6) {
            _loginState.value = LoginState.Error("Password must be at least 6 characters")
            return
        }
        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val uid = result.user?.uid ?: throw Exception("Account created but UID missing")

                // Write Firestore user profile
                userRepository.createUserProfile(
                    User(uid = uid, name = name.trim(), email = email.trim())
                )
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(
                    when {
                        e.message?.contains("email address is already") == true ->
                            "An account with this email already exists. Sign in instead?"
                        e.message?.contains("badly formatted") == true ->
                            "Please enter a valid email address."
                        else -> e.message ?: "Sign up failed"
                    }
                )
            }
        }
    }

    // ── Phone Auth ────────────────────────────────────────────────────────────

    /**
     * Triggers Firebase Phone Auth to send an OTP.
     * Dev stub: simulates a 1.5s delay then sets otpSent = true.
     *
     * TODO: Replace stub with real PhoneAuthProvider.verifyPhoneNumber()
     * which requires an Activity reference. Wire it via:
     *   val activity = LocalContext.current as Activity
     *   viewModel.sendOtp(phoneNumber, activity)
     */
    fun sendOtp(phoneNumber: String) {
        val formatted = formatPhoneNumber(phoneNumber)
        if (formatted == null) {
            _loginState.value = LoginState.Error("Enter a valid 10-digit Indian mobile number")
            return
        }
        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            // ── Dev stub — remove when real PhoneAuthProvider is wired ────────
            kotlinx.coroutines.delay(1500)
            verificationId = "DEV_VERIFICATION_ID"
            _otpSent.value = true
            _loginState.value = LoginState.Idle
        }
    }

    /**
     * Verifies the OTP entered by the user.
     * Dev stub: any 6-digit code returns success.
     *
     * TODO: Replace with real credential:
     *   val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
     *   auth.signInWithCredential(credential).await()
     */
    fun verifyOtp(otp: String) {
        if (verificationId == null) {
            _loginState.value = LoginState.Error("Please request an OTP first")
            return
        }
        if (otp.length != 6) {
            _loginState.value = LoginState.Error("OTP must be 6 digits")
            return
        }
        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            // ── Dev stub ─────────────────────────────────────────────────────
            kotlinx.coroutines.delay(1000)
            _loginState.value = LoginState.Success
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun resetOtp() {
        verificationId = null
        _otpSent.value = false
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }

    /**
     * DEV ONLY — skips Firebase Auth entirely.
     * Remove this (and the button in LoginScreen) once Firebase Auth is configured.
     */
    fun devBypass() {
        _loginState.value = LoginState.Success
    }

    private fun formatPhoneNumber(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "+91$digits"
            digits.length == 12 && digits.startsWith("91") -> "+$digits"
            raw.startsWith("+") && digits.length >= 10 -> "+$digits"
            else -> null
        }
    }
}
