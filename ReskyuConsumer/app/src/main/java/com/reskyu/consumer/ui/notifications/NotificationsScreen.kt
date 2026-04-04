package com.reskyu.consumer.ui.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBoxValue.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.consumer.data.model.AppNotification
import com.reskyu.consumer.data.model.NotificationType
import com.reskyu.consumer.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ── Alerts screen light-theme palette ─────────────────────────────────────────
private val ALBackground  = Color(0xFFF2F8F4)   // ScreenBg
private val ALAccent      = Color(0xFF52B788)   // GreenAccent
private val ALText        = Color(0xFF0C1E13)   // GreenDark
private val ALTextSub     = Color(0xFF5A7A65)   // muted sage (keep)
private val ALOutline     = Color(0xFFB0CABB)   // soft outline (keep)
private val ALDivider     = Color(0xFFD4EDDA)   // light divider (keep)
private val ALUnreadBg    = Color(0xFFF0FBF3)   // faint mint unread (keep)
private val ALUnreadBar   = Color(0xFF52B788)   // GreenAccent unread bar

/**
 * NotificationsScreen (Alerts) — matches app-wide light mint theme
 *
 * Features:
 *  ── Dark forest-green branded header (matches Home/Orders pattern)
 *  ── Animated unread badge in header
 *  ── Grouped sections: NEW | TODAY | EARLIER with sticky labels
 *  ── Type-aware pastel icon circles per notification
 *  ── Swipe-left to dismiss with red background reveal
 *  ── Unread rows have mint tinted bg + left green accent bar
 *  ── Smart relative timestamps
 *  ── Polished empty state
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationsScreen(
    navController: NavController,          // innerNavController (bottom-nav)
    outerNavController: NavController? = null,  // root nav — used for listing deep links
    viewModel: NotificationsViewModel = viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount = notifications.count { !it.isRead }
    val grouped = remember(notifications) { groupNotifications(notifications) }

    // Wire navigation callback so ViewModel can trigger navigation without holding a NavController
    LaunchedEffect(Unit) {
        viewModel.onNavigateToListing = { listingId ->
            (outerNavController ?: navController).navigate(
                Screen.DetailListing.createRoute(listingId)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ALBackground)
    ) {

        // ── Branded dark header — zIndex ensures it always draws above scrolling rows ──
        AlertsHeader(
            unreadCount   = unreadCount,
            onMarkAllRead = { viewModel.markAllAsRead() }
        )

        // ── Content ───────────────────────────────────────────────────────────────
        if (notifications.isEmpty()) {
            AlertsEmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                grouped.forEach { (groupLabel, items) ->

                    // Sticky section header
                    stickyHeader(key = groupLabel) {
                        AlertGroupHeader(label = groupLabel)
                    }

                    items(items, key = { it.id }) { notification ->
                        SwipeToDismissAlert(
                            notification = notification,
                            onDismiss    = { viewModel.dismiss(notification.id) },
                            modifier     = Modifier.animateItem()
                        ) {
                            AlertRow(
                                notification = notification,
                                onTap = { viewModel.onNotificationTapped(notification) }
                            )
                        }
                        HorizontalDivider(
                            modifier  = Modifier.padding(start = 78.dp, end = 16.dp),
                            color     = ALDivider,
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

// ── Header gradient — matches HomeScreen / MyOrdersScreen ─────────────────────
private val HGradN  = listOf(Color(0xFF0C1E13), Color(0xFF163823), Color(0xFF1F5235))
private val HLightN = Color(0xFF95D5B2)   // GreenLight for subtitle on dark

@Composable
private fun AlertsHeader(
    unreadCount: Int,
    onMarkAllRead: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(2f)                // always renders above scrolling list rows
            .shadow(                   // drop shadow under the rounded shape
                elevation    = 8.dp,
                shape        = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                ambientColor = Color(0x401F5235),
                spotColor    = Color(0x601F5235)
            )
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.verticalGradient(HGradN))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Alerts 🔔",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    // Animated unread badge
                    AnimatedVisibility(
                        visible = unreadCount > 0,
                        enter = fadeIn() + slideInHorizontally(),
                        exit  = fadeOut() + slideOutHorizontally()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.25f), CircleShape)
                                .size(22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$unreadCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Text(
                    if (unreadCount > 0) "$unreadCount unread" else "You're all caught up",
                    style = MaterialTheme.typography.bodySmall,
                    color = HLightN
                )
            }

            // Mark-all-read button — restyled for dark header
            AnimatedVisibility(visible = unreadCount > 0) {
                FilledTonalButton(
                    onClick = onMarkAllRead,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor   = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Rounded.DoneAll, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Mark all read", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ── Section Group Header ───────────────────────────────────────────────────────

@Composable
private fun AlertGroupHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ALBackground)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = ALTextSub,
                letterSpacing = 1.sp
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = ALDivider,
                thickness = 1.dp
            )
        }
    }
}

// ── Swipe-to-Dismiss wrapper ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissAlert(
    notification: AppNotification,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == EndToStart) { onDismiss(); true } else false
        }
    )

    SwipeToDismissBox(
        state                    = dismissState,
        modifier                 = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val committed = dismissState.targetValue == EndToStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (committed) Color(0xFFFFEBEE) else ALBackground
                    )
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (committed) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Dismiss",
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            "Dismiss",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC62828)
                        )
                    }
                }
            }
        }
    ) {
        content()
    }
}

// ── Alert Row ─────────────────────────────────────────────────────────────────

@Composable
private fun AlertRow(
    notification: AppNotification,
    onTap: () -> Unit
) {
    val style = alertStyle(notification.type, notification.isRead)

    // Scale animation on unread dot
    val dotScale by animateFloatAsState(
        targetValue = if (!notification.isRead) 1f else 0f,
        animationSpec = spring(),
        label = "dot_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (!notification.isRead) ALUnreadBg else Color.White)
            .clickable(onClick = onTap)
    ) {
        // Left accent stripe for unread
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(ALUnreadBar)
                    .align(Alignment.CenterStart)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (!notification.isRead) 19.dp else 16.dp)
                .padding(end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {

            // ── Type icon circle ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(style.bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    style.icon,
                    contentDescription = null,
                    tint = style.tintColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // ── Content ─────────────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {

                // Title + timestamp row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        notification.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (!notification.isRead) FontWeight.SemiBold else FontWeight.Normal,
                        color = ALText,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        smartTimestamp(notification.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = ALOutline,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(3.dp))

                // Body text
                Text(
                    notification.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = ALTextSub,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(7.dp))

                // Type pill badge
                Box(
                    modifier = Modifier
                        .background(style.bgColor, RoundedCornerShape(20.dp))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        style.pill,
                        style = MaterialTheme.typography.labelSmall,
                        color = style.tintColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                }
            }

            // Unread dot (animated scale)
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(y = 5.dp)
                    .clip(CircleShape)
                    .background(ALAccent)
                    .graphicsLayer { scaleX = dotScale; scaleY = dotScale }
            )
        }
    }
}

// ── Empty State ────────────────────────────────────────────────────────────────

@Composable
private fun AlertsEmptyState() {
    Box(
        Modifier
            .fillMaxSize()
            .background(ALBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFFD4EDDA), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🔔", fontSize = 46.sp)
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "You're all caught up!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ALText
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "We'll ping you when a food drop lands near you or your order status changes.",
                style = MaterialTheme.typography.bodySmall,
                color = ALTextSub,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// ── Style helper ──────────────────────────────────────────────────────────────

private data class AlertStyle(
    val icon: ImageVector,
    val bgColor: Color,
    val tintColor: Color,
    val pill: String
)

private fun alertStyle(type: NotificationType, isRead: Boolean): AlertStyle {
    // Muted pastel when read, vibrant when unread — both in the same hue family
    return when (type) {
        NotificationType.NEW_DROP -> AlertStyle(
            icon      = Icons.Rounded.LocalDining,
            bgColor   = if (isRead) Color(0xFFF5F5F5) else Color(0xFFFFF8E1),
            tintColor = if (isRead) Color(0xFF9E9E9E) else Color(0xFFF57F17),
            pill      = "New Drop 🍱"
        )
        NotificationType.ALERT -> AlertStyle(
            icon      = Icons.Rounded.Warning,
            bgColor   = if (isRead) Color(0xFFF5F5F5) else Color(0xFFFFEBEE),
            tintColor = if (isRead) Color(0xFF9E9E9E) else Color(0xFFC62828),
            pill      = "Urgent ⚡"
        )
        NotificationType.ORDER -> AlertStyle(
            icon      = Icons.Rounded.CheckCircle,
            bgColor   = if (isRead) Color(0xFFF5F5F5) else Color(0xFFE8F5E9),
            tintColor = if (isRead) Color(0xFF9E9E9E) else Color(0xFF1A9E45),
            pill      = "Order ✅"
        )
        NotificationType.IMPACT -> AlertStyle(
            icon      = Icons.Rounded.Eco,
            bgColor   = if (isRead) Color(0xFFF5F5F5) else Color(0xFFE8F5E9),
            tintColor = if (isRead) Color(0xFF9E9E9E) else Color(0xFF2DC653),
            pill      = "Impact 🌍"
        )
        NotificationType.SYSTEM -> AlertStyle(
            icon      = Icons.Rounded.Info,
            bgColor   = Color(0xFFF5F5F5),
            tintColor = Color(0xFF757575),
            pill      = "Info"
        )
    }
}

// ── Grouping logic ────────────────────────────────────────────────────────────

private fun groupNotifications(
    notifications: List<AppNotification>
): Map<String, List<AppNotification>> {
    val now        = System.currentTimeMillis()
    val dayAgo     = now - TimeUnit.HOURS.toMillis(24)

    val unread  = notifications.filter { !it.isRead }
    val today   = notifications.filter { it.isRead && it.timestamp > dayAgo }
    val earlier = notifications.filter { it.isRead && it.timestamp <= dayAgo }

    return buildMap {
        if (unread.isNotEmpty())  put("NEW",     unread)
        if (today.isNotEmpty())   put("TODAY",   today)
        if (earlier.isNotEmpty()) put("EARLIER", earlier)
    }
}

// ── Timestamp helper ──────────────────────────────────────────────────────────

private fun smartTimestamp(ms: Long): String {
    val age = System.currentTimeMillis() - ms
    return when {
        age < TimeUnit.MINUTES.toMillis(1)  -> "Just now"
        age < TimeUnit.HOURS.toMillis(1)    -> "${TimeUnit.MILLISECONDS.toMinutes(age)}m ago"
        age < TimeUnit.HOURS.toMillis(24)   -> "${TimeUnit.MILLISECONDS.toHours(age)}h ago"
        age < TimeUnit.DAYS.toMillis(2)     -> "Yesterday"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(ms))
    }
}
