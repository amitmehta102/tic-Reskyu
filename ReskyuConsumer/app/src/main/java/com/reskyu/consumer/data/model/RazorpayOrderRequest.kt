package com.reskyu.consumer.data.model

/**
 * RazorpayOrderRequest
 *
 * Request body sent to your backend (or Firebase Function) to create
 * a Razorpay order. The backend returns an `order_id` which is then
 * passed to the Razorpay Android SDK to open the payment sheet.
 *
 * Fields:
 *  - amount   : Amount in **paise** (INR × 100). E.g., ₹200 → 20000
 *  - currency : ISO 4217 currency code, defaults to "INR"
 *  - claimId  : Local claim document ID for receipt reference
 */
data class RazorpayOrderRequest(
    val amount: Int = 0,
    val currency: String = "INR",
    val claimId: String = ""
)
