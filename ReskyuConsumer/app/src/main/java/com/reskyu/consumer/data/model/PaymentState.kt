package com.reskyu.consumer.data.model

sealed class PaymentState {
    object Idle : PaymentState()
    object Processing : PaymentState()
    data class Success(val paymentId: String) : PaymentState()
    data class Failed(val reason: String) : PaymentState()
}
