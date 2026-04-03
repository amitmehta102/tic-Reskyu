package com.reskyu.merchant.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.merchant.ui.components.LoadingOverlay
import com.reskyu.merchant.ui.components.MetricCard
import com.reskyu.merchant.ui.components.SurplusIqBanner
import com.reskyu.merchant.ui.navigation.Screen

/**
 * Main dashboard screen showing stats, the SurplusIQ banner, and quick-action buttons.
 */
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val surplusIqResult by viewModel.surplusIqResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // TODO: Pass real merchantId from a shared auth state holder
    LaunchedEffect(Unit) {
        viewModel.loadDashboard(
            merchantId = "merchant_placeholder",
            lastPredDate = "",
            lastPredMeals = 0
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Dashboard", style = MaterialTheme.typography.headlineMedium)

            surplusIqResult?.let { SurplusIqBanner(result = it) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    label = "Meals Rescued",
                    value = "${stats.totalMealsRescued}",
                    unit = "meals",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Revenue",
                    value = "₹${stats.totalRevenue.toInt()}",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    label = "Pending Claims",
                    value = "${stats.pendingClaims}",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Active Listings",
                    value = "${stats.activeListings}",
                    modifier = Modifier.weight(1f)
                )
            }

            // Quick-action buttons
            Button(
                onClick = { navController.navigate(Screen.POST_LISTING) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("+ Post New Listing") }

            OutlinedButton(
                onClick = { navController.navigate(Screen.ORDER_MANAGEMENT) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("View Orders") }
        }

        LoadingOverlay(isVisible = isLoading)
    }
}
