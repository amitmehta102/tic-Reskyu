package com.reskyu.consumer.data.model

import com.google.firebase.Timestamp

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
    val status: String = "PENDING_PICKUP",
    val rating: Int = 0,
    val quantity: Int = 1,          // number of portions claimed
    val pickupDeadlineMs: Long = 0  // epoch-ms of computed pickup window end
) {
    constructor() : this("", "", "", "", "", "", "", 0.0, 0.0, Timestamp.now(), "PENDING_PICKUP", 0, 1, 0)
}
