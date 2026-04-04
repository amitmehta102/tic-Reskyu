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
 * Start destination is always [Screen.SPLASH].
 * [MerchantSplashScreen] reads Firebase Auth state via [MerchantSplashViewModel]
 * and navigates to the correct destination:
 *  - Authenticated     → DASHBOARD   (skips login)
 *  - NeedsOnboarding   → ONBOARDING
 *  - Unauthenticated   → LOGIN
 *
 * This approach is instant (Firebase reads from local cache) and
 * shows the branded splash during the brief auth check.
 */
@Composable
fun MerchantNavGraph(navController: NavHostController) {
    NavHost(
        navController    = navController,
        startDestination = Screen.SPLASH
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

