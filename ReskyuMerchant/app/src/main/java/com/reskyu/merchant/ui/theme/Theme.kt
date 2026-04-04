package com.reskyu.merchant.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary   = RGreenAccent,
    secondary = RGreenLight,
    tertiary  = RGreenMid
)

private val LightColorScheme = lightColorScheme(
    primary   = RGreenAccent,
    secondary = RGreenDeep,
    tertiary  = RGreenMid
)

@Composable
fun ReskyuMerchantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled — it would override our branded dark-green status bar
    // with the user's wallpaper colour on Android 12+, making icons invisible.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    // Apply theme-level status bar style once so every screen starts correctly.
    // Individual screens call DarkStatusBar() / LightStatusBar() to override.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Default: transparent bar, dark icons (light background screens).
            // Dark-header screens override with DarkStatusBar() individually.
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}