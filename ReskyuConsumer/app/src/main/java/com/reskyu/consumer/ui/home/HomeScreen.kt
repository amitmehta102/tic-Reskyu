package com.reskyu.consumer.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.consumer.ui.navigation.Screen

/**
 * HomeScreen
 *
 * The main consumer-facing screen showing nearby food listings.
 * Layout: TopAppBar → (Optional Map) → LazyColumn of [ListingCard]s
 *
 * TODO: Add Google Maps composable above the LazyColumn for map view.
 * TODO: Implement location permission request before loading listings.
 *
 * @param navController  For navigating to [Screen.DetailListing]
 * @param viewModel      Injected [HomeViewModel]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val listings by viewModel.listings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reskyu — Nearby Drops") },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            // TODO: Add GoogleMap composable here for map view

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else if (error != null) {
                Text(
                    text = "Error: $error",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(listings) { listing ->
                        ListingCard(
                            listing = listing,
                            onClick = {
                                navController.navigate(Screen.DetailListing.createRoute(listing.id))
                            }
                        )
                    }
                }
            }
        }
    }
}
