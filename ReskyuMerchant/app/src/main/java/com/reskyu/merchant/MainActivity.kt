package com.reskyu.merchant

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.reskyu.merchant.ui.navigation.MerchantNavGraph
import com.reskyu.merchant.ui.theme.ReskyuMerchantTheme

/**
 * Single Activity that hosts the entire Compose navigation graph.
 * The NavGraph always starts at [Screen.SPLASH]; the splash screen
 * handles auth resolution and navigates to the correct destination.
 *
 * Edge-to-edge is enabled with a fully transparent status bar and white
 * icons by default — this matches the dark gradient headers used on
 * every main screen and prevents icon-colour flicker on startup.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Transparent status bar + white icons from the very first frame.
        // Compose SideEffect in each screen can still override per-screen.
        enableEdgeToEdge(
            statusBarStyle     = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            ReskyuMerchantTheme {
                MerchantNavGraph(navController = rememberNavController())
            }
        }
    }
}