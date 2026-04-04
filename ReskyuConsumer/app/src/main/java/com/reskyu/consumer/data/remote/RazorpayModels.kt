package com.reskyu.consumer.data.remote

import com.google.gson.annotations.SerializedName

// ── Create Order ──────────────────────────────────────────────────────────────

/**
 * Request body for POST /payment/create-order
 *
 * @param amount    Amount in **paise** (INR × 100). e.g., ₹200 → 20000
 * @param currency  Always "INR" for Indian Rupees
 * @param receipt   Unique reference string (e.g., "claim_<uid>_<timestamp>")
 */
data class CreateOrderRequest(
    @SerializedName("amount")   val amount: Int,
    @SerializedName("currency") val currency: String = "INR",
    @SerializedName("receipt")  val receipt: String
)

/**
 * Response from POST /payment/create-order
 * Razorpay order details needed to open the Android checkout sheet.
 */
data class CreateOrderResponse(
    @SerializedName("orderId")  val orderId: String,
    @SerializedName("amount")   val amount: Int,
    @SerializedName("currency") val currency: String
)

// ── Verify Payment ────────────────────────────────────────────────────────────

/**
 * Request body for POST /payment/verify
 * All three fields are returned by Razorpay on payment success.
 */
data class VerifyPaymentRequest(
    @SerializedName("razorpay_order_id")   val orderId: String,
    @SerializedName("razorpay_payment_id") val paymentId: String,
    @SerializedName("razorpay_signature")  val signature: String
)

/**
 * Response from POST /payment/verify
 * @param success  true if the HMAC signature is valid
 * @param message  Human-readable status message
 */
data class VerifyPaymentResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String = ""
)
