package com.reskyu.consumer.data.model

/**
 * PaymentState
 *
 * Sealed class representing the Razorpay payment flow status.
 * Observed by ClaimViewModel to drive the checkout UI and LoadingOverlay.
 *
 * States:
 *  - Idle       : No payment in progress
 *  - Processing : Razorpay SDK is open / payment is being verified
 *  - Success    : Payment confirmed; navigate to ConfirmationScreen
 *  - Failed     : Payment failed or was dismissed; show retry option
 */
sealed class PaymentState {
    object Idle : PaymentState()
    object Processing : PaymentState()
    data class Success(val paymentId: String) : PaymentState()
    data class Failed(val reason: String) : PaymentState()
}
