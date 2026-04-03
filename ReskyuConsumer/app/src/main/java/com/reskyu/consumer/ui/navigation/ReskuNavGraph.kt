package com.reskyu.consumer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.reskyu.consumer.ui.auth.LoginScreen
import com.reskyu.consumer.ui.claim.ClaimScreen
import com.reskyu.consumer.ui.confirmation.ConfirmationScreen
import com.reskyu.consumer.ui.detail.ListingDetailScreen
import com.reskyu.consumer.ui.main.MainScreen
import com.reskyu.consumer.ui.splash.SplashScreen
import com.reskyu.consumer.utils.Constants

/**
 * ReskuNavGraph
 *
 * Root NavHost — maps the top-level routes to their composables.
 *
 * Flow:
 *   Splash → auth check → Login  OR  Main
 *   Main (bottom nav shell) → [Home | Orders | Notifications | Profile]
 *   Home → DetailListing → Claim → Confirmation  (all outside bottom nav)
 *
 * The bottom-nav destinations (Home, Orders, Notifications, Profile) are
 * nested inside [MainScreen], which has its own inner NavHost.
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

        // ── Auth Flow ─────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }

        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        // ── Main Shell (bottom nav) ────────────────────────────────────────────
        composable(Screen.Main.route) {
            MainScreen(outerNavController = navController)
        }

        // ── Listing Detail ────────────────────────────────────────────────────
        composable(
            route = Screen.DetailListing.route,
            arguments = listOf(
                navArgument(Constants.NAV_ARG_LISTING_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments
                ?.getString(Constants.NAV_ARG_LISTING_ID) ?: return@composable
            ListingDetailScreen(listingId = listingId, navController = navController)
        }

        // ── Claim / Checkout ─────────────────────────────────────────────────
        composable(
            route = Screen.Claim.route,
            arguments = listOf(
                navArgument(Constants.NAV_ARG_LISTING_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments
                ?.getString(Constants.NAV_ARG_LISTING_ID) ?: return@composable
            ClaimScreen(listingId = listingId, navController = navController)
        }

        // ── Confirmation / Ticket ─────────────────────────────────────────────
        composable(
            route = Screen.Confirmation.route,
            arguments = listOf(
                navArgument(Constants.NAV_ARG_CLAIM_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val claimId = backStackEntry.arguments
                ?.getString(Constants.NAV_ARG_CLAIM_ID) ?: return@composable
            ConfirmationScreen(claimId = claimId, navController = navController)
        }
    }
}
