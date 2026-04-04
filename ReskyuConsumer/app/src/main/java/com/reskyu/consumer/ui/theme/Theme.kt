package com.reskyu.consumer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ReskyuColorScheme = lightColorScheme(
    primary            = RGreenAccent,      // 0xFF52B788 — CTAs, active states
    onPrimary          = Color.White,
    primaryContainer   = RGreenMid,         // 0xFF1F5235
    onPrimaryContainer = RGreenLight,       // 0xFF95D5B2

    secondary          = RGreenDeep,        // 0xFF163823
    onSecondary        = Color.White,
    secondaryContainer = RDivider,          // 0xFFD4EDDA
    onSecondaryContainer = RTextPrimary,    // 0xFF0C1E13

    background         = RScreenBg,         // 0xFFF2F8F4
    onBackground       = RTextPrimary,      // 0xFF0C1E13

    surface            = RSurface,          // White
    onSurface          = RTextPrimary,      // 0xFF0C1E13
    surfaceVariant     = Color(0xFFF0F4F2),
    onSurfaceVariant   = RTextSub,          // 0xFF6B7280

    outline            = ROutline,          // 0xFFB0CABB

    error              = RError,            // 0xFFD32F2F
    onError            = Color.White,
)

@Composable
fun ReskyuConsumerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ReskyuColorScheme,
        typography  = Typography,
        content     = content
    )
}
