package com.reskyu.consumer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.app.Application
import com.reskyu.consumer.service.ReskuMessagingService

/**
 * ReskyuApplication
 *
 * Creates the FCM notification channel on app start.
 * The channel MUST exist before any notification can be displayed on Android 8+.
 * Safe to call multiple times — Android ignores duplicate channel creation.
 */
class ReskyuApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReskuMessagingService.CHANNEL_ID,
                ReskuMessagingService.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nearby food drop alerts from Reskyu"
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
