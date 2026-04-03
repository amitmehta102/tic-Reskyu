package com.reskyu.merchant.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.merchant.data.model.ClaimTab
import com.reskyu.merchant.ui.components.LoadingOverlay

/**
 * Order Management screen with tabbed claim list (Pending / Completed / Disputed).
 */
@Composable
fun OrderManagementScreen(
    navController: NavController,
    viewModel: OrderManagementViewModel = viewModel()
) {
    val allClaims by viewModel.filteredClaims.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val merchantId = "merchant_placeholder" // TODO: inject real merchantId

    LaunchedEffect(Unit) {
        viewModel.loadClaims(merchantId)
    }

    // Filter locally based on selected tab
    val displayedClaims = remember(allClaims, selectedTab) {
        allClaims.filter { claim ->
            when (selectedTab) {
                ClaimTab.PENDING -> claim.status == "PENDING_PICKUP"
                ClaimTab.COMPLETED -> claim.status == "COMPLETED"
                ClaimTab.DISPUTED -> claim.status == "DISPUTED"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Orders",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Tab row
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                ClaimTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.name) }
                    )
                }
            }

            if (displayedClaims.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No ${selectedTab.name.lowercase()} claims")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedClaims, key = { it.id }) { claim ->
                        ClaimCard(
                            claim = claim,
                            onComplete = { viewModel.completeClaim(it, merchantId) },
                            onDispute = { viewModel.disputeClaim(it, merchantId) }
                        )
                    }
                }
            }
        }
        LoadingOverlay(isVisible = isLoading)
    }
}
