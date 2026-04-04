package com.reskyu.consumer.data.repository

import com.reskyu.consumer.data.model.AppNotification
import com.reskyu.consumer.data.model.NotificationType
import java.util.concurrent.TimeUnit

/**
 * NotificationRepository
 *
 * Manages in-memory storage of push notifications received via FCM.
 *
 * Seeded with dev sample notifications so the Notifications screen
 * shows realistic content before FCM is wired up.
 *
 * TODO: Upgrade to DataStore or SharedPreferences for persistence
 *       across app restarts.
 */
class NotificationRepository {

    private val _notifications = mutableListOf<AppNotification>().apply {
        val now = System.currentTimeMillis()
        addAll(listOf(
            AppNotification(
                id = "notif_1",
                title = "🍱 New drop near you!",
                body = "The Bread Basket just posted Assorted Pastry Box for ₹99. Only 3 left!",
                timestamp = now - TimeUnit.MINUTES.toMillis(8),
                isRead = false,
                type = NotificationType.NEW_DROP
            ),
            AppNotification(
                id = "notif_2",
                title = "⚡ Last chance — 1 left!",
                body = "Green Leaf Café's Veg Thali expires in 45 min. Grab it for ₹79.",
                timestamp = now - TimeUnit.HOURS.toMillis(1),
                isRead = false,
                type = NotificationType.ALERT
            ),
            AppNotification(
                id = "notif_3",
                title = "✅ Order Confirmed",
                body = "Your claim at Spice Garden is confirmed. Show pickup code DEV003 at the counter.",
                timestamp = now - TimeUnit.HOURS.toMillis(5),
                isRead = true,
                type = NotificationType.ORDER
            ),
            AppNotification(
                id = "notif_4",
                title = "🌍 Your impact this week",
                body = "You've rescued 3 meals and saved 7.5 kg of CO₂ this week. Keep it up!",
                timestamp = now - TimeUnit.DAYS.toMillis(1),
                isRead = true,
                type = NotificationType.IMPACT
            ),
            AppNotification(
                id = "notif_5",
                title = "🆕 New nearby restaurants",
                body = "Italiano Express & Jain Sweets just joined Reskyu in your area!",
                timestamp = now - TimeUnit.DAYS.toMillis(2),
                isRead = true,
                type = NotificationType.SYSTEM
            )
        ))
    }

    /** Adds a new notification received from FCM (newest first). */
    fun addNotification(notification: AppNotification) {
        _notifications.add(0, notification)
    }

    /** Returns all notifications, newest first. */
    fun getNotifications(): List<AppNotification> = _notifications.toList()

    /** Marks a notification as read by ID. */
    fun markAsRead(id: String) {
        val index = _notifications.indexOfFirst { it.id == id }
        if (index != -1) {
            _notifications[index] = _notifications[index].copy(isRead = true)
        }
    }

    /** Returns the count of unread notifications (for badge display). */
    fun getUnreadCount(): Int = _notifications.count { !it.isRead }

    /** Permanently removes a notification by ID (swipe-to-dismiss). */
    fun dismiss(id: String) {
        _notifications.removeAll { it.id == id }
    }

    /** Clears all notifications. */
    fun clearAll() = _notifications.clear()
}
