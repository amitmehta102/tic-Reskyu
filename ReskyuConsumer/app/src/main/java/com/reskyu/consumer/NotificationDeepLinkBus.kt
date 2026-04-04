package com.reskyu.consumer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationDeepLinkBus {

    const val EXTRA_LISTING_ID = "notification_listing_id"

    private val _pendingListingId = MutableStateFlow<String?>(null)
    val pendingListingId: StateFlow<String?> = _pendingListingId.asStateFlow()

    fun postListingId(id: String) {
        _pendingListingId.value = id
    }

    fun consume() {
        _pendingListingId.value = null
    }
}
