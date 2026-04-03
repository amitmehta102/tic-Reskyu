package com.reskyu.merchant.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.reskyu.merchant.ui.navigation.Screen
import androidx.compose.material3.Icon

// ── Colors ────────────────────────────────────────────────────────────────────
private val NavDeep       = Color(0xFF163823)
private val NavAccent     = Color(0xFF52B788)
private val NavIndicator  = Color(0xFFDFF2E9)
private val NavUnselected = Color(0xFF9EAEAD)

// ── Nav items definition ──────────────────────────────────────────────────────
private data class NavItem(val route: String, val icon: ImageVector, val label: String)

private val navItems = listOf(
    NavItem(Screen.DASHBOARD,        Icons.Rounded.Home,     "Home"),
    NavItem(Screen.LIVE_LISTINGS,    Icons.Rounded.GridView, "Listings"),
    NavItem(Screen.ORDER_MANAGEMENT, Icons.Rounded.Receipt,  "Orders"),
    NavItem(Screen.ESG_ANALYTICS,    Icons.Rounded.BarChart, "Impact"),
    NavItem(Screen.PROFILE,          Icons.Rounded.Person,   "Profile"),
)

/**
 * Shared Material 3 [NavigationBar] used on all main-tab screens.
 *
 * @param navController  The app [NavController] for destination changes.
 * @param currentRoute   The route of the currently active screen — used to
 *                       highlight the correct tab.
 */
@Composable
fun MainBottomBar(navController: NavController, currentRoute: String) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        navItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            // Pop back to Dashboard so back-stack doesn't grow unboundedly
                            popUpTo(Screen.DASHBOARD) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(imageVector = item.icon, contentDescription = item.label)
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = NavDeep,
                    selectedTextColor   = NavDeep,
                    unselectedIconColor = NavUnselected,
                    unselectedTextColor = NavUnselected,
                    indicatorColor      = NavIndicator
                )
            )
        }
    }
}
