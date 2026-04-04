package com.reskyu.merchant.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.reskyu.merchant.R
import com.reskyu.merchant.data.model.MerchantAuthState
import com.reskyu.merchant.ui.components.DarkStatusBar
import com.reskyu.merchant.ui.navigation.Screen
import kotlinx.coroutines.delay

/**
 * Compose splash screen.
 *
 * Shows a full-screen dark background matching the logo image, with the
 * Reskyu logo fading + scaling in. The status bar is made transparent and
 * its icons are switched to light (white) so they're visible on the dark bg.
 * Waits for Firebase auth state to resolve, then navigates to the correct destination.
 */
@Composable
fun MerchantSplashScreen(
    navController: NavHostController,
    splashViewModel: MerchantSplashViewModel = viewModel()
) {
    val authState by splashViewModel.authState.collectAsStateWithLifecycle()

    // ── Status bar: transparent + white icons for the dark splash background ──
    DarkStatusBar()

    // Animation state
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.75f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    // Guard: only navigate once — prevents re-triggering if authState recomposes
    // or if the StateFlow briefly resets (e.g. WhileSubscribed restart).
    val navigated = remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (!navigated.value && authState !is MerchantAuthState.Loading) {
            navigated.value = true
            delay(MIN_SPLASH_MS)
            val destination = when (authState) {
                is MerchantAuthState.Authenticated   -> Screen.DASHBOARD
                is MerchantAuthState.NeedsOnboarding -> Screen.ONBOARDING
                else                                  -> Screen.LOGIN
            }
            navController.navigate(destination) {
                popUpTo(Screen.SPLASH) { inclusive = true }
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBg)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_reskyu_logo_login),
            contentDescription = "Reskyu logo",
            modifier = Modifier
                .size(260.dp)
                .scale(scale.value)
                .alpha(alpha.value)
        )
    }
}

private const val MIN_SPLASH_MS = 1_400L

/** Matches the near-black background of ic_reskyu_logo_login.png exactly. */
private val SplashBg = Color(0xFF1A1A1A)
