package com.reskyu.consumer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.reskyu.consumer.MainActivity
import com.reskyu.consumer.NotificationDeepLinkBus
import com.reskyu.consumer.R
import kotlin.random.Random

/**
 * ReskuMessagingService
 *
 * Handles FCM messages in ALL app states:
 *
 *  ┌───────────────────┬───────────────────────────────────────────────────┐
 *  │ App state         │ What happens                                      │
 *  ├───────────────────┼───────────────────────────────────────────────────┤
 *  │ FOREGROUND        │ onMessageReceived() called → show local notif     │
 *  │                   │  + write to Firestore notifications subcollection │
 *  │ BACKGROUND/KILLED │ Android system shows notification automatically   │
 *  │                   │ onMessageReceived() NOT called in this state      │
 *  │                   │ (FCM handles it via notification payload)          │
 *  └───────────────────┴───────────────────────────────────────────────────┘
 *
 * Notification channel is created here and in ReskyuApplication.
 */
class ReskuMessagingService : FirebaseMessagingService() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        const val CHANNEL_ID   = "reskyu_drops"
        const val CHANNEL_NAME = "Food Drops"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title     = message.notification?.title ?: message.data["title"] ?: "Reskyu"
        val body      = message.notification?.body  ?: message.data["body"]  ?: ""
        val type      = message.data["type"] ?: "SYSTEM"
        val listingId = message.data["listingId"]   // may be null for non-drop notifications

        // 1. Show a local push notification (required for FOREGROUND state)
        showLocalNotification(title, body, listingId)

        // 2. Write to Firestore so it appears in the Alerts screen in real-time
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("notifications")
            .add(
                mapOf(
                    "title"     to title,
                    "body"      to body,
                    "type"      to type,
                    "timestamp" to Timestamp.now(),
                    "isRead"    to false
                )
            )
    }

    private fun showLocalNotification(title: String, body: String, listingId: String? = null) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(notificationManager)

        // Carry listingId so MainActivity can open the listing detail screen directly
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!listingId.isNullOrBlank()) {
                putExtra(NotificationDeepLinkBus.EXTRA_LISTING_ID, listingId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)   // uses your app icon
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)                   // dismisses on tap
            .setContentIntent(pendingIntent)
            .build()

        // Use a random ID so multiple notifications stack rather than replace
        notificationManager.notify(Random.nextInt(), notification)
    }

    private fun createNotificationChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nearby food drop alerts from Reskyu"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("fcmToken", token)
    }
}
