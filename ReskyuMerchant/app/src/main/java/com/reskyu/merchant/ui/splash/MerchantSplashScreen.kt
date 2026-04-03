package com.reskyu.merchant.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.merchant.data.model.MerchantAuthState
import com.reskyu.merchant.ui.navigation.Screen

/**
 * Splash screen that resolves auth state and navigates to the correct destination.
 * Shown briefly while [MerchantSplashViewModel] determines login/onboarding state.
 */
@Composable
fun MerchantSplashScreen(
    navController: NavController,
    viewModel: MerchantSplashViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is MerchantAuthState.Authenticated -> {
                navController.navigate(Screen.DASHBOARD) {
                    popUpTo(Screen.SPLASH) { inclusive = true }
                }
            }
            is MerchantAuthState.NeedsOnboarding -> {
                navController.navigate(Screen.ONBOARDING) {
                    popUpTo(Screen.SPLASH) { inclusive = true }
                }
            }
            is MerchantAuthState.Unauthenticated -> {
                navController.navigate(Screen.LOGIN) {
                    popUpTo(Screen.SPLASH) { inclusive = true }
                }
            }
            else -> { /* Loading — stay on splash */ }
        }
    }

    // Splash UI: simple centered progress indicator
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
