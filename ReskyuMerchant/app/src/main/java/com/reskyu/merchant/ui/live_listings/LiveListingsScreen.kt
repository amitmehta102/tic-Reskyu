package com.reskyu.merchant.ui.live_listings

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
import com.reskyu.merchant.ui.components.LoadingOverlay

/**
 * Screen listing all OPEN / CLOSING listings for the merchant in a LazyColumn.
 */
@Composable
fun LiveListingsScreen(
    navController: NavController,
    viewModel: LiveListingsViewModel = viewModel()
) {
    val listings by viewModel.listings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // TODO: inject real merchantId
    val merchantId = "merchant_placeholder"

    LaunchedEffect(Unit) {
        viewModel.loadListings(merchantId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Live Listings", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(12.dp))

            if (listings.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active listings", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(listings, key = { it.id }) { listing ->
                        LiveListingCard(
                            listing = listing,
                            onCancel = { viewModel.cancelListing(it, merchantId) }
                        )
                    }
                }
            }
        }
        LoadingOverlay(isVisible = isLoading)
    }
}
