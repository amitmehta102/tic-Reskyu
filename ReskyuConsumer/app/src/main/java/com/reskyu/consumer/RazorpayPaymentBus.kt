package com.reskyu.consumer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * RazorpayPaymentBus
 *
 * A process-wide singleton that bridges the Razorpay Activity callbacks
 * (in [MainActivity]) with the Compose UI ([ClaimScreen] / [ClaimViewModel]).
 *
 * The Razorpay SDK calls onPaymentSuccess / onPaymentError on the Activity.
 * Since Compose ViewModels and Composables can't directly reference the Activity,
 * we route the result through this shared StateFlow, which ClaimScreen collects.
 *
 * Usage:
 *  - MainActivity.onPaymentSuccess → RazorpayPaymentBus.emit(Success(...))
 *  - ClaimScreen collects RazorpayPaymentBus.result and calls ClaimViewModel methods
 */
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
