package com.reskyu.consumer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.reskyu.consumer.utils.Constants

/**
 * ReskuNavGraph
 *
 * The single NavHost that maps every [Screen] route to its composable.
 * Hosted inside MainActivity — this is the only NavHost in the app.
 *
 * Navigation flow:
 *   Splash → (auth check) → Login OR Home
 *   Home → DetailListing → Claim → Confirmation
 *   Bottom Nav: Home | MyOrders | Notifications | Profile
 *
 * @param navController  The NavHostController created in MainActivity
 * @param startDestination  Determined by SplashViewModel auth check
 */
@Composable
fun ReskuNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        // ── Splash ────────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            // TODO: SplashScreen(navController = navController)
        }

        // ── Auth ──────────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            // TODO: LoginScreen(navController = navController)
        }

        // ── Home ──────────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            // TODO: HomeScreen(navController = navController)
        }

        // ── Listing Detail ────────────────────────────────────────────────────
        composable(
            route = Screen.DetailListing.route,
            // arguments = listOf(navArgument(Constants.NAV_ARG_LISTING_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString(Constants.NAV_ARG_LISTING_ID) ?: return@composable
            // TODO: ListingDetailScreen(listingId = listingId, navController = navController)
        }

        // ── Claim / Checkout ─────────────────────────────────────────────────
        composable(
            route = Screen.Claim.route,
            // arguments = listOf(navArgument(Constants.NAV_ARG_LISTING_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString(Constants.NAV_ARG_LISTING_ID) ?: return@composable
            // TODO: ClaimScreen(listingId = listingId, navController = navController)
        }

        // ── Confirmation / Ticket ─────────────────────────────────────────────
        composable(
            route = Screen.Confirmation.route,
            // arguments = listOf(navArgument(Constants.NAV_ARG_CLAIM_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val claimId = backStackEntry.arguments?.getString(Constants.NAV_ARG_CLAIM_ID) ?: return@composable
            // TODO: ConfirmationScreen(claimId = claimId, navController = navController)
        }

        // ── My Orders ─────────────────────────────────────────────────────────
        composable(Screen.MyOrders.route) {
            // TODO: MyOrdersScreen(navController = navController)
        }

        // ── Profile ───────────────────────────────────────────────────────────
        composable(Screen.Profile.route) {
            // TODO: ProfileScreen(navController = navController)
        }

        // ── Notifications ─────────────────────────────────────────────────────
        composable(Screen.Notifications.route) {
            // TODO: NotificationsScreen(navController = navController)
        }
    }
}
