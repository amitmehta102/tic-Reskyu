package com.reskyu.consumer.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.consumer.data.model.AuthState
import com.reskyu.consumer.ui.navigation.Screen

/**
 * SplashScreen
 *
 * Shown briefly on launch while [SplashViewModel] determines the auth state.
 * Navigates to [Screen.Login] or [Screen.Home] based on Firebase session.
 *
 * Navigation behavior:
 *  - Pops the Splash destination off the back stack after navigating away
 *    so the user can't press Back to return to it.
 *
 * @param navController  NavController for navigation
 * @param viewModel      Injected SplashViewModel
 */
@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            is AuthState.Unauthenticated -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            null -> { /* Still loading — show splash */ }
        }
    }

    // ── Splash UI ─────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Reskyu",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        // TODO: Replace with animated logo / Lottie animation
    }
}
