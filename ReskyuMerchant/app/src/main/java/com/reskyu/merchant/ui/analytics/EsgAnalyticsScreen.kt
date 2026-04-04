package com.reskyu.merchant.ui.analytics

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.reskyu.merchant.data.model.SurplusIqResult
import com.reskyu.merchant.ui.components.LoadingOverlay
import com.reskyu.merchant.ui.components.MainBottomBar
import com.reskyu.merchant.ui.components.ReskyuHeader
import com.reskyu.merchant.ui.navigation.Screen
import com.reskyu.merchant.ui.theme.RGreenAccent
import com.reskyu.merchant.ui.theme.RGreenLight
import com.reskyu.merchant.ui.theme.RScreenBg
import com.google.firebase.auth.FirebaseAuth

// ── Brand palette ─────────────────────────────────────────────────────────────
private val GreenDark   = Color(0xFF0C1E13)
private val GreenDeep   = Color(0xFF163823)
private val GreenAccent = Color(0xFF52B788)
private val GreenLight  = Color(0xFF95D5B2)

/**
 * ESG Analytics screen.
 *
 * Sections:
 *  1. SurplusIQ card — Loading / Success / Error states, powered by Gemini
 *  2. 2×2 impact metric grid
 *  3. Weekly meals bar chart (MPAndroidChart)
 */
@Composable
fun EsgAnalyticsScreen(
    navController: NavController,
    viewModel: EsgAnalyticsViewModel = viewModel()
) {
    val stats           by viewModel.esgStats.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val isNewRestaurant by viewModel.isNewRestaurant.collectAsState()
    val surplusIq       by viewModel.surplusIq.collectAsState()
    val demand          by viewModel.demand.collectAsState()

    val merchantId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    LaunchedEffect(Unit) { viewModel.loadStats(merchantId) }

    Scaffold(
        containerColor = RScreenBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ReskyuHeader(
                title    = "🌱 ESG Impact",
                subtitle = "Your environmental contribution"
            )
        },
        bottomBar = { MainBottomBar(navController = navController, currentRoute = Screen.ESG_ANALYTICS) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // ── Body ──────────────────────────────────────────────────────
                item {
                    // While loading, show nothing — the LoadingOverlay handles it
                    if (isLoading) return@item

                    if (isNewRestaurant) {
                        // ── New restaurant empty state ─────────────────────────
                        NewRestaurantEmptyState()
                    } else {
                        // ── Real data content ─────────────────────────────────
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // ① SurplusIQ
                            SurplusIqSection(
                                state         = surplusIq,
                                navController = navController,
                                onRetry       = { viewModel.retryPrediction(merchantId) }
                            )

                            // ② Local Demand Intelligence
                            LocalDemandSection(
                                state     = demand,
                                onRefresh = { viewModel.refreshDemand(merchantId) }
                            )

                            // ③ Sell Everything Mode
                            val sellEverythingState by viewModel.sellEverything.collectAsState()
                            SellEverythingSection(
                                state = sellEverythingState,
                                onActivate = { viewModel.requestSellEverythingConfirmation() },
                                onConfirm = { viewModel.confirmAndApplySellEverything(merchantId) },
                                onCancel = { viewModel.cancelSellEverythingConfirmation() },
                                onRetry = { viewModel.retrySellEverything(merchantId) }
                            )

                            // ④ Metric grid
                            SectionLabel("Total Impact")
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ImpactMetricCard("🍱", "Meals Rescued",  "${stats.totalMealsRescued}",              Color(0xFF2D6A4F), Modifier.weight(1f))
                                ImpactMetricCard("🌍", "CO₂ Saved",      "${stats.co2SavedKg.toInt()} kg",          Color(0xFF457B9D), Modifier.weight(1f))
                            }
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ImpactMetricCard("🗑️", "Waste Diverted", "${stats.foodWasteReducedKg.toInt()} kg", Color(0xFFE76F51), Modifier.weight(1f))
                                ImpactMetricCard("💰", "Revenue Earned",  "₹${stats.totalRevenue.toInt()}",         RGreenAccent,       Modifier.weight(1f))
                            }

                            // ③ Weekly meals chart
                            SectionLabel("Weekly Performance")
                            WeeklyBarChart(weeklyData = stats.weeklyData)

                            // ③b Top selling items
                            if (stats.topSellingItems.isNotEmpty()) {
                                SectionLabel("Top Selling Items")
                                TopSellingItemsCard(items = stats.topSellingItems)
                            }

                            // ④ Order Outcome breakdown
                            SectionLabel("Order Outcomes")
                            OrderOutcomeCard(
                                completed = stats.completedOrders,
                                disputed  = stats.disputedOrders
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            LoadingOverlay(isVisible = isLoading)
        }
    }
}

