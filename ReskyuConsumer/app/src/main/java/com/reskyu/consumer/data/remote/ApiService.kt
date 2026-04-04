package com.reskyu.consumer.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * ApiService
 *
 * Retrofit interface for the Reskyu Node.js backend.
 * Backend runs at BuildConfig.NODE_API_BASE_URL via localtunnel.
 *
 * Routes (update paths here if your Node.js route names differ):
 *  POST /payment/create-order  → create a Razorpay order and return the orderId
 *  POST /payment/verify        → verify HMAC signature to confirm payment authenticity
 */
interface ApiService {

    /**
     * Creates a Razorpay order on the server.
     * The server uses its RAZORPAY_KEY_SECRET to sign the order — the key secret
     * never touches the Android app.
     *
     * @param body  [CreateOrderRequest] with amount (in paise), currency, receipt
     * @return      [CreateOrderResponse] with the Razorpay orderId
     */
    @POST("payment/create-order")
    suspend fun createOrder(@Body body: CreateOrderRequest): CreateOrderResponse

    /**
     * Verifies the HMAC-SHA256 signature returned by Razorpay on payment success.
     * If invalid, the server returns success=false and the claim must NOT be written.
     *
     * @param body  [VerifyPaymentRequest] with orderId, paymentId, signature
     * @return      [VerifyPaymentResponse] with success flag
     */
    @POST("payment/verify")
    suspend fun verifyPayment(@Body body: VerifyPaymentRequest): VerifyPaymentResponse
}
