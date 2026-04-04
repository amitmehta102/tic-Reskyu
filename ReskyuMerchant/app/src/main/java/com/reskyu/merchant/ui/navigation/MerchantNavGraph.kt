package com.reskyu.merchant.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.reskyu.merchant.ui.analytics.EsgAnalyticsScreen
import com.reskyu.merchant.ui.auth.MerchantLoginScreen
import com.reskyu.merchant.ui.dashboard.DashboardScreen
import com.reskyu.merchant.ui.live_listings.LiveListingsScreen
import com.reskyu.merchant.ui.onboarding.OnboardingScreen
import com.reskyu.merchant.ui.orders.OrderManagementScreen
import com.reskyu.merchant.ui.orders.QrScannerScreen
import com.reskyu.merchant.ui.post_listing.PostListingScreen
import com.reskyu.merchant.ui.profile.MerchantProfileScreen
import com.reskyu.merchant.ui.splash.MerchantSplashScreen

/**
 * Root navigation host for the Merchant app.
 *
 * [startDestination] is computed by [MainActivity] from the Firebase auth state
 * that was already resolved while the system splash screen was on screen.
 * This means we jump directly to the correct screen with no intermediate
 * Compose splash required.
 */
@Composable
fun MerchantNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.SPLASH
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination
    ) {
        composable(Screen.SPLASH) {
            MerchantSplashScreen(navController = navController)
        }
        composable(Screen.LOGIN) {
            MerchantLoginScreen(navController = navController)
        }
        composable(Screen.ONBOARDING) {
            OnboardingScreen(navController = navController)
        }
        composable(Screen.DASHBOARD) {
            DashboardScreen(navController = navController)
        }
        composable(Screen.POST_LISTING) {
            PostListingScreen(navController = navController)
        }
        composable(Screen.LIVE_LISTINGS) {
            LiveListingsScreen(navController = navController)
        }
        composable(Screen.ORDER_MANAGEMENT) {
            OrderManagementScreen(navController = navController)
        }
        composable(Screen.ESG_ANALYTICS) {
            EsgAnalyticsScreen(navController = navController)
        }
        composable(Screen.PROFILE) {
            MerchantProfileScreen(navController = navController)
        }
        composable(Screen.QR_SCANNER) {
            QrScannerScreen(
                onScanResult = { rawValue ->
                    // Pass result back to OrderManagementScreen via SavedStateHandle
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("qr_result", rawValue)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
