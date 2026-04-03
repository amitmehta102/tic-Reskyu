package com.reskyu.merchant.ui.live_listings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reskyu.merchant.data.model.DietaryTag
import com.reskyu.merchant.data.model.Listing
import com.reskyu.merchant.data.model.ListingStatus

// ── Status → color mapping ────────────────────────────────────────────────────
private fun statusColor(status: String) = when (status) {
    ListingStatus.OPEN.name    -> Color(0xFF52B788)
    ListingStatus.CLOSING.name -> Color(0xFFF4A261)
    ListingStatus.SOLD_OUT.name -> Color(0xFFE63946)
    else                       -> Color(0xFF9CA3AF)
}

// ── Dietary tag → badge color ─────────────────────────────────────────────────
private fun dietaryColor(tag: String) = when (tag) {
    DietaryTag.VEG.name     -> Color(0xFF2D6A4F)
    DietaryTag.NON_VEG.name -> Color(0xFFE63946)
    DietaryTag.VEGAN.name   -> Color(0xFF457B9D)
    else                    -> Color(0xFF9CA3AF)
}

// ── Expiry time format ────────────────────────────────────────────────────────
private fun formatTimeLeft(expiresAt: Long): String {
    val diff = expiresAt - System.currentTimeMillis()
    return when {
        diff <= 0          -> "Expired"
        diff < 3_600_000   -> "${diff / 60_000}m left"
        else               -> "${diff / 3_600_000}h ${(diff % 3_600_000) / 60_000}m left"
    }
}

/**
 * A single listing card in the Live Listings screen.
 *
 * Layout:
 *  - Top: item name (left) + status badge (right)
 *  - Middle: dietary tag • meals left • time left
 *  - Divider
 *  - Bottom: discounted price ~~original~~ (left) + Cancel button (right)
 */
@Composable
fun LiveListingCard(
    listing:  Listing,
    onCancel: (String) -> Unit
) {
    val sColor = remember(listing.status) { statusColor(listing.status) }
    val dColor = remember(listing.dietaryTag) { dietaryColor(listing.dietaryTag) }

    val timeLeft = remember(listing.expiresAt) { formatTimeLeft(listing.expiresAt) }

    val isExpiringSoon = remember(listing.expiresAt) {
        val diff = listing.expiresAt - System.currentTimeMillis()
        diff in 1..1_800_000  // less than 30 min
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Row 1: Name + status badge ────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Text(
                    text       = listing.heroItem.ifBlank { "Unnamed Item" },
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFF111827),
                    modifier   = Modifier.weight(1f).padding(end = 8.dp)
                )
                // Status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(sColor.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text       = listing.status,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = sColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Row 2: Tags ───────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Dietary badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(dColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text       = listing.dietaryTag,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color      = dColor
                    )
                }

                // Meals left
                Text(
                    text     = "📦 ${listing.mealsLeft} meals",
                    fontSize = 12.sp,
                    color    = Color(0xFF6B7280)
                )

                // Time left (red when < 30 min)
                if (listing.expiresAt > 0L) {
                    Text(
                        text     = "⏱ $timeLeft",
                        fontSize = 12.sp,
                        color    = if (isExpiringSoon) Color(0xFFE63946) else Color(0xFF6B7280),
                        fontWeight = if (isExpiringSoon) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            // ── Divider ───────────────────────────────────────────────────────
            HorizontalDivider(
                modifier  = Modifier.padding(vertical = 12.dp),
                color     = Color(0xFFF3F4F6)
            )

            // ── Row 3: Price + Cancel ─────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text       = "₹${listing.discountedPrice.toInt()}",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color(0xFF2D6A4F)
                    )
                    Text(
                        text            = "₹${listing.originalPrice.toInt()}",
                        fontSize        = 14.sp,
                        color           = Color(0xFFD1D5DB),
                        textDecoration  = TextDecoration.LineThrough,
                        modifier        = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Cancel only makes sense for OPEN / CLOSING listings
                if (listing.status in listOf(
                        ListingStatus.OPEN.name,
                        ListingStatus.CLOSING.name
                    )
                ) {
                    TextButton(
                        onClick = { onCancel(listing.id) },
                        colors  = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFE63946)
                        )
                    ) {
                        Text(
                            text       = "Cancel",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
