package com.reskyu.merchant.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.reskyu.merchant.ui.components.ReskyuHeader
import com.reskyu.merchant.ui.navigation.Screen
import com.reskyu.merchant.ui.theme.RGreenAccent
import com.reskyu.merchant.ui.theme.RGreenDeep
import com.reskyu.merchant.ui.theme.RScreenBg
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private val GreenDeep   = RGreenDeep
private val GreenAccent = RGreenAccent

/**
 * Order Management screen — tabbed view of PENDING / COMPLETED / DISPUTED claims.
 * Merchants can confirm pickup (manually or via QR scan) or raise a dispute.
 */
@Composable
fun OrderManagementScreen(
    navController: NavController,
    viewModel: OrderManagementViewModel = viewModel()
) {
    val displayedClaims   by viewModel.filteredClaims.collectAsState()
    val allClaimsForBadge by viewModel.allClaims.collectAsState()
    val selectedTab       by viewModel.selectedTab.collectAsState()
    val isLoading         by viewModel.isLoading.collectAsState()
    val error             by viewModel.error.collectAsState()
    val qrScanResult      by viewModel.qrScanResult.collectAsState()

    val merchantId   = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val snackbarHost = remember { SnackbarHostState() }
    val scope        = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.loadClaims(merchantId) }

    // Handle QR scan result from the back stack
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(Unit) {
        savedStateHandle?.getStateFlow<String?>("qr_result", null)
            ?.collect { raw ->
                raw?.let {
                    viewModel.scanAndComplete(it, merchantId)
                    savedStateHandle["qr_result"] = null
                }
            }
    }

    // Show errors as a Snackbar
    LaunchedEffect(error) {
        error?.let {
            scope.launch {
                snackbarHost.showSnackbar(
                    message     = it,
                    actionLabel = "Dismiss",
                    duration    = SnackbarDuration.Long
                )
                viewModel.clearError()
            }
        }
    }

    // Badge counts
    val pendingCount   = remember(allClaimsForBadge) { allClaimsForBadge.count { it.status == "PENDING_PICKUP" } }
    val completedCount = remember(allClaimsForBadge) { allClaimsForBadge.count { it.status == "COMPLETED" } }
    val disputedCount  = remember(allClaimsForBadge) { allClaimsForBadge.count { it.status == "DISPUTED" } }

    // QR scan dialogs
    when (val result = qrScanResult) {
        is QrScanResult.Success -> QrSuccessDialog(
            heroItem  = result.heroItem,
            onDismiss = { viewModel.resetQrResult(); viewModel.selectTab(ClaimTab.COMPLETED) }
        )
        is QrScanResult.Error -> QrErrorDialog(
            message   = result.message,
            onDismiss = { viewModel.resetQrResult() },
            onRetry   = { viewModel.resetQrResult(); navController.navigate(Screen.QR_SCANNER) }
        )
        else -> {}
    }

    // Pager — 3 pages matching the 3 ClaimTabs
    val tabs       = ClaimTab.entries.toList()
    val pagerState = rememberPagerState(
        initialPage = tabs.indexOf(selectedTab).coerceAtLeast(0),
        pageCount   = { tabs.size }
    )

    // Swipe → update ViewModel tab
    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectTab(tabs[pagerState.currentPage])
    }
    // Programmatic tab change (e.g. after QR scan) → animate pager
    LaunchedEffect(selectedTab) {
        val page = tabs.indexOf(selectedTab).coerceAtLeast(0)
        if (pagerState.currentPage != page) pagerState.animateScrollToPage(page)
    }

    Scaffold(
        containerColor      = RScreenBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost        = { SnackbarHost(hostState = snackbarHost) },
        floatingActionButton = {
            if (selectedTab == ClaimTab.PENDING) {
                ExtendedFloatingActionButton(
                    onClick        = { navController.navigate(Screen.QR_SCANNER) },
                    containerColor = Color(0xFF1B4332),
                    contentColor   = Color.White,
                    shape          = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scan QR") },
                    text = { Text("Scan QR", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
                )
            }
        },
        bottomBar = { MainBottomBar(navController = navController, currentRoute = Screen.ORDER_MANAGEMENT) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Pinned header — title + subtitle to match listing screen height ─
            ReskyuHeader(
                title    = "Order Management",
                subtitle = "Track & confirm customer pickups"
            )

            // ── Tab selector — lives BELOW the header on the screen bg ────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE8F5EE))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    Triple(ClaimTab.PENDING,   "Pending",   pendingCount),
                    Triple(ClaimTab.COMPLETED, "Completed", completedCount),
                    Triple(ClaimTab.DISPUTED,  "Disputed",  disputedCount)
                ).forEach { (tab, label, count) ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) GreenAccent else Color.Transparent)
                            .clickable {
                                viewModel.selectTab(tab)
                                scope.launch { pagerState.animateScrollToPage(tabs.indexOf(tab)) }
                            }
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
                                color      = if (isSelected) Color.White else GreenDeep.copy(alpha = 0.6f)
                            )
                            if (count > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSelected) Color.White.copy(alpha = 0.25f)
                                            else GreenAccent.copy(alpha = 0.15f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text       = "$count",
                                        fontSize   = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = if (isSelected) Color.White else GreenDeep
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Swipeable content ─────────────────────────────────────────────
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val tabForPage = tabs[page]
                val claims = remember(allClaimsForBadge, tabForPage) {
                    allClaimsForBadge.filter { c ->
                        when (tabForPage) {
                            ClaimTab.PENDING   -> c.status == "PENDING_PICKUP"
                            ClaimTab.COMPLETED -> c.status == "COMPLETED"
                            ClaimTab.DISPUTED  -> c.status == "DISPUTED"
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!isLoading && claims.isEmpty()) {
                        EmptyOrdersState(tab = tabForPage)
                    } else {
                        LazyColumn(
                            modifier            = Modifier.fillMaxSize(),
                            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(claims, key = { it.id }) { claim ->
                                ClaimCard(
                                    claim      = claim,
                                    onComplete = { viewModel.completeClaim(it) },
                                    onDispute  = { viewModel.disputeClaim(it) }
                                )
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                    LoadingOverlay(isVisible = isLoading)
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

// ── QR result dialogs ─────────────────────────────────────────────────────────

@Composable
private fun QrSuccessDialog(heroItem: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(20.dp),
        containerColor   = Color(0xFF1B4332),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.fillMaxWidth()
            ) {
                Text(text = "✅", fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text       = "Order Completed!",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            }
        },
        text = {
            Text(
                text      = "\"$heroItem\" has been marked as picked up. The order is now in your Completed tab.",
                fontSize  = 14.sp,
                color     = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier       = Modifier.fillMaxWidth(),
                shape          = RoundedCornerShape(12.dp),
                colors         = ButtonDefaults.buttonColors(containerColor = Color(0xFF52B788))
            ) {
                Text(
                    text       = "View Completed Orders",
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White
                )
            }
        }
    )
}

@Composable
private fun QrErrorDialog(message: String, onDismiss: () -> Unit, onRetry: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(20.dp),
        containerColor   = Color(0xFF1A1A2E),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.fillMaxWidth()
            ) {
                Text(text = "❌", fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text       = "Scan Failed",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            }
        },
        text = {
            Text(
                text      = message,
                fontSize  = 14.sp,
                color     = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick  = onRetry,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF52B788))
            ) {
                Text(
                    text       = "Try Again",
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text  = "Dismiss",
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
        }
    )
}
