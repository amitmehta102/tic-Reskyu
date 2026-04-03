package com.reskyu.merchant.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.merchant.data.model.ClaimTab
import com.reskyu.merchant.ui.components.LoadingOverlay
import com.reskyu.merchant.ui.components.MainBottomBar
import com.reskyu.merchant.ui.navigation.Screen

private val GreenDark   = Color(0xFF0C1E13)
private val GreenDeep   = Color(0xFF163823)
private val GreenAccent = Color(0xFF52B788)

/**
 * Order Management screen — tabbed view of PENDING / COMPLETED / DISPUTED claims.
 * Merchants can confirm pickup or raise a dispute on pending orders.
 */
@Composable
fun OrderManagementScreen(
    navController: NavController,
    viewModel: OrderManagementViewModel = viewModel()
) {
    val allClaims   by viewModel.filteredClaims.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isLoading   by viewModel.isLoading.collectAsState()

    val merchantId = "merchant_placeholder"

    LaunchedEffect(Unit) { viewModel.loadClaims(merchantId) }

    // Derive per-tab counts for tab badges
    val pendingCount   = remember(allClaims) { allClaims.count { it.status == "PENDING_PICKUP" } }
    val completedCount = remember(allClaims) { allClaims.count { it.status == "COMPLETED" } }
    val disputedCount  = remember(allClaims) { allClaims.count { it.status == "DISPUTED" } }

    val displayedClaims = remember(allClaims, selectedTab) {
        allClaims.filter { claim ->
            when (selectedTab) {
                ClaimTab.PENDING   -> claim.status == "PENDING_PICKUP"
                ClaimTab.COMPLETED -> claim.status == "COMPLETED"
                ClaimTab.DISPUTED  -> claim.status == "DISPUTED"
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF2F8F4),
        bottomBar = { MainBottomBar(navController = navController, currentRoute = Screen.ORDER_MANAGEMENT) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Header + tabs ─────────────────────────────────────────────────
            OrdersHeader(
                selectedTab    = selectedTab,
                pendingCount   = pendingCount,
                completedCount = completedCount,
                disputedCount  = disputedCount,
                onTabChange    = { viewModel.selectTab(it) }
            )

            // ── Content ───────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                if (!isLoading && displayedClaims.isEmpty()) {
                    EmptyOrdersState(tab = selectedTab)
                } else {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayedClaims, key = { it.id }) { claim ->
                            ClaimCard(
                                claim      = claim,
                                onComplete = { viewModel.completeClaim(it, merchantId) },
                                onDispute  = { viewModel.disputeClaim(it, merchantId) }
                            )
                        }
                    }
                }
                LoadingOverlay(isVisible = isLoading)
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun OrdersHeader(
    selectedTab:    ClaimTab,
    pendingCount:   Int,
    completedCount: Int,
    disputedCount:  Int,
    onTabChange:    (ClaimTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(GreenDark, GreenDeep)))
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text       = "Order Management",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )

            // Custom pill tab row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val tabData = listOf(
                    Triple(ClaimTab.PENDING,   "Pending",   pendingCount),
                    Triple(ClaimTab.COMPLETED, "Completed", completedCount),
                    Triple(ClaimTab.DISPUTED,  "Disputed",  disputedCount)
                )
                tabData.forEach { (tab, label, count) ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable { onTabChange(tab) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text       = label,
                                fontSize   = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color      = if (isSelected) GreenDeep else Color.White.copy(alpha = 0.65f)
                            )
                            if (count > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSelected) GreenAccent.copy(alpha = 0.2f)
                                            else Color.White.copy(alpha = 0.2f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text      = "$count",
                                        fontSize  = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color     = if (isSelected) GreenDeep else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyOrdersState(tab: ClaimTab) {
    val (emoji, title, subtitle) = when (tab) {
        ClaimTab.PENDING   -> Triple("🕐", "No Pending Orders",
            "New customer claims will show up here")
        ClaimTab.COMPLETED -> Triple("✅", "No Completed Orders",
            "Orders confirmed as picked up will appear here")
        ClaimTab.DISPUTED  -> Triple("🎉", "No Disputes",
            "All good — no disputes to resolve")
    }
    Column(
        modifier                = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment     = Alignment.CenterHorizontally,
        verticalArrangement     = Arrangement.Center
    ) {
        Text(text = emoji, fontSize = 60.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text       = title,
            fontSize   = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Color(0xFF374151)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text      = subtitle,
            fontSize  = 14.sp,
            color     = Color(0xFF9CA3AF),
            textAlign = TextAlign.Center
        )
    }
}
