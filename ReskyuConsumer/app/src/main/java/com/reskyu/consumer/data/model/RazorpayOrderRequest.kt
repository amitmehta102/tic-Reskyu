package com.reskyu.consumer.data.model

data class RazorpayOrderRequest(
    val amount: Int = 0,
    val currency: String = "INR",
    val claimId: String = ""
)