// ── New Restaurant empty state ─────────────────────────────────────────────────

@Composable
private fun NewRestaurantEmptyState() {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = "🌱", fontSize = 56.sp)

            Text(
                text       = "New Restaurant",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF1B4332)
            )

            Text(
                text      = "No sales data yet.\nStart listing surplus food to see your impact analytics here.",
                fontSize  = 14.sp,
                color     = Color(0xFF6B7280),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Small tip chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF52B788).copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text      = "📋  Post a listing → complete an order → unlock insights",
                    fontSize  = 12.sp,
                    color     = Color(0xFF2D6A4F),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

// ── SurplusIQ Section (Loading / Success / Error) ─────────────────────────────

@Composable
private fun SurplusIqSection(
    state:         SurplusIqUiState,
    navController: NavController,
    onRetry:       () -> Unit
) {
    when (state) {
        is SurplusIqUiState.Loading       -> SurplusIqLoading()
        is SurplusIqUiState.Success       -> SurplusIqCard(
                                                result        = state.result,
                                                navController = navController
                                            )
        is SurplusIqUiState.NewRestaurant -> SurplusIqNewRestaurant(state)
        is SurplusIqUiState.Error         -> SurplusIqError(message = state.message, onRetry = onRetry)
    }
}

// Loading state
@Composable
private fun SurplusIqLoading() {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF1B4332)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier              = Modifier.padding(20.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color       = GreenAccent,
                modifier    = Modifier.size(28.dp),
                strokeWidth = 2.5.dp
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text          = "SURPLUSIQ",
                    fontSize      = 10.sp,
                    color         = GreenLight,
                    letterSpacing = 1.5.sp,
                    fontWeight    = FontWeight.Bold
                )
                Text(
                    text       = "Analysing your sales data…",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White
                )

            }
        }
    }
}

