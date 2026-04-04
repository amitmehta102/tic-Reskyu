package com.reskyu.consumer.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.reskyu.consumer.data.model.DietaryTag
import com.reskyu.consumer.ui.components.DietaryChip
import com.reskyu.consumer.ui.navigation.Screen
import java.util.concurrent.TimeUnit

/**
 * ListingDetailScreen
 *
 * Full-detail view of a single food listing.
 *
 * Layout:
 *  ┌── Full-width hero image (with back button overlay) ──────────────┐
 *  │   Scrollable body:                                               │
 *  │    · Business name + dietary chip + rating row                   │
 *  │    · Hero item title + description row                           │
 *  │    · Pricing pill (discounted + crossed original + savings %)    │
 *  │    · Info rows: meals left, location, expiry                     │
 *  │    · Divider                                                     │
 *  │    · Impact card: CO₂ saved estimate, money saved                │
 *  └──────────────────────────────────────────────────────────────────┘
 *  Sticky bottom bar: "Claim Now — ₹X" button
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
    val error by viewModel.error.collectAsState()

    LaunchedEffect(listingId) { viewModel.loadListing(listingId) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("😕", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadListing(listingId) }) { Text("Retry") }
                    }
                }
            }

            listing != null -> {
                val l = listing!!
                val discountPct = if (l.originalPrice > 0)
                    ((l.originalPrice - l.discountedPrice) / l.originalPrice * 100).toInt() else 0
                val timeLeftMs = l.expiresAt.toDate().time - System.currentTimeMillis()
                val isOpen = l.status == "OPEN" && l.mealsLeft > 0 && timeLeftMs > 0
                val co2Saved = 2.5  // kg per meal (configurable)

                // Quantity state — drives the stepper in the bottom bar and impact card
                var quantity     by remember { mutableStateOf(1) }
                val maxQty       = l.mealsLeft.coerceAtLeast(1)
                val totalPayable = l.discountedPrice * quantity

                // ── Scrollable content ────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 152.dp) // space for quantity stepper + claim button
                ) {

                    // ── Hero Image ────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        if (l.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = l.imageUrl,
                                contentDescription = l.heroItem,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🍱", fontSize = 72.sp)
                            }
                        }

                        // Gradient scrim at bottom of image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                                        )
                                    )
                                )
                        )

                        // Back button
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .padding(12.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(50)
                                )
                        ) {
                            Icon(
                                Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Discount badge
                        if (discountPct > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "-$discountPct% OFF",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ── Body Content ──────────────────────────────────────────
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        // Business name + dietary chip row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = l.businessName,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            DietaryChip(tag = DietaryTag.valueOf(l.dietaryTag))
                        }

                        // Hero item title
                        Text(
                            text = l.heroItem,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        // ── Pricing ───────────────────────────────────────────
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "₹${l.discountedPrice.toInt()}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "₹${l.originalPrice.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                textDecoration = TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (discountPct > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Save ₹${(l.originalPrice - l.discountedPrice).toInt()}",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        // ── Info rows ─────────────────────────────────────────
                        InfoRow(
                            icon = "🍽️",
                            label = "Portions available",
                            value = "${l.mealsLeft} left",
                            valueColor = when {
                                l.mealsLeft <= 1 -> MaterialTheme.colorScheme.error
                                l.mealsLeft <= 3 -> Color(0xFFE65100)
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )

                        InfoRow(
                            icon = "⏰",
                            label = "Pickup window closes",
                            value = formatExpiryDetail(timeLeftMs),
                            valueColor = if (timeLeftMs < TimeUnit.HOURS.toMillis(1))
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                        )

                        InfoRow(
                            icon = "📍",
                            label = "Source",
                            value = l.businessName
                        )

                        HorizontalDivider()

                        // ── Impact Card ───────────────────────────────────────
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val co2Total = String.format("%.1f", co2Saved * quantity)
                                ImpactStat(emoji = "🌍", value = "${co2Total}kg", label = "CO₂ saved")
                                VerticalDivider(modifier = Modifier.height(40.dp))
                                ImpactStat(
                                    emoji = "💰",
                                    value = "₹${((l.originalPrice - l.discountedPrice) * quantity).toInt()}",
                                    label = "Money saved"
                                )
                                VerticalDivider(modifier = Modifier.height(40.dp))
                                ImpactStat(emoji = "🍱", value = "$quantity", label = "Meal${if (quantity > 1) "s" else ""} rescued")
                            }
                        }

                        // Sold out / cancelled notice
                        if (!isOpen) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = when {
                                        timeLeftMs <= 0 -> "⏰ This listing has expired"
                                        l.mealsLeft <= 0 -> "❌ Sold out — check back tomorrow!"
                                        else -> "🚫 This listing is no longer available"
                                    },
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // ── Sticky Bottom Bar (quantity stepper + claim) ──────────────────
                Box(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // ── Portions stepper + reactive total ────────────────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // [Portions label] [−] [qty] [+]
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "Portions",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { if (quantity > 1) quantity-- },
                                        modifier = Modifier.size(32.dp),
                                        enabled = quantity > 1 && isOpen
                                    ) {
                                        Icon(
                                            Icons.Rounded.Remove,
                                            contentDescription = "Less",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (quantity > 1 && isOpen)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    }
                                    Text(
                                        "$quantity",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { if (quantity < maxQty) quantity++ },
                                        modifier = Modifier.size(32.dp),
                                        enabled = quantity < maxQty && isOpen
                                    ) {
                                        Icon(
                                            Icons.Rounded.Add,
                                            contentDescription = "More",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (quantity < maxQty && isOpen)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    }
                                }

                                // Reactive total
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "₹${totalPayable.toInt()}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        if (quantity > 1)
                                            "₹${l.discountedPrice.toInt()} × $quantity"
                                        else
                                            "instead of ₹${l.originalPrice.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // ── Claim button (full width) ──────────────────────────
                            Button(
                                onClick = {
                                    navController.navigate(
                                        Screen.Claim.createRoute(l.id, quantity)
                                    )
                                },
                                enabled = isOpen,
                                modifier = Modifier.height(52.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = if (isOpen) "Claim Now →" else "Unavailable",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Helper Composables ─────────────────────────────────────────────────────────

@Composable
private fun InfoRow(
    icon: String,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(icon, fontSize = 16.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun ImpactStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 22.sp)
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

private fun formatExpiryDetail(timeLeftMs: Long): String {
    if (timeLeftMs <= 0) return "Expired"
    val h = TimeUnit.MILLISECONDS.toHours(timeLeftMs)
    val m = TimeUnit.MILLISECONDS.toMinutes(timeLeftMs) % 60
    return when {
        h <= 0 -> "in ${m}m"
        else -> "in ${h}h ${m}m"
    }
}
