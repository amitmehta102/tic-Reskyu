package com.reskyu.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reskyu.consumer.data.model.DietaryTag
import com.reskyu.consumer.data.model.Listing
import com.reskyu.consumer.ui.components.DietaryChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ── ListingCard light-theme palette (matches screenshot) ───────────────────
private val LC_Text        = Color(0xFF133922)   // dark forest green
private val LC_TextSub     = Color(0xFF5A7A65)   // muted sage green-grey
private val LC_Outline     = Color(0xFFB0CABB)   // soft green outline
private val LC_Green       = Color(0xFF1A9E45)   // darker price green
private val LC_Error       = Color(0xFFD32F2F)   // error red
private val LC_Surface     = Color(0xFFEBF7EE)   // very light mint (card placeholder)

/**
 * ListingCard
 *
 * A card composable for the home screen LazyColumn.
 * Features:
 *  - Coil AsyncImage hero thumbnail (graceful placeholder/error)
 *  - Discount % badge overlaid on image
 *  - Dietary tag chip + meals left with urgency coloring
 *  - Strikethrough original price + discounted price
 *  - Expiry countdown ("Expires in 2h 15m" / "Expiring soon!")
 */
@Composable
fun ListingCard(
    listing: Listing,
    onClick: () -> Unit,
    distanceKm: Double? = null,
    modifier: Modifier = Modifier
) {
    val discountPct = if (listing.originalPrice > 0)
        ((listing.originalPrice - listing.discountedPrice) / listing.originalPrice * 100).toInt()
    else 0

    val timeLeftMs = listing.expiresAt.toDate().time - System.currentTimeMillis()
    val expiryText = formatExpiry(timeLeftMs)
    val isExpiringSoon = timeLeftMs < TimeUnit.HOURS.toMillis(1)
    val distanceText = formatDistance(distanceKm)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ── Hero Image with discount badge ────────────────────────────────
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(MaterialTheme.shapes.small)
            ) {
                AsyncImage(
                    model = listing.imageUrl.ifBlank { null },
                    contentDescription = listing.heroItem,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    // Placeholder composable shown while loading
                    error = null,
                )
                // Grey placeholder when imageUrl is blank
                if (listing.imageUrl.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LC_Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🍱", fontSize = 28.sp)
                    }
                }

                // Discount badge
                if (discountPct > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(
                                color = Color(0xFF2DC653),
                                shape = RoundedCornerShape(bottomStart = 8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "-$discountPct%",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // ── Content ───────────────────────────────────────────────────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {

                // Business name
                Text(
                    text = listing.businessName,
                    style = MaterialTheme.typography.labelMedium,
                    color = LC_TextSub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Hero item (main title)
                Text(
                    text = listing.heroItem,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = LC_Text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Dietary chip + meals left
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DietaryChip(tag = DietaryTag.valueOf(listing.dietaryTag))

                    // Meals-left urgency chip
                    val (chipBg, chipText, chipLabel) = when {
                        listing.mealsLeft <= 1 -> Triple(
                            Color(0xFFFFEBEE), LC_Error, "🔥 Last 1!"
                        )
                        listing.mealsLeft <= 3 -> Triple(
                            Color(0xFFFFF3E0), Color(0xFFE65100),
                            "⚡ ${listing.mealsLeft} left"
                        )
                        else -> Triple(
                            Color(0xFFE8F5E9), LC_TextSub,
                            "${listing.mealsLeft} left"
                        )
                    }
                    Surface(
                        color = chipBg,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = chipLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = chipText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Pricing row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "₹${listing.discountedPrice.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LC_Green
                    )
                    Text(
                        text = "₹${listing.originalPrice.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = TextDecoration.LineThrough,
                        color = LC_TextSub
                    )
                }

                // Expiry + distance row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = if (isExpiringSoon) LC_Error else LC_Outline
                        )
                        Text(
                            text = expiryText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isExpiringSoon) LC_Error else LC_Outline
                        )
                    }

                    // Distance pill
                    if (distanceText != null) {
                        Text(
                            text = "📍 $distanceText",
                            style = MaterialTheme.typography.labelSmall,
                            color = LC_Green,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun formatExpiry(timeLeftMs: Long): String {
    if (timeLeftMs <= 0) return "Expired"
    val h = TimeUnit.MILLISECONDS.toHours(timeLeftMs)
    val m = TimeUnit.MILLISECONDS.toMinutes(timeLeftMs) % 60
    return when {
        h <= 0 && m <= 30 -> "⚠️ Expiring soon!"
        h <= 0 -> "Expires in ${m}m"
        else -> "Expires in ${h}h ${m}m"
    }
}
