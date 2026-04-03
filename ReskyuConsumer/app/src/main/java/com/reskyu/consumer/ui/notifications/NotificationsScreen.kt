package com.reskyu.consumer.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

/**
 * NotificationsScreen
 *
 * Displays the list of push notifications received via FCM.
 * Reads from [NotificationsViewModel] which wraps [NotificationRepository].
 *
 * @param navController  For back navigation
 * @param viewModel      Injected [NotificationsViewModel]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    viewModel: NotificationsViewModel = viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                Text(
                    text = "No notifications yet.\nWe'll alert you when food drops happen near you!",
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notification ->
                    NotificationItem(
                        notification = notification,
                        onRead = { viewModel.markAsRead(notification.id) }
                    )
                }
            }
        }
    }
}
