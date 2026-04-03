package com.reskyu.merchant.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Handles incoming FCM push notifications for the Merchant app.
 *
 * Expected notification types:
 *  - "NEW_CLAIM"    → A consumer has claimed one of the merchant's listings.
 *  - "DISPUTE"      → A consumer has raised a dispute on a claim.
 *
 * Register this service in AndroidManifest.xml with:
 *   <service android:name=".service.MerchantMessagingService"
 *            android:exported="false">
 *     <intent-filter>
 *       <action android:name="com.google.firebase.MESSAGING_EVENT"/>
 *     </intent-filter>
 *   </service>
 */
class MerchantMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val type = remoteMessage.data["type"] ?: return
        val title = remoteMessage.notification?.title ?: "Reskyu Merchant"
        val body = remoteMessage.notification?.body ?: ""

        when (type) {
            "NEW_CLAIM" -> {
                // TODO: Show a local notification with "New Claim Alert" content
                showLocalNotification(title, body)
            }
            "DISPUTE" -> {
                // TODO: Show a local notification indicating a dispute was raised
                showLocalNotification(title, body)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: Send the new FCM token to Firestore /merchants/{uid}/fcmToken
    }

    private fun showLocalNotification(title: String, body: String) {
        // TODO: Build and display a NotificationCompat notification
    }
}
