package com.reskyu.merchant.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Call at the top of any Composable that has a DARK background behind the status bar.
 *
 * Effect:
 *  - Status bar background → fully transparent (header draws under it)
 *  - Status bar icons      → WHITE  (visible on dark header)
 *
 * This must be called on every dark-header screen because `enableEdgeToEdge()`
 * in the Activity sets it once, but navigation between screens can reset it.
 */
@Composable
fun DarkStatusBar() {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window     = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)

            // Make the status bar area fully transparent so our header gradient
            // paints seamlessly behind the system bar.
            window.statusBarColor = Color.Transparent.toArgb()

            // White icons — visible on dark / coloured backgrounds.
            controller.isAppearanceLightStatusBars = false

            // Ensure the window draws edge-to-edge (safe to call multiple times).
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }
}

/**
 * Call at the top of any Composable that has a LIGHT / white background behind the status bar.
 *
 * Effect:
 *  - Status bar background → fully transparent
 *  - Status bar icons      → DARK (visible on light background)
 */
@Composable
fun LightStatusBar() {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window     = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)

            window.statusBarColor = Color.Transparent.toArgb()

            // Dark icons — visible on white / light backgrounds.
            controller.isAppearanceLightStatusBars = true

            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }
}
