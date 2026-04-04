package com.reskyu.consumer.data.model

data class TicketUiState(
    val claimId: String = "",
    val businessName: String = "",
    val heroItem: String = "",
    val amount: Double = 0.0,
    val paymentId: String = "",
    val pickupByTime: String = ""
)
