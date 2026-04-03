package com.reskyu.merchant.ui.live_listings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reskyu.merchant.data.model.Listing

/**
 * A single listing card in the Live Listings list.
 * Shows key listing info and provides a Cancel quick-action.
 *
 * @param listing      The [Listing] to display.
 * @param onCancel     Called when the merchant taps "Cancel".
 */
@Composable
fun LiveListingCard(
    listing: Listing,
    onCancel: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = listing.heroItem,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(listing.dietaryTag) }
                )
                AssistChip(
                    onClick = {},
                    label = { Text("${listing.mealsLeft} left") }
                )
                AssistChip(
                    onClick = {},
                    label = { Text(listing.status) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "₹${listing.discountedPrice.toInt()} / ₹${listing.originalPrice.toInt()}",
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = { onCancel(listing.id) }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
