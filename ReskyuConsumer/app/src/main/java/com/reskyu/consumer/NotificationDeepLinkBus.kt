package com.reskyu.consumer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NotificationDeepLinkBus
 *
 * Process-singleton that bridges notification taps → in-app navigation.
 *
 * Flow:
 *  1. User taps an FCM notification (type = NEW_DROP).
 *  2. MainActivity.onCreate() / onNewIntent() reads the listingId from the
 *     intent extra and calls NotificationDeepLinkBus.postListingId(id).
 *  3. HomeScreen collects [pendingListingId] and navigates to the detail screen,
 *     then calls [consume()] to clear the pending navigation.
 *
 * Using a StateFlow means the pending intent survives config changes.
 */
object NotificationDeepLinkBus {

    const val EXTRA_LISTING_ID = "notification_listing_id"

    private val _pendingListingId = MutableStateFlow<String?>(null)
    val pendingListingId: StateFlow<String?> = _pendingListingId.asStateFlow()

    fun postListingId(id: String) {
        _pendingListingId.value = id
    }

    /** Call this AFTER navigation has happened to clear the pending intent. */
    fun consume() {
        _pendingListingId.value = null
    }
}
