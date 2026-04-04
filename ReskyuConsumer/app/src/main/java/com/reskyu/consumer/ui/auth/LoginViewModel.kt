package com.reskyu.consumer.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.reskyu.consumer.data.model.LoginState
import com.reskyu.consumer.data.model.User
import com.reskyu.consumer.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.length < 6) {
            _loginState.value = LoginState.Error("Enter a valid email and password (min 6 chars)")
            return
        }
        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
                val uid = result.user?.uid ?: throw Exception("Sign in succeeded but UID missing")

                fetchAndSaveFcmToken(uid)

                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(
                    when {
                        e.message?.contains("no user record")      == true ->
                            "No account found for this email. Sign up instead?"
                        e.message?.contains("password is invalid") == true ->
                            "Incorrect password. Please try again."
                        else -> e.message ?: "Sign in failed"
                    }
                )
            }
        }
    }

    fun signUpWithEmail(
        name: String,
        email: String,
        password: String,
        consumerType: String = "INDIVIDUAL"
    ) {
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

                userRepository.createUserProfile(
                    User(
                        uid          = uid,
                        name         = name.trim(),
                        email        = email.trim(),
                        consumerType = consumerType
                    )
                )

                fetchAndSaveFcmToken(uid)

                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(
                    when {
                        e.message?.contains("email address is already") == true ->
                            "An account with this email already exists. Sign in instead?"
                        e.message?.contains("badly formatted")          == true ->
                            "Please enter a valid email address."
                        else -> e.message ?: "Sign up failed"
                    }
                )
            }
        }
    }

    private suspend fun fetchAndSaveFcmToken(uid: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            userRepository.saveFcmToken(uid, token)
        } catch (_: Exception) {
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }

    fun devBypass() {
        _loginState.value = LoginState.Success
    }
}
