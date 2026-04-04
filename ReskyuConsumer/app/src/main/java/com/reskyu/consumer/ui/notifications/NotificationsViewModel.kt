package com.reskyu.consumer.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.consumer.data.model.AppNotification
import com.reskyu.consumer.data.repository.AuthRepository
import com.reskyu.consumer.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * NotificationsViewModel
 *
 * Subscribes to the real-time Firestore notification subcollection for the
 * current user at /users/{uid}/notifications.
 * Falls back to dev sample data when not authenticated.
 */
class NotificationsViewModel : ViewModel() {

    private val authRepository         = AuthRepository()
    private val notificationRepository = NotificationRepository()

    private val isAuthenticated: Boolean
        get() = try { authRepository.requireUid(); true } catch (_: Exception) { false }

    // Start empty for authenticated users; show dev samples only in preview/dev mode
    private val _notifications = MutableStateFlow<List<AppNotification>>(
        if (isAuthenticated) emptyList() else notificationRepository.devSampleNotifications()
    )
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    /** Callback set by the screen to navigate to a listing without passing navController into ViewModel */
    var onNavigateToListing: ((String) -> Unit)? = null

    init { subscribeToNotifications() }

    private fun subscribeToNotifications() {
        val uid = try { authRepository.requireUid() } catch (_: Exception) { return }

        viewModelScope.launch {
            notificationRepository
                .observeNotifications(uid)
                .catch { _notifications.value = emptyList() }
                .collect { notifs -> _notifications.value = notifs }
        }
    }

    /** Marks a single notification as read in Firestore.
     *  If the notification has a deepLink (listingId), also triggers navigation. */
    fun onNotificationTapped(notification: AppNotification) {
        markAsRead(notification.id)
        notification.deepLink?.takeIf { it.isNotBlank() }?.let { listingId ->
            onNavigateToListing?.invoke(listingId)
        }
    }

    /** Marks a single notification as read in Firestore. */
    fun markAsRead(id: String) {
        val uid = try { authRepository.requireUid() } catch (_: Exception) {
            // Dev mode — just update local state
            _notifications.value = _notifications.value.map {
                if (it.id == id) it.copy(isRead = true) else it
            }
            return
        }
        viewModelScope.launch {
            try { notificationRepository.markAsRead(uid, id) } catch (_: Exception) {}
        }
    }

    /** Marks all unread notifications as read. */
    fun markAllAsRead() {
        val uid = try { authRepository.requireUid() } catch (_: Exception) {
            _notifications.value = _notifications.value.map { it.copy(isRead = true) }
            return
        }
        viewModelScope.launch {
            _notifications.value
                .filter { !it.isRead }
                .forEach { try { notificationRepository.markAsRead(uid, it.id) } catch (_: Exception) {} }
        }
    }

    /** Deletes a notification (swipe-to-dismiss). */
    fun dismiss(id: String) {
        val uid = try { authRepository.requireUid() } catch (_: Exception) {
            _notifications.value = _notifications.value.filter { it.id != id }
            return
        }
        viewModelScope.launch {
            try { notificationRepository.dismiss(uid, id) } catch (_: Exception) {}
        }
    }

    fun getUnreadCount(): Int = _notifications.value.count { !it.isRead }
}
