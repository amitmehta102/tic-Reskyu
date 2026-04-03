package com.reskyu.consumer.data.repository

import com.reskyu.consumer.data.model.AppNotification
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
        // Dev seed — realistic FCM-style notifications
        val now = System.currentTimeMillis()
        addAll(listOf(
            AppNotification(
                id = "notif_1",
                title = "🍱 New drop near you!",
                body = "The Bread Basket just posted Assorted Pastry Box for ₹99. Only 3 left!",
                timestamp = now - TimeUnit.MINUTES.toMillis(8),
                isRead = false
            ),
            AppNotification(
                id = "notif_2",
                title = "⚡ Last minute – 1 left!",
                body = "Green Leaf Café's Veg Thali expires in 45 min. Grab it for ₹79.",
                timestamp = now - TimeUnit.HOURS.toMillis(1),
                isRead = false
            ),
            AppNotification(
                id = "notif_3",
                title = "✅ Order Confirmed",
                body = "Your claim at Spice Garden is confirmed. Pick up by 8:00 PM!",
                timestamp = now - TimeUnit.HOURS.toMillis(5),
                isRead = true
            ),
            AppNotification(
                id = "notif_4",
                title = "🌍 Your impact this week",
                body = "You've rescued 3 meals and saved 7.5kg of CO₂ this week. Amazing!",
                timestamp = now - TimeUnit.DAYS.toMillis(1),
                isRead = true
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

    /** Clears all notifications. */
    fun clearAll() = _notifications.clear()
}
