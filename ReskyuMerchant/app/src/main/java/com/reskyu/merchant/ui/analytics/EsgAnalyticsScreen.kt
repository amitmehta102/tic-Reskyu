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
import com.reskyu.merchant.data.model.EsgStats
import com.reskyu.merchant.ui.components.LoadingOverlay
import com.reskyu.merchant.ui.components.MainBottomBar
import com.reskyu.merchant.ui.navigation.Screen

// ── Brand palette ─────────────────────────────────────────────────────────────
private val GreenDark   = Color(0xFF0C1E13)
private val GreenDeep   = Color(0xFF163823)
private val GreenAccent = Color(0xFF52B788)
private val GreenLight  = Color(0xFF95D5B2)

/**
 * ESG Analytics screen — environmental impact dashboard.
 *
 * Sections:
 *  1. Impact level card (Rookie → Eco Warrior → Champion → Food Hero)
 *  2. 2×2 metric grid (meals rescued, CO₂, food waste, revenue)
 *  3. Weekly meals bar chart (MPAndroidChart)
 */
@Composable
fun EsgAnalyticsScreen(
    navController: NavController,
    viewModel: EsgAnalyticsViewModel = viewModel()
) {
    val stats     by viewModel.esgStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadStats("merchant_placeholder") }

    Scaffold(
        containerColor = Color(0xFFF2F8F4),
        bottomBar = { MainBottomBar(navController = navController, currentRoute = Screen.ESG_ANALYTICS) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // ── Header ────────────────────────────────────────────────────
                item { EsgHeader() }

                // ── Body ──────────────────────────────────────────────────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        // ① Impact level ──────────────────────────────────────
                        ImpactLevelCard(stats = stats)

                        // ② Metric grid ───────────────────────────────────────
                        SectionLabel("Total Impact")

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ImpactMetricCard(
                                emoji       = "🍱",
                                label       = "Meals Rescued",
                                value       = "${stats.totalMealsRescued}",
                                accentColor = Color(0xFF2D6A4F),
                                modifier    = Modifier.weight(1f)
                            )
                            ImpactMetricCard(
                                emoji       = "🌍",
                                label       = "CO₂ Saved",
                                value       = "${stats.co2SavedKg.toInt()} kg",
                                accentColor = Color(0xFF457B9D),
                                modifier    = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ImpactMetricCard(
                                emoji       = "🗑️",
                                label       = "Waste Diverted",
                                value       = "${stats.foodWasteReducedKg.toInt()} kg",
                                accentColor = Color(0xFFE76F51),
                                modifier    = Modifier.weight(1f)
                            )
                            ImpactMetricCard(
                                emoji       = "💰",
                                label       = "Revenue Earned",
                                value       = "₹${stats.totalRevenue.toInt()}",
                                accentColor = GreenAccent,
                                modifier    = Modifier.weight(1f)
                            )
                        }

                        // ③ Weekly chart ──────────────────────────────────────
                        SectionLabel("Weekly Performance")
                        WeeklyBarChart(weeklyData = stats.weeklyData)

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            LoadingOverlay(isVisible = isLoading)
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun EsgHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(GreenDark, GreenDeep)))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text       = "🌱 ESG Impact",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Text(
                text     = "Your environmental contribution",
                fontSize = 13.sp,
                color    = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

// ── Impact Level Card ─────────────────────────────────────────────────────────

private data class ImpactLevel(val emoji: String, val label: String, val desc: String)

private fun getImpactLevel(meals: Int) = when {
    meals >= 200 -> ImpactLevel("⭐", "Food Hero",    "Outstanding community impact!")
    meals >= 50  -> ImpactLevel("🏆", "Champion",     "You're making a real difference!")
    meals >= 10  -> ImpactLevel("🌿", "Eco Warrior",  "Building great habits!")
    else         -> ImpactLevel("🌱", "Rookie",        "Every rescued meal counts!")
}

@Composable
private fun ImpactLevelCard(stats: EsgStats) {
    val level = remember(stats.totalMealsRescued) { getImpactLevel(stats.totalMealsRescued) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF1B4332)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier              = Modifier.padding(20.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = level.emoji, fontSize = 44.sp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text          = "IMPACT LEVEL",
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.SemiBold,
                    color         = GreenLight,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text       = level.label,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White
                )
                Text(
                    text     = level.desc,
                    fontSize = 13.sp,
                    color    = Color.White.copy(alpha = 0.65f)
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

// ── Impact Metric Card ────────────────────────────────────────────────────────

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

// ── Weekly Bar Chart ──────────────────────────────────────────────────────────

private val DAY_LABELS = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
private fun WeeklyBarChart(weeklyData: List<Float>) {
    // Ensure exactly 7 values, defaulting missing entries to 0f
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
                factory = { context ->
                    BarChart(context).apply {
                        applyChartStyle(safeData)
                    }
                },
                update  = { chart -> chart.applyChartStyle(safeData) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            )
        }
    }
}

// Extension to keep chart setup DRY between factory and update
private fun BarChart.applyChartStyle(data: List<Float>) {
    val entries  = data.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }
    val dataSet  = BarDataSet(entries, "").apply {
        color              = AndroidColor.parseColor("#52B788")
        highLightColor     = AndroidColor.parseColor("#2D6A4F")
        setDrawValues(false)
    }
    this.data    = BarData(dataSet).apply { barWidth = 0.55f }

    description.isEnabled = false
    legend.isEnabled      = false
    setDrawGridBackground(false)
    setDrawBorders(false)
    setTouchEnabled(false)
    setScaleEnabled(false)
    setBackgroundColor(AndroidColor.TRANSPARENT)
    extraBottomOffset = 4f

    xAxis.apply {
        position         = XAxis.XAxisPosition.BOTTOM
        setDrawGridLines(false)
        setDrawAxisLine(false)
        granularity      = 1f
        valueFormatter   = IndexAxisValueFormatter(DAY_LABELS)
        textColor        = AndroidColor.parseColor("#9CA3AF")
        textSize         = 10f
    }
    axisLeft.apply {
        setDrawGridLines(true)
        gridColor        = AndroidColor.parseColor("#F3F4F6")
        setDrawAxisLine(false)
        textColor        = AndroidColor.parseColor("#9CA3AF")
        textSize         = 10f
        granularity      = 1f
        axisMinimum      = 0f
    }
    axisRight.isEnabled = false

    invalidate()
}
