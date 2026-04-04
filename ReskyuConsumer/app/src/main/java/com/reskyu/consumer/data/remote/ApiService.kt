package com.reskyu.consumer.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("payment/create-order")
    suspend fun createOrder(@Body body: CreateOrderRequest): CreateOrderResponse

    @POST("payment/verify")
    suspend fun verifyPayment(@Body body: VerifyPaymentRequest): VerifyPaymentResponse
}
