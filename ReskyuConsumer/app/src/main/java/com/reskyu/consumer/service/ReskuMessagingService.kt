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
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.reskyu.consumer.MainActivity
import com.reskyu.consumer.NotificationDeepLinkBus
import com.reskyu.consumer.R
import kotlin.random.Random

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

        val notifId = Random.nextInt()
        showLocalNotification(title, body, listingId, requestCode = notifId)

        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("notifications")
            .add(
                buildMap {
                    put("title",     title)
                    put("body",      body)
                    put("type",      type)
                    put("timestamp", Timestamp.now())
                    put("isRead",    false)
                    if (!listingId.isNullOrBlank()) put("listingId", listingId)
                }
            )
    }

    private fun showLocalNotification(
        title: String,
        body: String,
        listingId: String? = null,
        requestCode: Int = Random.nextInt()   // unique per notification — fixes PendingIntent overwrite bug
    ) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(notificationManager)

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!listingId.isNullOrBlank()) {
                putExtra(NotificationDeepLinkBus.EXTRA_LISTING_ID, listingId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, requestCode, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(requestCode, notification)
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
        db.collection("users").document(uid)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
    }
}
