package com.reskyu.consumer.data.remote

import com.google.gson.annotations.SerializedName

data class CreateOrderRequest(
    @SerializedName("amount")   val amount: Int,
    @SerializedName("currency") val currency: String = "INR",
    @SerializedName("receipt")  val receipt: String
)

data class CreateOrderResponse(
    @SerializedName("orderId")  val orderId: String,
    @SerializedName("amount")   val amount: Int,
    @SerializedName("currency") val currency: String
)

data class VerifyPaymentRequest(
    @SerializedName("razorpay_order_id")   val orderId: String,
    @SerializedName("razorpay_payment_id") val paymentId: String,
    @SerializedName("razorpay_signature")  val signature: String
)

data class VerifyPaymentResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String = ""
)
