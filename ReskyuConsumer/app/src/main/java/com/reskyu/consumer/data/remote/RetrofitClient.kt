package com.reskyu.consumer.data.remote

/**
 * RetrofitClient
 *
 * Singleton Retrofit instance for making HTTP calls to your backend
 * (e.g., a Firebase Cloud Function that creates Razorpay orders).
 *
 * TODO: Uncomment and add Retrofit + Gson dependencies when backend is ready:
 *   implementation("com.squareup.retrofit2:retrofit:2.11.0")
 *   implementation("com.squareup.retrofit2:converter-gson:2.11.0")
 *
 * Usage:
 *   val api = RetrofitClient.instance.create(YourApiService::class.java)
 */
object RetrofitClient {

    // TODO: Replace with your actual Cloud Functions base URL
    private const val BASE_URL = "https://us-central1-YOUR_PROJECT.cloudfunctions.net/"

    // TODO: Uncomment when Retrofit dependency is added
    // val instance: Retrofit by lazy {
    //     Retrofit.Builder()
    //         .baseUrl(BASE_URL)
    //         .addConverterFactory(GsonConverterFactory.create())
    //         .build()
    // }
}
