package com.reskyu.consumer.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.reskyu.consumer.TabNavigationBus
import com.reskyu.consumer.ui.home.HomeScreen
import com.reskyu.consumer.ui.notifications.NotificationsScreen
import com.reskyu.consumer.ui.orders.MyOrdersScreen
import com.reskyu.consumer.ui.profile.ProfileScreen
import com.reskyu.consumer.ui.navigation.Screen

private val NavBg           = Color(0xFFFFFFFF)   // pure white nav bar
private val NavIndicator    = Color(0xFFB7E4CB)   // active-tab pill (GreenAccent light tint)
private val NavIconActive   = Color(0xFF0C1E13)   // GreenDark — active icon
private val NavIconInactive = Color(0xFF8A9E93)   // muted sage grey (keep)
private val NavLabelActive  = Color(0xFF0C1E13)   // GreenDark
private val NavLabelInactive = Color(0xFF8A9E93)  // muted (keep)

@Composable
fun MainScreen(outerNavController: NavController) {
    val innerNavController = rememberNavController()

    val pendingTab by TabNavigationBus.pendingTab.collectAsState()
    LaunchedEffect(pendingTab) {
        pendingTab?.let { route ->
            TabNavigationBus.consume()
            innerNavController.navigate(route) {
                popUpTo(Screen.Home.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),   // screens handle their own insets
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
                NotificationsScreen(
                    navController      = innerNavController,
                    outerNavController = outerNavController
                )
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

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home,     "Home",    Icons.Rounded.Home),
    BottomNavItem(Screen.MyOrders, "Orders",  Icons.Rounded.ShoppingBag),
    BottomNavItem(Screen.Profile,  "Profile", Icons.Rounded.Person),
)

@Composable
private fun ReskyuBottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = NavBg,
        contentColor   = NavIconActive,
        tonalElevation = 0.dp,
        modifier       = Modifier.navigationBarsPadding()  // fill behind gesture bar
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (selected) return@NavigationBarItem  // already here — do nothing
                    navController.navigate(item.screen.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) NavIconActive else NavIconInactive
                    )
                },
                label = {
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) NavLabelActive else NavLabelInactive
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor        = NavIconActive,
                    unselectedIconColor      = NavIconInactive,
                    selectedTextColor        = NavLabelActive,
                    unselectedTextColor      = NavLabelInactive,
                    indicatorColor           = NavIndicator
                )
            )
        }
    }
}
