package com.reskyu.merchant.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.merchant.data.model.DashboardStats
import com.reskyu.merchant.ui.components.LoadingOverlay
import com.reskyu.merchant.ui.components.MainBottomBar
import com.reskyu.merchant.ui.navigation.Screen

// ── Brand palette ─────────────────────────────────────────────────────────────
private val GreenDark    = Color(0xFF0C1E13)
private val GreenDeep    = Color(0xFF163823)
private val GreenMid     = Color(0xFF1F5235)
private val GreenAccent  = Color(0xFF52B788)
private val GreenLight   = Color(0xFF95D5B2)
private val ScreenBg     = Color(0xFFF2F8F4)

/**
 * Main dashboard screen — hero header with live stats + scrollable action cards.
 * Hosts [MainBottomBar] for app-wide navigation.
 */
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel()
) {
    val stats          by viewModel.stats.collectAsState()
    val surplusIqResult by viewModel.surplusIqResult.collectAsState()
    val isLoading      by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDashboard(
            merchantId    = "merchant_placeholder",
            lastPredDate  = "",
            lastPredMeals = 0
        )
    }

    Scaffold(
        containerColor = ScreenBg,
        bottomBar = { MainBottomBar(navController = navController, currentRoute = Screen.DASHBOARD) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // ── Hero header ───────────────────────────────────────────────
                item { DashboardHeader(stats = stats, navController = navController) }

                // ── Body ─────────────────────────────────────────────────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        // SurplusIQ AI Banner (only when prediction is available)
                        surplusIqResult?.let { result ->
                            SurplusIqCard(
                                meals     = result.predictedMeals,
                                reasoning = result.reasoning
                            )
                        }

                        // ── Secondary stats row ───────────────────────────────
                        SectionLabel("Today's Activity")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ActivityChip(
                                emoji = "📦",
                                label = "Active Listings",
                                value = "${stats.activeListings}",
                                accentColor = GreenMid,
                                modifier = Modifier.weight(1f)
                            )
                            ActivityChip(
                                emoji = "🕐",
                                label = "Pending Claims",
                                value = "${stats.pendingClaims}",
                                accentColor = Color(0xFFE76F51),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // ── Quick actions ─────────────────────────────────────
                        SectionLabel("Quick Actions")

                        ActionCard(
                            emoji      = "➕",
                            title      = "Post New Listing",
                            subtitle   = "Create a food-drop for today",
                            accentColor = GreenAccent,
                            onClick    = { navController.navigate(Screen.POST_LISTING) }
                        )
                        ActionCard(
                            emoji      = "📋",
                            title      = "Manage Orders",
                            subtitle   = "Review pending & completed claims",
                            accentColor = Color(0xFF457B9D),
                            onClick    = { navController.navigate(Screen.ORDER_MANAGEMENT) }
                        )
                        ActionCard(
                            emoji      = "🌱",
                            title      = "ESG Impact",
                            subtitle   = "Track your environmental contribution",
                            accentColor = Color(0xFF2D6A4F),
                            onClick    = { navController.navigate(Screen.ESG_ANALYTICS) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            LoadingOverlay(isVisible = isLoading)
        }
    }
}

// ── Hero header ───────────────────────────────────────────────────────────────

@Composable
private fun DashboardHeader(stats: DashboardStats, navController: NavController) {
    val hour = remember {
        java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    }
    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else      -> "Good Evening"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(GreenDark, GreenDeep, GreenMid)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 28.dp)
        ) {
            // ── Top row: greeting + profile icon ─────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text  = "$greeting 👋",
                        fontSize = 13.sp,
                        color = GreenLight,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text       = "Reskyu Merchant",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
                IconButton(
                    onClick = { navController.navigate(Screen.PROFILE) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.AccountCircle,
                        contentDescription = "Profile",
                        tint               = Color.White,
                        modifier           = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Hero stats row ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeroStatTile(
                    emoji    = "🍱",
                    value    = "${stats.totalMealsRescued}",
                    label    = "Meals Rescued",
                    modifier = Modifier.weight(1f)
                )
                HeroStatTile(
                    emoji    = "💰",
                    value    = "₹${stats.totalRevenue.toInt()}",
                    label    = "Revenue",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroStatTile(emoji: String, value: String, label: String, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(18.dp),
        color    = Color.White.copy(alpha = 0.10f)
    ) {
        Column(
            modifier             = Modifier.padding(16.dp),
            horizontalAlignment  = Alignment.CenterHorizontally,
            verticalArrangement  = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = emoji, fontSize = 26.sp)
            Text(
                text       = value,
                fontSize   = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )
            Text(
                text     = label,
                fontSize = 11.sp,
                color    = GreenLight.copy(alpha = 0.85f),
                letterSpacing = 0.3.sp
            )
        }
    }
}

// ── SurplusIQ Card ────────────────────────────────────────────────────────────

@Composable
private fun SurplusIqCard(meals: Int, reasoning: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(18.dp),
        colors   = CardDefaults.cardColors(
            containerColor = Color(0xFF1B4332)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier            = Modifier.padding(18.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = "🤖", fontSize = 32.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "SurplusIQ Prediction",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = GreenLight,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text       = "Prepare $meals meals today",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
                if (reasoning.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text     = reasoning,
                        fontSize = 12.sp,
                        color    = Color.White.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}

// ── Section Label ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text       = text,
        fontSize   = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color      = Color(0xFF6B7280),
        letterSpacing = 0.8.sp,
        modifier   = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

// ── Activity Chip ─────────────────────────────────────────────────────────────

@Composable
private fun ActivityChip(
    emoji: String,
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier              = Modifier.padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 18.sp)
            }
            Column {
                Text(
                    text       = value,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = accentColor
                )
                Text(
                    text     = label,
                    fontSize = 10.sp,
                    color    = Color.Gray
                )
            }
        }
    }
}

// ── Action Card ───────────────────────────────────────────────────────────────

@Composable
private fun ActionCard(
    emoji: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick   = onClick
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = title,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFF111827)
                )
                Text(
                    text     = subtitle,
                    fontSize = 12.sp,
                    color    = Color(0xFF9CA3AF)
                )
            }
            Icon(
                imageVector        = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint               = Color(0xFFD1D5DB),
                modifier           = Modifier.size(20.dp)
            )
        }
    }
}
