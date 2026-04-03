package com.reskyu.consumer.data.model

/**
 * TicketUiState
 *
 * UI state for the ConfirmationScreen "ticket" card shown after a
 * successful claim. Bundles all the information needed to render
 * the success ticket without additional Firestore reads.
 *
 * Fields:
 *  - claimId      : The newly created claim document ID
 *  - businessName : Merchant name shown on the ticket
 *  - heroItem     : Food item name shown on the ticket
 *  - amount       : Amount paid in INR
 *  - paymentId    : Razorpay payment reference
 *  - pickupByTime : Human-readable expiry time (e.g., "by 10:00 PM")
 */
data class TicketUiState(
    val claimId: String = "",
    val businessName: String = "",
    val heroItem: String = "",
    val amount: Double = 0.0,
    val paymentId: String = "",
    val pickupByTime: String = ""
)
