package com.reskyu.consumer.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.consumer.data.model.Claim
import com.reskyu.consumer.data.model.OrderTab

/**
 * MyOrdersScreen — refined version
 *
 * Layout:
 *  ── Header: "My Orders" + subtitle
 *  ── Summary strip: lifetime stats (total orders, total saved)
 *  ── Custom pill tab bar with per-tab counts
 *  ── LazyColumn: OrderCards for the selected tab
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(
    navController: NavController,
    viewModel: MyOrdersViewModel = viewModel()
) {
    val selectedTab    by viewModel.selectedTab.collectAsState()
    val filteredClaims by viewModel.filteredClaims.collectAsState()
    val allClaims      by viewModel.allClaims.collectAsState()
    val isLoading      by viewModel.isLoading.collectAsState()

    // Per-tab counts for badges
    val upcomingCount  = allClaims.count { it.status == "PENDING_PICKUP" }
    val completedCount = allClaims.count { it.status == "COMPLETED" }
    val missedCount    = allClaims.count { it.status != "PENDING_PICKUP" && it.status != "COMPLETED" }

    // Lifetime stats
    val totalSaved     = allClaims.sumOf { (it.originalPrice - it.amount).coerceAtLeast(0.0) }
    val mealsRescued   = allClaims.count { it.status == "COMPLETED" }

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            // ── Header ─────────────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        "My Orders",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Your food rescue history",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Stats strip ────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatChip(
                        icon = Icons.Rounded.LocalDining,
                        label = "$mealsRescued Meals",
                        sublabel = "Rescued",
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        icon = Icons.Rounded.Savings,
                        label = "₹${totalSaved.toInt()}",
                        sublabel = "Saved",
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        icon = Icons.Rounded.Receipt,
                        label = "${allClaims.size} Orders",
                        sublabel = "Total",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Custom Pill Tab Bar ─────────────────────────────────────────────
            item {
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    OrderTabItem(
                        label = "Upcoming",
                        count = upcomingCount,
                        selected = selectedTab == OrderTab.UPCOMING,
                        accentColor = Color(0xFFE65100),
                        onClick = { viewModel.selectTab(OrderTab.UPCOMING) }
                    )
                    OrderTabItem(
                        label = "Completed",
                        count = completedCount,
                        selected = selectedTab == OrderTab.COMPLETED,
                        accentColor = Color(0xFF2E7D32),
                        onClick = { viewModel.selectTab(OrderTab.COMPLETED) }
                    )
                    OrderTabItem(
                        label = "Missed",
                        count = missedCount,
                        selected = selectedTab == OrderTab.EXPIRED,
                        accentColor = Color(0xFF757575),
                        onClick = { viewModel.selectTab(OrderTab.EXPIRED) }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // ── Content ────────────────────────────────────────────────────────
            if (filteredClaims.isEmpty()) {
                item {
                    OrdersEmptyState(
                        tab = selectedTab,
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(32.dp)
                    )
                }
            } else {
                item { Spacer(Modifier.height(8.dp)) }

                items(filteredClaims, key = { it.id }) { claim ->
                    OrderCard(
                        claim = claim,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                    )
                }

                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

// ── Custom Tab Item ────────────────────────────────────────────────────────────

@Composable
private fun OrderTabItem(
    label: String,
    count: Int,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Tab(
        selected = selected,
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (count > 0) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .background(
                            color = if (selected) accentColor else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// ── Stat Chip ─────────────────────────────────────────────────────────────────

@Composable
private fun StatChip(
    icon: ImageVector,
    label: String,
    sublabel: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Empty State ────────────────────────────────────────────────────────────────

@Composable
private fun OrdersEmptyState(tab: OrderTab, modifier: Modifier = Modifier) {
    val (emoji, title, subtitle) = when (tab) {
        OrderTab.UPCOMING  -> Triple("🛍️", "No upcoming orders", "Head to Home and claim a food drop to see it here!")
        OrderTab.COMPLETED -> Triple("🌟", "No completed orders", "Your rescued meals will appear here once you pick them up.")
        OrderTab.EXPIRED   -> Triple("😌", "No missed orders!", "You've been picking up on time. Keep it up!")
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 56.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