// New restaurant — not enough data yet
@Composable
private fun SurplusIqNewRestaurant(state: SurplusIqUiState.NewRestaurant) {
    val progress  = (state.mealsRescued.toFloat() / state.required).coerceIn(0f, 1f)
    val remaining = (state.required - state.mealsRescued).coerceAtLeast(0)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF1B3A2A)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "🌱", fontSize = 24.sp)
                Column {
                    Text(
                        text          = "SURPLUSIQ",
                        fontSize      = 10.sp,
                        color         = GreenLight,
                        letterSpacing = 1.5.sp,
                        fontWeight    = FontWeight.Bold
                    )
                    Text(
                        text       = if (state.mealsRescued == 0)
                                         "AI prediction unlocks soon!"
                                     else
                                         "$remaining more ${if (remaining == 1) "rescue" else "rescues"} to unlock AI!",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White
                    )
                }
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress   = { progress },
                    modifier   = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color      = GreenAccent,
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text     = "${state.mealsRescued} meals rescued",
                        fontSize = 11.sp,
                        color    = GreenLight.copy(alpha = 0.7f)
                    )
                    Text(
                        text     = "Goal: ${state.required}",
                        fontSize = 11.sp,
                        color    = GreenLight.copy(alpha = 0.7f)
                    )
                }
            }

            // Generic tip
            Text(
                text      = if (state.mealsRescued == 0)
                                "Post your first listing to start building your impact story."
                            else
                                "Keep listing daily — Gemini will analyse your trend once you hit ${state.required} rescues.",
                fontSize  = 12.sp,
                color     = Color.White.copy(alpha = 0.55f),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

// Success state
@Composable
private fun SurplusIqCard(result: SurplusIqResult, navController: NavController) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF1B4332)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Header row: icon + label
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "🤖", fontSize = 24.sp)
                Column {
                    Text(
                        text          = "SURPLUSIQ",
                        fontSize      = 10.sp,
                        color         = GreenLight,
                        letterSpacing = 1.5.sp,
                        fontWeight    = FontWeight.Bold
                    )

                }
            }

            // Big prediction number
            Row(
                verticalAlignment     = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text       = "${result.predictedMeals}",
                    fontSize   = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White,
                    lineHeight = 56.sp
                )
                Column(modifier = Modifier.padding(bottom = 6.dp)) {
                    Text(
                        text     = "meals to",
                        fontSize = 14.sp,
                        color    = Color.White.copy(alpha = 0.55f)
                    )
                    Text(
                        text       = "prepare today",
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = GreenLight
                    )
                }
            }

            // Gemini reasoning
            if (result.reasoning.isNotBlank()) {
                Text(
                    text      = "\" ${result.reasoning} \"",
                    color     = Color.White.copy(alpha = 0.7f),
                    fontStyle = FontStyle.Italic
                )
            }

            // ── New insight chips ───────────────────────────────────────────
            val hasInsights = result.bestTimeToList.isNotBlank()
                           || result.pricingHint.isNotBlank()
                           || result.actionTip.isNotBlank()

            if (hasInsights) {
                Spacer(modifier = Modifier.height(2.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (result.bestTimeToList.isNotBlank()) {
                        InsightChip(
                            emoji = "🕐",
                            label = "Best time to list",
                            value = result.bestTimeToList
                        )
                    }
                    if (result.pricingHint.isNotBlank()) {
                        InsightChip(
                            emoji = "💰",
                            label = "Pricing hint",
                            value = result.pricingHint
                        )
                    }
                    if (result.actionTip.isNotBlank()) {
                        InsightChip(
                            emoji = "💡",
                            label = "Today's tip",
                            value = result.actionTip
                        )
                    }
                }
            }

            // ── Create Surplus Listing CTA ────────────────────────────────
            Button(
                onClick = {
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("prefill_meals", result.predictedMeals)
                    navController.navigate(Screen.POST_LISTING)
                },
                modifier       = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape          = RoundedCornerShape(14.dp),
                colors         = ButtonDefaults.buttonColors(
                    containerColor = GreenAccent,
                    contentColor   = Color.White
                )
            ) {
                Text(
                    text       = "🚀  Create Surplus Listing",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            }

            // Confidence bar
            if (result.confidence > 0f) {
                val pct = (result.confidence * 100).toInt()
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text          = "CONFIDENCE",
                            fontSize      = 10.sp,
                            color         = GreenLight,
                            letterSpacing = 1.sp,
                            fontWeight    = FontWeight.SemiBold
                        )
                        Text(
                            text       = "$pct%",
                            fontSize   = 12.sp,
                            color      = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress   = { result.confidence },
                        modifier   = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color      = GreenAccent,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                }
            }
        }
    }
}

// ── Insight chip (used inside SurplusIqCard) ────────────────────────────────

@Composable
private fun InsightChip(emoji: String, label: String, value: String) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(emoji, fontSize = 14.sp, modifier = Modifier.padding(top = 1.dp))
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text          = label.uppercase(),
                fontSize      = 9.sp,
                color         = GreenLight.copy(alpha = 0.7f),
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
            Text(
                text       = value,
                fontSize   = 12.sp,
                color      = Color.White.copy(alpha = 0.88f),
                lineHeight = 16.sp
            )
        }
    }
}

