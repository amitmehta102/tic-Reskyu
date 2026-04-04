package com.reskyu.consumer.data.model

/**
 * AppNotification
 *
 * Represents a push notification displayed in the Notifications screen.
 *
 * [type] drives the icon & accent color:
 *  - NEW_DROP    : a new food listing appeared nearby
 *  - ORDER       : order status update (confirmed, ready)
 *  - IMPACT      : weekly/monthly impact summary
 *  - ALERT       : urgency / last chance / expiry warning
 *  - SYSTEM      : app updates, announcements
 */
enum class NotificationType { NEW_DROP, ORDER, IMPACT, ALERT, SYSTEM }

data class AppNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: NotificationType = NotificationType.SYSTEM,
    val deepLink: String? = null
)
