package com.reskyu.consumer.ui.navigation

sealed class Screen(val route: String) {

    object Splash : Screen("splash")
    object Login  : Screen("login")

    object Home   : Screen("home")

    object DetailListing : Screen("detail/{listingId}") {
        fun createRoute(listingId: String) = "detail/$listingId"
    }

    object Claim : Screen("claim/{listingId}?quantity={quantity}") {
        fun createRoute(listingId: String, quantity: Int = 1) = "claim/$listingId?quantity=$quantity"
    }

    object Confirmation : Screen("confirmation/{claimId}") {
        fun createRoute(claimId: String) = "confirmation/$claimId"
    }

    object Main : Screen("main")

    object MyOrders       : Screen("orders")
    object Profile        : Screen("profile")
    object Notifications  : Screen("notifications")
}
