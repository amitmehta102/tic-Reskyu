package com.reskyu.consumer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * TabNavigationBus
 *
 * Simple singleton event bus that lets screens outside the bottom-nav shell
 * (e.g. ConfirmationScreen) request a tab switch inside MainScreen's inner NavHost.
 *
 * Usage:
 *   // From any outer screen — set the desired tab route before navigating to Main:
 *   TabNavigationBus.navigateTo(Screen.MyOrders.route)
 *   navController.navigate(Screen.Main.route) { ... }
 *
 *   // Inside MainScreen — observe and consume:
 *   val pending by TabNavigationBus.pendingTab.collectAsState()
 *   LaunchedEffect(pending) {
 *       pending?.let { route ->
 *           TabNavigationBus.consume()
 *           innerNavController.navigate(route) { ... }
 *       }
 *   }
 */
object TabNavigationBus {
    private val _pendingTab = MutableStateFlow<String?>(null)
    val pendingTab: StateFlow<String?> = _pendingTab.asStateFlow()

    fun navigateTo(route: String) { _pendingTab.value = route }
    fun consume()                 { _pendingTab.value = null   }
}
