package com.reskyu.merchant.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Sends FCM push notifications to the consumer app via the Render proxy API.
 *
 * Endpoint: POST https://cloudnary-api.onrender.com/notify/new-drop
 *
 * The server receives this payload and forwards an FCM message to all
 * subscribed consumers — bypassing the Firebase Functions paid plan requirement.
 *
 * This call is intentionally fire-and-forget from the ViewModel:
 *   - A failure here does NOT block listing publishing
 *   - Errors are logged but swallowed so UX is never degraded
 */
class NotificationService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val notifyEndpoint = "https://cloudnary-api.onrender.com/notify/new-drop"

    /**
     * Fires a "new drop" push notification to all consumer devices.
     *
     * @param businessName   The merchant's business name (shown in notification title)
     * @param heroItem       The featured item name (shown in notification body)
     * @param discountedPrice The sale price as a string, e.g. "299"
     * @param listingId      The Firestore document ID of the newly created listing
     *
     * This is a suspend function but callers should use
     * `launch { notificationService.notifyNewDrop(...) }` so it doesn't block
     * the publish flow if the Render server is cold-starting.
     */
    suspend fun notifyNewDrop(
        businessName:   String,
        heroItem:       String,
        discountedPrice: String,
        listingId:      String
    ) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("businessName",    businessName)
                put("heroItem",        heroItem)
                put("discountedPrice", discountedPrice)
                put("listingId",       listingId)
            }.toString()

            val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(notifyEndpoint)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                // Log but don't throw — notification failure should never block publish
                if (!response.isSuccessful) {
                    android.util.Log.w(
                        "NotificationService",
                        "notify/new-drop returned HTTP ${response.code}: ${response.message}"
                    )
                }
            }
        } catch (e: Exception) {
            // Network error, cold-start timeout, etc. — swallow silently
            android.util.Log.e("NotificationService", "Failed to send FCM notification: ${e.message}")
        }
    }
}
