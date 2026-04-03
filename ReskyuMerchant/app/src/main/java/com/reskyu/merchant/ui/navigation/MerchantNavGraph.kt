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
import com.reskyu.merchant.ui.post_listing.PostListingScreen
import com.reskyu.merchant.ui.profile.MerchantProfileScreen

/**
 * Root navigation host for the Merchant app.
 *
 * Starts at [Screen.LOGIN]. Dev Mode bypass on the login screen
 * allows navigating straight to DASHBOARD without Firebase auth.
 */
@Composable
fun MerchantNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.LOGIN
    ) {
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
    }
}
