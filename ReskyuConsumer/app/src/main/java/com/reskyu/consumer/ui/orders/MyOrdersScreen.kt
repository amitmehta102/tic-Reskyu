package com.reskyu.consumer.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

// ── Light theme palette for Orders (matches screenshot) ────────────────────
private val LBackground  = Color(0xFFEBF7EE)   // very light mint (screenshot bg)
private val LSurface     = Color.White
private val LAccent      = Color(0xFF2DC653)   // Reskyu green
private val LOnAccent    = Color.White
private val LText        = Color(0xFF133922)   // dark forest green (screenshot header)
private val LTextSub     = Color(0xFF6B7280)   // grey subtitle
private val LDivider     = Color(0xFFD4EDDA)   // light green divider
private val LChipSel     = Color(0xFF2DC653)   // selected tab fill
private val LChipUnsel   = Color(0xFFF0F4F2)   // unselected tab
private val LIconTint    = Color(0xFF2DC653)

/**
 * MyOrdersScreen — clean light theme
 * 2 tabs: Current (PENDING_PICKUP) and Past (everything else)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(
    navController: NavController,
    viewModel: MyOrdersViewModel = viewModel()
) {
    val allClaims by viewModel.allClaims.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }   // 0=Current, 1=Past

    val currentOrders = remember(allClaims) { allClaims.filter { it.status == "PENDING_PICKUP" } }
    val pastOrders    = remember(allClaims) { allClaims.filter { it.status != "PENDING_PICKUP" } }
    val displayedOrders = if (selectedTab == 0) currentOrders else pastOrders

    val totalSaved   = allClaims.sumOf { (it.originalPrice - it.amount).coerceAtLeast(0.0) }
    val mealsRescued = allClaims.count { it.status == "COMPLETED" }

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh    = { viewModel.refresh() },
        modifier     = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(LBackground)
        ) {

            // ── Clean white header ──────────────────────────────────────────────
            item {
                Surface(
                    modifier       = Modifier.fillMaxWidth(),
                    color          = LSurface,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Text(
                            "My Orders",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = LText
                        )
                        Text(
                            "Your food rescue history",
                            style = MaterialTheme.typography.bodySmall,
                            color = LTextSub
                        )
                    }
                }
            }

            // ── 3 stat chips ────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LightStatChip(
                        icon     = Icons.Rounded.LocalDining,
                        value    = "$mealsRescued",
                        label    = "Rescued",
                        modifier = Modifier.weight(1f)
                    )
                    LightStatChip(
                        icon     = Icons.Rounded.Savings,
                        value    = "₹${totalSaved.toInt()}",
                        label    = "Saved",
                        modifier = Modifier.weight(1f)
                    )
                    LightStatChip(
                        icon     = Icons.Rounded.Receipt,
                        value    = "${allClaims.size}",
                        label    = "Total",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Current / Past pill tabs ─────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LightPillTab(
                        label    = "Current",
                        count    = currentOrders.size,
                        selected = selectedTab == 0,
                        onClick  = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    LightPillTab(
                        label    = "Past",
                        count    = pastOrders.size,
                        selected = selectedTab == 1,
                        onClick  = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Content ─────────────────────────────────────────────────────────
            if (displayedOrders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (selectedTab == 0) "🛍️" else "🌟", fontSize = 52.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (selectedTab == 0) "No current orders" else "No past orders yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = LText,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (selectedTab == 0) "Claim a food drop from Home!"
                                else "Your rescued meals will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LTextSub,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                item { Spacer(Modifier.height(4.dp)) }
                items(displayedOrders, key = { it.id }) { claim ->
                    OrderCard(
                        claim    = claim,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                    )
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

// ── Light Pill Tab ─────────────────────────────────────────────────────────────

@Composable
private fun LightPillTab(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick         = onClick,
        modifier        = modifier,
        shape           = RoundedCornerShape(12.dp),
        color           = if (selected) LChipSel else LChipUnsel,
        shadowElevation = if (selected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 11.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) LOnAccent else LTextSub
            )
            if (count > 0) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(
                            color = if (selected) Color.White.copy(alpha = 0.28f) else LDivider,
                            shape = CircleShape
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) LOnAccent else LTextSub,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// ── Light Stat Chip ────────────────────────────────────────────────────────────

@Composable
private fun LightStatChip(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier        = modifier,
        shape           = RoundedCornerShape(14.dp),
        color           = LSurface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = LIconTint)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = LText)
            Text(label, style = MaterialTheme.typography.labelSmall, color = LTextSub)
        }
    }
}
