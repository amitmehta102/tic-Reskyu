package com.reskyu.consumer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RazorpayPaymentBus {

    private val _result = MutableStateFlow<RazorpayResult>(RazorpayResult.Idle)
    val result: StateFlow<RazorpayResult> = _result.asStateFlow()

    fun emit(result: RazorpayResult) {
        _result.value = result
    }

    fun reset() {
        _result.value = RazorpayResult.Idle
    }
}

sealed class RazorpayResult {
    object Idle : RazorpayResult()
    data class Success(val paymentId: String, val signature: String) : RazorpayResult()
    data class Failure(val reason: String) : RazorpayResult()
}
