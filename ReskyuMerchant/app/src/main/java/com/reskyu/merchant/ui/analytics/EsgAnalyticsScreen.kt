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
import com.reskyu.merchant.ui.navigation.Screen
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
    val stats      by viewModel.esgStats.collectAsState()
    val isLoading  by viewModel.isLoading.collectAsState()
    val surplusIq  by viewModel.surplusIq.collectAsState()

    val merchantId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    LaunchedEffect(Unit) { viewModel.loadStats(merchantId) }

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

                        // ① SurplusIQ ─────────────────────────────────────────
                        SurplusIqSection(
                            state    = surplusIq,
                            onRetry  = { viewModel.retryPrediction(merchantId) }
                        )

                        // ② Metric grid ───────────────────────────────────────
                        SectionLabel("Total Impact")

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ImpactMetricCard("🍱", "Meals Rescued",   "${stats.totalMealsRescued}",                   Color(0xFF2D6A4F), Modifier.weight(1f))
                            ImpactMetricCard("🌍", "CO₂ Saved",       "${stats.co2SavedKg.toInt()} kg",               Color(0xFF457B9D), Modifier.weight(1f))
                        }
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ImpactMetricCard("🗑️", "Waste Diverted",  "${stats.foodWasteReducedKg.toInt()} kg",       Color(0xFFE76F51), Modifier.weight(1f))
                            ImpactMetricCard("💰", "Revenue Earned",   "₹${stats.totalRevenue.toInt()}",              GreenAccent,       Modifier.weight(1f))
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

// ── ESG Header ────────────────────────────────────────────────────────────────

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

// ── SurplusIQ Section (Loading / Success / Error) ─────────────────────────────

@Composable
private fun SurplusIqSection(
    state:   SurplusIqUiState,
    onRetry: () -> Unit
) {
    when (state) {
        is SurplusIqUiState.Loading -> SurplusIqLoading()
        is SurplusIqUiState.Success -> SurplusIqCard(result = state.result)
        is SurplusIqUiState.Error   -> SurplusIqError(message = state.message, onRetry = onRetry)
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
                Text(
                    text     = "Powered by Gemini 2.0 Flash",
                    fontSize = 11.sp,
                    color    = Color.White.copy(alpha = 0.45f)
                )
            }
        }
    }
}

// Success state
@Composable
private fun SurplusIqCard(result: SurplusIqResult) {
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
                    Text(
                        text     = "Powered by Gemini 2.0 Flash",
                        fontSize = 11.sp,
                        color    = Color.White.copy(alpha = 0.45f)
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
                    fontSize  = 13.sp,
                    color     = Color.White.copy(alpha = 0.7f),
                    fontStyle = FontStyle.Italic
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
                        progress     = { result.confidence },
                        modifier     = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color        = GreenAccent,
                        trackColor   = Color.White.copy(alpha = 0.15f)
                    )
                }
            }
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
                        text     = "Prediction unavailable",
                        fontSize = 13.sp,
                        color    = Color.White.copy(alpha = 0.55f)
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
    val entries = data.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }
    val dataSet = BarDataSet(entries, "").apply {
        color          = AndroidColor.parseColor("#52B788")
        highLightColor = AndroidColor.parseColor("#2D6A4F")
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
        granularity    = 1f
        axisMinimum    = 0f
    }
    axisRight.isEnabled = false
    invalidate()
}
