package com.reskyu.consumer.data.model

import com.google.firebase.Timestamp

/**
 * Claim
 *
 * Represents an order/transaction document in `/claims/{claimId}`.
 * Created atomically when a consumer completes checkout — the Firestore
 * transaction both decrements `listing.mealsLeft` and writes this document.
 *
 * Firestore path: /claims/{claimId}
 * Fields:
 *  - id            : Document ID (auto-generated)
 *  - userId        : Firebase Auth UID of the consumer
 *  - merchantId    : UID of the merchant
 *  - listingId     : Reference to the source listing
 *  - businessName  : Denormalized for My Orders UI (avoids extra reads)
 *  - heroItem      : Denormalized for My Orders UI
 *  - paymentId     : Razorpay payment ID (format: pay_XXXXX)
 *  - amount        : Discounted amount paid in INR
 *  - originalPrice : Original price before discount (for savings display)
 *  - timestamp     : When the claim was created
 *  - status        : PENDING_PICKUP | COMPLETED | EXPIRED | DISPUTED
 */
data class Claim(
    val id: String = "",
    val userId: String = "",
    val merchantId: String = "",
    val listingId: String = "",
    val businessName: String = "",
    val heroItem: String = "",
    val paymentId: String = "",
    val amount: Double = 0.0,
    val originalPrice: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now(),
    val status: String = "PENDING_PICKUP"
) {
    /** No-arg constructor required by Firestore deserialization */
    constructor() : this("", "", "", "", "", "", "", 0.0, 0.0, Timestamp.now(), "PENDING_PICKUP")
}
