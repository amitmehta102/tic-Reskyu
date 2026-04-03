package com.reskyu.consumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.reskyu.consumer.ui.navigation.ReskuNavGraph
import com.reskyu.consumer.ui.theme.ReskyuConsumerTheme

/**
 * MainActivity
 *
 * The single Activity in the app. Hosts the Compose NavHost via [ReskuNavGraph].
 * All navigation happens inside — no fragment transactions, no secondary Activities.
 *
 * Edge-to-edge is enabled so screens can draw under the status bar if needed.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReskyuConsumerTheme {
                val navController = rememberNavController()
                ReskuNavGraph(navController = navController)
            }
        }
    }
}