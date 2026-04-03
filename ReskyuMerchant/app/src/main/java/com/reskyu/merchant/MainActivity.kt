package com.reskyu.merchant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.reskyu.merchant.ui.navigation.MerchantNavGraph
import com.reskyu.merchant.ui.theme.ReskyuMerchantTheme

/**
 * Single Activity that hosts the entire Compose navigation graph.
 * All navigation is handled declaratively by [MerchantNavGraph].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReskyuMerchantTheme {
                val navController = rememberNavController()
                MerchantNavGraph(navController = navController)
            }
        }
    }
}