package com.reskyu.consumer.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * RetrofitClient
 *
 * Singleton Retrofit instance for making HTTP calls to your backend
 * (e.g., a Firebase Cloud Function that creates Razorpay orders).
 *
 * Usage:
 *   val api = RetrofitClient.instance.create(YourApiService::class.java)
 *
 * TODO: Replace BASE_URL with your actual Cloud Function or backend URL.
 */
object RetrofitClient {

    // TODO: Replace with your actual backend / Cloud Functions base URL
    private const val BASE_URL = "https://us-central1-YOUR_PROJECT.cloudfunctions.net/"

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
