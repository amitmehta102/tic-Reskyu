package com.reskyu.merchant.data.model

/**
 * Sealed class tracking the lifecycle of a posted listing.
 */
sealed class PublishState {
    object Idle : PublishState()
    object Publishing : PublishState()
    data class Live(val listingId: String) : PublishState()
    data class Error(val message: String) : PublishState()
}
