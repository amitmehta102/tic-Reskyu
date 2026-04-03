package com.reskyu.consumer.ui.navigation

/**
 * Screen
 *
 * Sealed class that defines all navigation route strings for the app.
 * Use [route] as the destination string in NavHost and navigate() calls.
 *
 * Route conventions:
 *  - Static routes:  "home", "orders", "profile"
 *  - Dynamic routes: "detail/{listingId}" — use [DetailListing.createRoute(id)]
 *
 * Usage in NavHost:
 *   composable(Screen.Home.route) { HomeScreen(...) }
 *   composable(Screen.DetailListing.route) { backStackEntry ->
 *       val id = backStackEntry.arguments?.getString("listingId")
 *       ListingDetailScreen(listingId = id)
 *   }
 *
 * Usage for navigation:
 *   navController.navigate(Screen.Home.route)
 *   navController.navigate(Screen.DetailListing.createRoute("listing_789"))
 */
sealed class Screen(val route: String) {

    // ─── Auth Flow ────────────────────────────────────────────────────────────
    object Splash : Screen("splash")
    object Login  : Screen("login")

    // ─── Main Flow ────────────────────────────────────────────────────────────
    object Home   : Screen("home")

    object DetailListing : Screen("detail/{listingId}") {
        fun createRoute(listingId: String) = "detail/$listingId"
    }

    object Claim : Screen("claim/{listingId}") {
        fun createRoute(listingId: String) = "claim/$listingId"
    }

    object Confirmation : Screen("confirmation/{claimId}") {
        fun createRoute(claimId: String) = "confirmation/$claimId"
    }

    object MyOrders       : Screen("orders")
    object Profile        : Screen("profile")
    object Notifications  : Screen("notifications")
}
