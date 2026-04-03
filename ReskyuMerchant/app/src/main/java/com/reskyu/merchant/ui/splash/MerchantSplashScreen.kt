package com.reskyu.merchant.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.merchant.R
import com.reskyu.merchant.data.model.MerchantAuthState
import com.reskyu.merchant.ui.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MerchantSplashScreen(
    navController: NavController,
    viewModel: MerchantSplashViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()

    // Animation states
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }

    // State to track if the minimum splash duration has passed
    var isAnimationFinished by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        // Run animations concurrently
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700)
            )
        }

        // Wait for animations to finish plus a small reading buffer
        delay(1200)
        isAnimationFinished = true
    }

    LaunchedEffect(authState, isAnimationFinished) {
        // Only navigate if the animation time has elapsed AND auth state is resolved
        if (isAnimationFinished && authState !is MerchantAuthState.Loading) {
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
                else -> { /* Fallback for Loading, handled by the outer if-condition */ }
            }
        }
    }

    // Splash UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary), // Uses your theme's primary color
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            // Replace with your actual drawable if different
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Reskyu Merchant Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Reskyu Merchant",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}