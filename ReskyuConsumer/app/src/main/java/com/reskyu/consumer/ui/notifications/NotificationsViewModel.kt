package com.reskyu.consumer.ui.notifications

import androidx.lifecycle.ViewModel
import com.reskyu.consumer.data.model.AppNotification
import com.reskyu.consumer.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NotificationsViewModel
 *
 * Exposes notifications from [NotificationRepository] to [NotificationsScreen].
 *
 * In a production app, this list would be populated reactively as FCM messages
 * arrive via [ReskuMessagingService], which calls [NotificationRepository.addNotification].
 *
 * TODO: Wire [ReskuMessagingService] → [NotificationRepository] → this ViewModel
 *       using a shared singleton or Hilt injection.
 */
class NotificationsViewModel : ViewModel() {

    // TODO: Inject via Hilt/DI instead of direct instantiation
    private val notificationRepository = NotificationRepository()

    private val _notifications = MutableStateFlow<List<AppNotification>>(
        notificationRepository.getNotifications()
    )
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    /**
     * Marks a notification as read and refreshes the list.
     * @param id  The notification ID to mark
     */
    fun markAsRead(id: String) {
        notificationRepository.markAsRead(id)
        _notifications.value = notificationRepository.getNotifications()
    }

    /** Returns the count of unread notifications for a badge indicator. */
    fun getUnreadCount(): Int = notificationRepository.getUnreadCount()
}
