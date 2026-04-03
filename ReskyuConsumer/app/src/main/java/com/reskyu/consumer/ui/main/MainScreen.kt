package com.reskyu.consumer.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.reskyu.consumer.ui.home.HomeScreen
import com.reskyu.consumer.ui.notifications.NotificationsScreen
import com.reskyu.consumer.ui.orders.MyOrdersScreen
import com.reskyu.consumer.ui.profile.ProfileScreen
import com.reskyu.consumer.ui.navigation.Screen

/**
 * MainScreen
 *
 * The persistent shell that wraps all bottom-nav destinations:
 *   Home | My Orders | Notifications | Profile
 *
 * Uses a nested NavHost so each tab preserves its own back stack.
 * [outerNavController] is the root NavController used to navigate to
 * screens that live outside the bottom nav (detail, claim, confirmation).
 *
 * @param outerNavController  Root nav controller from MainActivity
 */
@Composable
fun MainScreen(outerNavController: NavController) {
    val innerNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            ReskyuBottomNavBar(navController = innerNavController)
        }
    ) { innerPadding ->
        NavHost(
            navController = innerNavController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    innerNavController = innerNavController,
                    outerNavController = outerNavController
                )
            }
            composable(Screen.MyOrders.route) {
                MyOrdersScreen(navController = innerNavController)
            }
            composable(Screen.Notifications.route) {
                NotificationsScreen(navController = innerNavController)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    navController = innerNavController,
                    outerNavController = outerNavController
                )
            }
        }
    }
}

// ── Bottom Nav Bar ────────────────────────────────────────────────────────────

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home,          "Home",          Icons.Rounded.Home),
    BottomNavItem(Screen.MyOrders,      "Orders",         Icons.Rounded.ShoppingBag),
    BottomNavItem(Screen.Notifications, "Alerts",         Icons.Rounded.Notifications),
    BottomNavItem(Screen.Profile,       "Profile",        Icons.Rounded.Person),
)

@Composable
private fun ReskyuBottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.screen.route) {
                        // Pop back to start so we don't build a huge back stack
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}
