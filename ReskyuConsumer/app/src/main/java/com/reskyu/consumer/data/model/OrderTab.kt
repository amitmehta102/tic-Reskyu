package com.reskyu.consumer.data.model

/**
 * OrderTab
 *
 * Enum representing the tab filter options on the My Orders screen.
 *
 * Tabs:
 *  - UPCOMING   : Claims with status PENDING_PICKUP
 *  - COMPLETED  : Claims with status COMPLETED
 *  - EXPIRED    : Claims where the listing has expired and pickup was not done
 */
enum class OrderTab {
    UPCOMING,
    COMPLETED,
    EXPIRED
}
