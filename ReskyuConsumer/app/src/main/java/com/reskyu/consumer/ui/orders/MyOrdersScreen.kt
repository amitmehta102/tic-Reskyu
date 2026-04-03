package com.reskyu.consumer.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.consumer.data.model.OrderTab

/**
 * MyOrdersScreen
 *
 * Displays the consumer's claim history, tabbed by status:
 *  - UPCOMING  → PENDING_PICKUP claims (to be picked up)
 *  - COMPLETED → Successfully picked up
 *  - EXPIRED   → Listing expired before pickup
 *
 * @param navController  For back navigation
 * @param viewModel      Injected [MyOrdersViewModel]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(
    navController: NavController,
    viewModel: MyOrdersViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val filteredClaims by viewModel.filteredClaims.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Orders") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {

            // Tab Row
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                OrderTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.name) }
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredClaims.isEmpty()) {
                        item {
                            Text(
                                text = "No ${selectedTab.name.lowercase()} orders yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    } else {
                        items(filteredClaims) { claim ->
                            OrderCard(claim = claim)
                        }
                    }
                }
            }
        }
    }
}
