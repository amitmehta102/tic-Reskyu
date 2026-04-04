package com.reskyu.consumer.data.model

sealed class AuthState {
    data class Authenticated(val uid: String) : AuthState()
    object Unauthenticated : AuthState()
}
