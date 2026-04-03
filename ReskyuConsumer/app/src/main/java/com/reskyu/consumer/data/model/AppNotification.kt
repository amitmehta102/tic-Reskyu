package com.reskyu.consumer.data.model

/**
 * AppNotification
 *
 * Represents a push notification displayed in the Notifications screen.
 * Stored locally (e.g., SharedPreferences or in-memory list) after being
 * received via Firebase Cloud Messaging (FCM).
 *
 * Fields:
 *  - id        : Unique identifier for the notification
 *  - title     : Short headline (e.g., "New drop near you!")
 *  - body      : Full notification body text
 *  - timestamp : Unix epoch ms when the notification was received
 *  - isRead    : Whether the user has viewed it
 *  - deepLink  : Optional route to navigate to on tap (e.g., "detail/listing_789")
 */
data class AppNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val deepLink: String? = null
)
