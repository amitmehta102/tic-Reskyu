package com.reskyu.consumer.data.repository

import com.reskyu.consumer.data.model.AppNotification

/**
 * NotificationRepository
 *
 * Manages local storage and retrieval of push notifications received
 * via Firebase Cloud Messaging (FCM).
 *
 * Storage strategy: In-memory list (simple) or SharedPreferences/DataStore
 * (persistent across app restarts). Upgrade to DataStore for production.
 *
 * Key Methods:
 *  - addNotification()     : Called by ReskuMessagingService when a new FCM message arrives
 *  - getNotifications()    : Returns all stored notifications for the Notifications screen
 *  - markAsRead()          : Marks a notification as read by ID
 *  - clearAll()            : Clears all stored notifications
 */
class NotificationRepository {

    // TODO: Replace with DataStore or SharedPreferences for persistence across restarts
    private val _notifications = mutableListOf<AppNotification>()

    /** Adds a new notification received from FCM to the local store. */
    fun addNotification(notification: AppNotification) {
        _notifications.add(0, notification) // Newest first
    }

    /**
     * Returns all stored notifications, newest first.
     * @return  Immutable snapshot of the notification list
     */
    fun getNotifications(): List<AppNotification> = _notifications.toList()

    /**
     * Marks a specific notification as read.
     * @param id  The notification ID to mark
     */
    fun markAsRead(id: String) {
        val index = _notifications.indexOfFirst { it.id == id }
        if (index != -1) {
            _notifications[index] = _notifications[index].copy(isRead = true)
        }
    }

    /** Returns the count of unread notifications (for badge display). */
    fun getUnreadCount(): Int = _notifications.count { !it.isRead }

    /** Clears all notifications from the local store. */
    fun clearAll() = _notifications.clear()
}
