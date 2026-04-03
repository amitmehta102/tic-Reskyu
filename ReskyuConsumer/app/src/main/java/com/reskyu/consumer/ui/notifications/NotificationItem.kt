package com.reskyu.consumer.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reskyu.consumer.data.model.AppNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * NotificationItem
 *
 * A composable representing a single notification row.
 * Unread notifications are highlighted with a subtle background tint.
 *
 * @param notification  The [AppNotification] to display
 * @param onRead        Called when the item is tapped (marks as read)
 */
@Composable
fun NotificationItem(
    notification: AppNotification,
    onRead: () -> Unit
) {
    val backgroundColor = if (!notification.isRead)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    else
        MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRead),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .background(backgroundColor)
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Unread dot indicator
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .offset(y = 6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small
                        )
                )
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (!notification.isRead) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTimestamp(notification.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun formatTimestamp(ms: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(ms))
}