// Error state
@Composable
private fun SurplusIqError(message: String, onRetry: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF1B2A22)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.weight(1f)
            ) {
                Text("🤖", fontSize = 24.sp)
                Column {
                    Text(
                        text          = "SURPLUSIQ",
                        fontSize      = 10.sp,
                        color         = GreenLight,
                        letterSpacing = 1.sp,
                        fontWeight    = FontWeight.Bold
                    )
                    Text(
                        text  = "Prediction unavailable",
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
            }
            TextButton(onClick = onRetry) {
                Text(
                    text       = "Retry",
                    color      = GreenAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Section label ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        fontSize      = 12.sp,
        fontWeight    = FontWeight.SemiBold,
        color         = Color(0xFF6B7280),
        letterSpacing = 0.8.sp,
        modifier      = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

// ── Impact metric card ────────────────────────────────────────────────────────

@Composable
private fun ImpactMetricCard(
    emoji:       String,
    label:       String,
    value:       String,
    accentColor: Color,
    modifier:    Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
            Text(
                text       = value,
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = accentColor
            )
            Text(
                text     = label,
                fontSize = 11.sp,
                color    = Color(0xFF9CA3AF)
            )
        }
    }
}

// ── Weekly bar chart ──────────────────────────────────────────────────────────

private val DAY_LABELS = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
private fun WeeklyBarChart(weeklyData: List<Float>) {
    val safeData = remember(weeklyData) {
        List(7) { i -> weeklyData.getOrElse(i) { 0f } }
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text       = "Meals rescued per day",
                fontSize   = 12.sp,
                color      = Color(0xFF9CA3AF),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            AndroidView(
                factory  = { ctx -> BarChart(ctx).apply { applyChartStyle(safeData) } },
                update   = { chart -> chart.applyChartStyle(safeData) },
                modifier = Modifier.fillMaxWidth().height(190.dp)
            )
        }
    }
}

private fun BarChart.applyChartStyle(data: List<Float>) {
    val maxVal  = data.maxOrNull() ?: 0f
    val entries = data.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }
    val colors  = data.map { v ->
        if (v == maxVal && maxVal > 0f)
            AndroidColor.parseColor("#1B4332")
        else
            AndroidColor.parseColor("#52B788")
    }
    val dataSet = BarDataSet(entries, "").apply {
        setColors(colors)
        highLightColor = AndroidColor.parseColor("#2D6A4F")
        setDrawValues(true)
        valueTextColor = AndroidColor.parseColor("#374151")
        valueTextSize  = 9f
    }
    this.data = BarData(dataSet).apply { barWidth = 0.55f }

    description.isEnabled = false
    legend.isEnabled      = false
    setDrawGridBackground(false)
    setDrawBorders(false)
    setTouchEnabled(false)
    setScaleEnabled(false)
    setBackgroundColor(AndroidColor.TRANSPARENT)
    extraBottomOffset = 4f

    xAxis.apply {
        position       = XAxis.XAxisPosition.BOTTOM
        setDrawGridLines(false)
        setDrawAxisLine(false)
        granularity    = 1f
        valueFormatter = IndexAxisValueFormatter(DAY_LABELS)
        textColor      = AndroidColor.parseColor("#9CA3AF")
        textSize       = 10f
    }
    axisLeft.apply {
        setDrawGridLines(true)
        gridColor      = AndroidColor.parseColor("#F3F4F6")
        setDrawAxisLine(false)
        textColor      = AndroidColor.parseColor("#9CA3AF")
        textSize       = 10f
        granularity    = 1f
        axisMinimum    = 0f
    }
    axisRight.isEnabled = false
    invalidate()
}

// ── Top selling items card ────────────────────────────────────────────────────

@Composable
private fun TopSellingItemsCard(items: Map<String, Int>) {
    val list   = items.entries.toList()
    val maxVal = list.firstOrNull()?.value?.toFloat() ?: 1f

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            list.forEachIndexed { idx, (name, count) ->
                val fraction = count.toFloat() / maxVal
                val isTop    = idx == 0

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text     = when (idx) {
                                    0    -> "🥇"
                                    1    -> "🥈"
                                    2    -> "🥉"
                                    else -> "  ${idx + 1}."
                                },
                                fontSize = if (idx < 3) 16.sp else 13.sp
                            )
                            Text(
                                text       = name,
                                fontSize   = 14.sp,
                                fontWeight = if (isTop) FontWeight.Bold else FontWeight.Normal,
                                color      = if (isTop) Color(0xFF1B4332) else Color(0xFF374151)
                            )
                        }
                        Text(
                            text       = "$count sold",
                            fontWeight = if (isTop) FontWeight.Bold else FontWeight.Medium,
                            color      = if (isTop) Color(0xFF52B788) else Color(0xFF9CA3AF)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFF3F4F6))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isTop) Color(0xFF1B4332)
                                    else       Color(0xFF52B788).copy(alpha = 0.6f)
                                )
                        )
                    }
                }
            }
        }
    }
}

