package com.reskyu.consumer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * LoadingOverlay
 *
 * A full-screen semi-transparent overlay with a centered progress spinner.
 * Shown during async operations like checkout (payment processing) and auth.
 *
 * Usage — show conditionally in a Box:
 *   Box {
 *       ScreenContent()
 *       if (isLoading) LoadingOverlay()
 *   }
 */
@Composable
fun LoadingOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}
