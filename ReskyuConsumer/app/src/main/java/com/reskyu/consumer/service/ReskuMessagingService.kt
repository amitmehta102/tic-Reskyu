package com.reskyu.consumer.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.reskyu.consumer.data.model.AppNotification
import com.reskyu.consumer.data.repository.NotificationRepository
import java.util.UUID

/**
 * ReskuMessagingService
 *
 * Firebase Cloud Messaging (FCM) receiver service.
 * Handles two scenarios:
 *  1. [onMessageReceived]  : App is in the FOREGROUND — we handle and display manually
 *  2. [onNewToken]         : FCM token refreshed — send new token to your backend/Firestore
 *
 * Registration in AndroidManifest.xml (required):
 *   <service
 *       android:name=".service.ReskuMessagingService"
 *       android:exported="false">
 *       <intent-filter>
 *           <action android:name="com.google.firebase.MESSAGING_EVENT" />
 *       </intent-filter>
 *   </service>
 *
 * Notification data payload (from Cloud Function or Firestore trigger):
 *   {
 *     "title": "New drop near you!",
 *     "body": "Bhopal Bakery just listed pastries for ₹200",
 *     "deepLink": "detail/listing_789"
 *   }
 */
class ReskuMessagingService : FirebaseMessagingService() {

    // TODO: Use Hilt/DI for proper singleton injection
    private val notificationRepository = NotificationRepository()

    /**
     * Called when an FCM message is received while the app is in the FOREGROUND.
     * We add it to [NotificationRepository] so it appears in the Notifications screen.
     *
     * For background messages, FCM handles display automatically using
     * the notification payload's title/body fields.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "Reskyu"
        val body  = message.notification?.body  ?: message.data["body"] ?: ""
        val deepLink = message.data["deepLink"]

        val notification = AppNotification(
            id = UUID.randomUUID().toString(),
            title = title,
            body = body,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            deepLink = deepLink
        )

        notificationRepository.addNotification(notification)

        // TODO: Show a local notification using NotificationCompat.Builder
        //       so the user sees it even while using the app
    }

    /**
     * Called when the FCM registration token is refreshed.
     * You must save this token to the user's Firestore document so
     * the backend can send targeted push notifications.
     *
     * Firestore path to update: /users/{uid}/fcmToken: "new_token"
     *
     * @param token  The new FCM registration token
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: Save token to Firestore: /users/{uid} → fcmToken: token
        // viewModelScope is not available here; use a WorkManager task or coroutineScope
    }
}
