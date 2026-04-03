package com.reskyu.merchant.ui.post_listing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.merchant.data.model.DietaryTag
import com.reskyu.merchant.data.model.PublishState
import com.reskyu.merchant.ui.components.LoadingOverlay
import com.reskyu.merchant.ui.navigation.Screen

/**
 * Screen for creating / posting a new food-drop listing.
 * Includes Camera/Gallery image picker, form fields, and publish action.
 */
@Composable
fun PostListingScreen(
    navController: NavController,
    viewModel: PostListingViewModel = viewModel()
) {
    val form by viewModel.form.collectAsState()
    val publishState by viewModel.publishState.collectAsState()

    LaunchedEffect(publishState) {
        if (publishState is PublishState.Live) {
            navController.navigate(Screen.LIVE_LISTINGS) {
                popUpTo(Screen.POST_LISTING) { inclusive = true }
            }
            viewModel.resetPublishState()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Post Listing", style = MaterialTheme.typography.headlineMedium)

            // Image picker area
            // TODO: Add ActivityResultLauncher for Camera & Gallery
            OutlinedButton(
                onClick = { /* Launch image picker */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (form.imageUrl.isNotEmpty()) "Image Selected ✓" else "📷 Select Photo")
            }

            OutlinedTextField(
                value = form.heroItem,
                onValueChange = { viewModel.updateForm { copy(heroItem = it) } },
                label = { Text("Hero Item (e.g. Assorted Pastries)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Dietary Tag selection
            Text("Dietary Tag", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DietaryTag.entries.forEach { tag ->
                    FilterChip(
                        selected = form.dietaryTag == tag,
                        onClick = { viewModel.updateForm { copy(dietaryTag = tag) } },
                        label = { Text(tag.name) }
                    )
                }
            }

            OutlinedTextField(
                value = form.mealsAvailable.toString(),
                onValueChange = { viewModel.updateForm { copy(mealsAvailable = it.toIntOrNull() ?: 1) } },
                label = { Text("Meals Available") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.originalPrice.toString(),
                onValueChange = { viewModel.updateForm { copy(originalPrice = it.toDoubleOrNull() ?: 0.0) } },
                label = { Text("Original Price (₹)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.discountedPrice.toString(),
                onValueChange = { viewModel.updateForm { copy(discountedPrice = it.toDoubleOrNull() ?: 0.0) } },
                label = { Text("Discounted Price (₹)") },
                modifier = Modifier.fillMaxWidth()
            )

            if (publishState is PublishState.Error) {
                Text(
                    text = (publishState as PublishState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    // TODO: pass real merchantId, businessName, geoHash
                    viewModel.publishListing(
                        merchantId = "merchant_placeholder",
                        businessName = "My Business",
                        geoHash = "wsx4"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = publishState !is PublishState.Publishing
            ) {
                Text("Publish Listing")
            }
        }

        LoadingOverlay(isVisible = publishState is PublishState.Publishing)
    }
}
