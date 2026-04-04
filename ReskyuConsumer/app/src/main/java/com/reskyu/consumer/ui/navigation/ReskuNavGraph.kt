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

@Composable
fun ReskuNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }

        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        composable(Screen.Main.route) {
            MainScreen(outerNavController = navController)
        }

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

        composable(
            route = Screen.Claim.route,
            arguments = listOf(
                navArgument(Constants.NAV_ARG_LISTING_ID) { type = NavType.StringType },
                navArgument("quantity") { type = NavType.IntType; defaultValue = 1 }
            )
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments
                ?.getString(Constants.NAV_ARG_LISTING_ID) ?: return@composable
            val quantity = backStackEntry.arguments?.getInt("quantity") ?: 1
            ClaimScreen(listingId = listingId, initialQuantity = quantity, navController = navController)
        }

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
