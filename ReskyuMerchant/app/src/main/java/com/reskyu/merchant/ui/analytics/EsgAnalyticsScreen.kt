package com.reskyu.merchant.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.reskyu.merchant.ui.components.LoadingOverlay
import com.reskyu.merchant.ui.components.MetricCard

/**
 * ESG Analytics screen showing environmental impact metrics and a weekly bar chart.
 * The chart is rendered using MPAndroidChart wrapped in an [AndroidView].
 */
@Composable
fun EsgAnalyticsScreen(
    navController: NavController,
    viewModel: EsgAnalyticsViewModel = viewModel()
) {
    val stats by viewModel.esgStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadStats("merchant_placeholder") // TODO: real merchantId
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("ESG Impact", style = MaterialTheme.typography.headlineMedium)

            // Impact metrics grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    label = "Meals Rescued",
                    value = "${stats.totalMealsRescued}",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "CO₂ Saved",
                    value = "%.1f".format(stats.co2SavedKg),
                    unit = "kg",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    label = "Food Waste ↓",
                    value = "%.1f".format(stats.foodWasteReducedKg),
                    unit = "kg",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Revenue",
                    value = "₹${stats.totalRevenue.toInt()}",
                    modifier = Modifier.weight(1f)
                )
            }

            // Weekly bar chart via MPAndroidChart
            Text("Weekly Meals Rescued", style = MaterialTheme.typography.titleMedium)
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                factory = { context ->
                    BarChart(context).apply {
                        description.isEnabled = false
                        legend.isEnabled = false
                    }
                },
                update = { chart ->
                    val entries = stats.weeklyData.mapIndexed { index, value ->
                        BarEntry(index.toFloat(), value)
                    }
                    val dataSet = BarDataSet(entries, "Meals").apply {
                        // TODO: set colors from MaterialTheme
                    }
                    chart.data = BarData(dataSet)
                    chart.invalidate()
                }
            )
        }

        LoadingOverlay(isVisible = isLoading)
    }
}