// ── Local Demand Intelligence Section ───────────────────────────────────

@Composable
private fun LocalDemandSection(
    state:     DemandUiState,
    onRefresh: () -> Unit
) {
    SectionLabel("Local Demand Status")
    when (state) {
        is DemandUiState.Idle,
        is DemandUiState.Loading -> LocalDemandLoading()
        is DemandUiState.Success -> LocalDemandCard(result = state.result, onRefresh = onRefresh)
        is DemandUiState.Error   -> LocalDemandError(message = state.message, onRefresh = onRefresh)
    }
}

@Composable
private fun LocalDemandLoading() {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF1B4332)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier              = Modifier.padding(20.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color       = GreenAccent,
                modifier    = Modifier.size(28.dp),
                strokeWidth = 2.5.dp
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text          = "LOCAL DEMAND",
                    fontSize      = 10.sp,
                    color         = GreenLight,
                    letterSpacing = 1.5.sp,
                    fontWeight    = FontWeight.Bold
                )
                Text(
                    text       = "Scanning nearby listings…",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White
                )

            }
        }
    }
}

@Composable
private fun LocalDemandCard(
    result:    com.reskyu.merchant.data.model.DemandResult,
    onRefresh: () -> Unit
) {
    // Colour scheme based on demand level
    val level        = result.demandLevel.uppercase()
    val cardBg       = when (level) {
        "HIGH"   -> Color(0xFF0D2B1A)
        "MEDIUM" -> Color(0xFF1E2910)
        else     -> Color(0xFF1A1E20)
    }
    val levelColor   = when (level) {
        "HIGH"   -> Color(0xFF52B788)
        "MEDIUM" -> Color(0xFFF4A261)
        else     -> Color(0xFF9CA3AF)
    }
    val levelEmoji   = when (level) {
        "HIGH"   -> "🔴"
        "MEDIUM" -> "🟡"
        else     -> "⚪"
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Header row ────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "📡", fontSize = 24.sp)
                    Column {
                        Text(
                            text          = "LOCAL DEMAND",
                            fontSize      = 10.sp,
                            color         = GreenLight,
                            letterSpacing = 1.5.sp,
                            fontWeight    = FontWeight.Bold
                        )

                    }
                }
                TextButton(onClick = onRefresh) {
                    Text(
                        text       = "Refresh",
                        fontSize   = 12.sp,
                        color      = GreenAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── Demand level badge ─────────────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(levelColor.copy(alpha = 0.18f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = levelEmoji, fontSize = 16.sp)
                        Text(
                            text       = level,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = levelColor,
                            letterSpacing = 1.sp
                        )
                    }
                }
                Text(
                    text     = "demand in your area",
                    fontSize = 13.sp,
                    color    = Color.White.copy(alpha = 0.65f)
                )
            }

            // ── Insight chips ────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (result.bestAction.isNotBlank()) {
                    InsightChip(
                        emoji = "🎯",
                        label = "Suggested action",
                        value = result.bestAction
                    )
                }
                if (result.bestListingWindow.isNotBlank()) {
                    InsightChip(
                        emoji = "⏱",
                        label = "Best window",
                        value = result.bestListingWindow
                    )
                }
                if (result.reason.isNotBlank()) {
                    InsightChip(
                        emoji = "💬",
                        label = "Why",
                        value = result.reason
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalDemandError(message: String, onRefresh: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF1A1E20)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.weight(1f)
            ) {
                Text("📡", fontSize = 24.sp)
                Column {
                    Text(
                        text          = "LOCAL DEMAND",
                        fontSize      = 10.sp,
                        color         = GreenLight,
                        letterSpacing = 1.sp,
                        fontWeight    = FontWeight.Bold
                    )
                    Text(
                        text  = "Demand signal unavailable",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 13.sp
                    )
                }
            }
            TextButton(onClick = onRefresh) {
                Text(
                    text       = "Retry",
                    color      = GreenAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Revenue Bar Chart ──────────────────────────────────────────────────────────

@Composable
private fun RevenueBarChart(weeklyRevenue: List<Float>) {
    val safeData = remember(weeklyRevenue) {
        List(7) { i -> weeklyRevenue.getOrElse(i) { 0f } }
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text       = "Revenue earned per day (₹)",
                fontSize   = 12.sp,
                color      = Color(0xFF9CA3AF),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            AndroidView(
                factory = { ctx -> BarChart(ctx).apply { applyRevenueStyle(safeData) } },
                update  = { chart -> chart.applyRevenueStyle(safeData) },
                modifier = Modifier.fillMaxWidth().height(190.dp)
            )
        }
    }
}

private fun BarChart.applyRevenueStyle(data: List<Float>) {
    val entries = data.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }
    val dataSet = BarDataSet(entries, "").apply {
        color          = AndroidColor.parseColor("#F4A261")
        highLightColor = AndroidColor.parseColor("#E76F51")
        setDrawValues(false)
    }
    this.data = BarData(dataSet).apply { barWidth = 0.55f }

    description.isEnabled = false
    legend.isEnabled      = false
    setDrawGridBackground(false)
    setDrawBorders(false)
    setTouchEnabled(false)
    setScaleEnabled(false)
    setBackgroundColor(AndroidColor.TRANSPARENT)
    extraBottomOffset = 4f

    xAxis.apply {
        position       = XAxis.XAxisPosition.BOTTOM
        setDrawGridLines(false)
        setDrawAxisLine(false)
        granularity    = 1f
        valueFormatter = IndexAxisValueFormatter(DAY_LABELS)
        textColor      = AndroidColor.parseColor("#9CA3AF")
        textSize       = 10f
    }
    axisLeft.apply {
        setDrawGridLines(true)
        gridColor      = AndroidColor.parseColor("#F3F4F6")
        setDrawAxisLine(false)
        textColor      = AndroidColor.parseColor("#9CA3AF")
        textSize       = 10f
        axisMinimum    = 0f
    }
    axisRight.isEnabled = false
    invalidate()
}



// ── Order Outcome Breakdown Card ───────────────────────────────────────────────

@Composable
private fun OrderOutcomeCard(completed: Int, disputed: Int) {
    val total   = completed + disputed
    val compPct = if (total > 0) completed.toFloat() / total else 0f
    val dispPct = if (total > 0) disputed.toFloat() / total  else 0f

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Stacked horizontal bar ─────────────────────────────────────────
            if (total > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                ) {
                    if (compPct > 0f) Box(
                        modifier = Modifier
                            .weight(compPct)
                            .fillMaxHeight()
                            .background(Color(0xFF52B788))
                    )
                    if (dispPct > 0f) Box(
                        modifier = Modifier
                            .weight(dispPct)
                            .fillMaxHeight()
                            .background(Color(0xFFEF4444))
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFFF3F4F6))
                )
            }

            // ── Legend rows ───────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Completed
                Row(
                    modifier          = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF52B788))
                    )
                    Column {
                        Text(
                            text       = "$completed",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color(0xFF52B788)
                        )
                        Text(
                            text     = "Completed",
                            fontSize = 11.sp,
                            color    = Color(0xFF9CA3AF)
                        )
                    }
                }
                // Disputed
                Row(
                    modifier          = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFEF4444))
                    )
                    Column {
                        Text(
                            text       = "$disputed",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color(0xFFEF4444)
                        )
                        Text(
                            text     = "Disputed",
                            fontSize = 11.sp,
                            color    = Color(0xFF9CA3AF)
                        )
                    }
                }
            }

            // ── Recovery rate summary line ─────────────────────────────────────
            if (total > 0) {
                val rateStr = "%.0f%%".format(compPct * 100f)
                Text(
                    text     = "✅ $rateStr of all orders completed successfully",
                    fontSize = 12.sp,
                    color    = Color(0xFF6B7280)
                )
            } else {
                Text(
                    text     = "No orders yet — start listing to see outcomes",
                    fontSize = 12.sp,
                    color    = Color(0xFFB0B8C4)
                )
            }
        }
    }
}

