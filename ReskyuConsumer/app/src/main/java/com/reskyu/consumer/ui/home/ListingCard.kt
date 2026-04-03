package com.reskyu.consumer.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.reskyu.consumer.data.model.DietaryTag
import com.reskyu.consumer.data.model.Listing
import com.reskyu.consumer.ui.components.DietaryChip

/**
 * ListingCard
 *
 * A card composable representing a single food listing in the home screen's
 * LazyColumn. Displays the hero image, business name, hero item, dietary tag,
 * meals left, and pricing with discount.
 *
 * @param listing  The [Listing] data to display
 * @param onClick  Callback invoked when the card is tapped (navigate to detail)
 */
@Composable
fun ListingCard(
    listing: Listing,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {

            // Hero Image
            // TODO: Replace Box with AsyncImage (Coil) loading listing.imageUrl
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.small)
            ) {
                // Placeholder — replace with:
                // AsyncImage(model = listing.imageUrl, contentDescription = listing.heroItem,
                //            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {}
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Business name
                Text(
                    text = listing.businessName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                // Hero item
                Text(
                    text = listing.heroItem,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Dietary chip + meals left
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DietaryChip(tag = DietaryTag.valueOf(listing.dietaryTag))
                    Text(
                        text = "${listing.mealsLeft} left",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (listing.mealsLeft <= 2) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Pricing
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "₹${listing.discountedPrice.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "₹${listing.originalPrice.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
