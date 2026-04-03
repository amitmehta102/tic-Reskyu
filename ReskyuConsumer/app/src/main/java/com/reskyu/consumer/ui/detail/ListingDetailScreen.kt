package com.reskyu.consumer.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.consumer.data.model.DietaryTag
import com.reskyu.consumer.ui.components.DietaryChip
import com.reskyu.consumer.ui.components.ReskyuButton
import com.reskyu.consumer.ui.navigation.Screen

/**
 * ListingDetailScreen
 *
 * Full-detail view of a single food listing. Shown after tapping a [ListingCard].
 * Displays hero image, business info, dietary tag, portions left, pricing,
 * and a "Claim Now" CTA that navigates to the checkout screen.
 *
 * @param listingId     The Firestore document ID of the listing to display
 * @param navController For Back navigation and proceeding to [Screen.Claim]
 * @param viewModel     Injected [ListingDetailViewModel]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(
    listingId: String,
    navController: NavController,
    viewModel: ListingDetailViewModel = viewModel()
) {
    val listing by viewModel.listing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(listingId) {
        viewModel.loadListing(listingId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(listing?.businessName ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading || listing == null) {
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            }
        } else {
            val l = listing!!
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Hero image
                // TODO: Replace Surface with AsyncImage(model = l.imageUrl, ...)
                Surface(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {}

                // Title + dietary chip
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DietaryChip(tag = DietaryTag.valueOf(l.dietaryTag))
                }

                Text(l.heroItem, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(l.businessName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Pricing
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("₹${l.discountedPrice.toInt()}", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("₹${l.originalPrice.toInt()}", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Meals left
                Text("${l.mealsLeft} portions available", style = MaterialTheme.typography.bodyMedium)

                // TODO: Add Gemini AI insight section here (GeminiApiClient.generateListingInsight)

                Spacer(modifier = Modifier.height(16.dp))

                ReskyuButton(
                    text = "Claim Now — ₹${l.discountedPrice.toInt()}",
                    onClick = { navController.navigate(Screen.Claim.createRoute(l.id)) },
                    enabled = l.mealsLeft > 0 && l.status == "OPEN"
                )
            }
        }
    }
}