// ── Sell Everything Mode Section ─────────────────────────────────────────────

@Composable
private fun SellEverythingSection(
    state: SellEverythingUiState,
    onActivate: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    SectionLabel("Sell Everything Mode")

    when (state) {
        is SellEverythingUiState.Idle -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }
        is SellEverythingUiState.FetchingStrategy -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)), //Amber 100
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFFD97706), modifier = Modifier.size(24.dp))
                    Text("AI is strategising...", color = Color(0xFF92400E), fontWeight = FontWeight.Medium)
                }
            }
        }
        is SellEverythingUiState.NotTriggered -> SellEverythingNotTriggeredCard(state.minutesUntilClose)
        is SellEverythingUiState.NoListings -> SellEverythingNoListingsCard()
        is SellEverythingUiState.Ready -> SellEverythingReadyCard(state, onActivate)
        is SellEverythingUiState.Confirming -> {
            SellEverythingReadyCard(state = SellEverythingUiState.Ready(state.result, state.mealsLeft, state.minutesUntilClose), onActivate = {})
            AlertDialog(
                onDismissRequest = onCancel,
                title = { Text("Activate Sell Everything Mode?") },
                text = { Text("This will apply a ${state.result.discountPercentage}% discount to your active listings and auto-create a ${state.result.bundleMeals}-meal bundle listing priced at ₹${state.result.bundlePrice.toInt()}.") },
                confirmButton = {
                    TextButton(onClick = onConfirm) {
                        Text("Confirm & Apply", color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onCancel) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
        is SellEverythingUiState.Applying -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFFD97706), modifier = Modifier.size(24.dp))
                    Text("Applying discounts & bundles...", color = Color(0xFF92400E), fontWeight = FontWeight.Medium)
                }
            }
        }
        is SellEverythingUiState.Applied -> SellEverythingAppliedCard(state)
        is SellEverythingUiState.Error -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(state.message, color = Color(0xFFB91C1C))
                    TextButton(onClick = onRetry) {
                        Text("Retry", color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SellEverythingNotTriggeredCard(minutes: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                Text("🤫", fontSize = 20.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sell Everything Mode",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                )
                if (minutes < 0) {
                     Text("Set closing time in profile to use.", fontSize = 12.sp, color = Color.Gray)
                } else {
                     Text("Activates 90 mins before closing. ($minutes mins left)", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun SellEverythingNoListingsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
             Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                Text("🤷", fontSize = 20.sp)
            }
            Column {
                Text("Sell Everything Mode", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                Text("No active listings to sell out.", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun SellEverythingReadyCard(state: SellEverythingUiState.Ready, onActivate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)), // Light amber
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                 modifier = Modifier.fillMaxWidth(),
                 horizontalArrangement = Arrangement.SpaceBetween,
                 verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔥", fontSize = 24.sp)
                    Text("SELL EVERYTHING", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFFD97706), letterSpacing = 1.sp)
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFFEF3C7)).padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("${state.minutesUntilClose} mins left", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Text("AI Strategy for remaining ${state.mealsLeft} meals:", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StrategyItem("💸", "Apply ${state.result.discountPercentage}% discount to all active listings")
                StrategyItem("📦", "Create Bundle: ${state.result.bundleMeals} meals for ₹${state.result.bundlePrice.toInt()}")
                if (state.result.pushRequired) {
                    StrategyItem("📣", "Send push: \"${state.result.urgencyMessage}\"")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onActivate,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
            ) {
                Text("Activate Strategy", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun SellEverythingAppliedCard(state: SellEverythingUiState.Applied) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)), // Light emerald
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFD1FAE5)),
                contentAlignment = Alignment.Center
            ) {
                Text("✅", fontSize = 20.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sell Everything Active",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF065F46)
                )
                Text("${state.result.discountPercentage}% discount and bundle applied.", fontSize = 12.sp, color = Color(0xFF059669))
            }
        }
    }
}

@Composable
private fun StrategyItem(emoji: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha=0.6f)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 16.sp)
        Text(text, fontSize = 13.sp, color = Color(0xFF4B5563), fontWeight = FontWeight.Medium)
    }
}
