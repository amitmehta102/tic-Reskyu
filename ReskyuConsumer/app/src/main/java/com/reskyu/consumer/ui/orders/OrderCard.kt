package com.reskyu.consumer.ui.orders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reskyu.consumer.data.model.Claim
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * OrderCard — premium redesign
 *
 * Features:
 *  - Left accent stripe (color-coded by status)
 *  - Business name + status badge header
 *  - Food item title
 *  - For UPCOMING: pickup code + countdown timer
 *  - Price row: amount paid + savings strikethrough
 *  - Order date + payment ID (tappable to expand)
 */
@Composable
fun OrderCard(
    claim: Claim,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val accentColor = statusAccentColor(claim.status)
    val savedAmount  = (claim.originalPrice - claim.amount).coerceAtLeast(0.0)
    val discountPct  = if (claim.originalPrice > 0)
        ((savedAmount / claim.originalPrice) * 100).toInt() else 0

    Card(
        modifier = modifier.clickable { expanded = !expanded },
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {

            // ── Left accent stripe ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // ── Header: store + status badge ──────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Rounded.Store,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            claim.businessName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    StatusBadge(status = claim.status)
                }

                // ── Food item ─────────────────────────────────────────────────
                Text(
                    claim.heroItem,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp
                )

                // ── Pickup code (UPCOMING only) ────────────────────────────────
                if (claim.status == "PENDING_PICKUP") {
                    PickupCodeRow(claim = claim)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))

                // ── Pricing row ───────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "₹${claim.amount.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (claim.originalPrice > claim.amount) {
                            Text(
                                "₹${claim.originalPrice.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Savings badge
                    if (savedAmount > 0) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Color(0xFF2E7D32).copy(alpha = .1f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "💚 Saved ₹${savedAmount.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // ── Date row ─────────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            formatTimestamp(claim.timestamp.toDate().time),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    // Expand/collapse chevron
                    Icon(
                        if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "More details",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                // ── Expandable detail section ─────────────────────────────────
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))
                        DetailRow(label = "Order ID", value = claim.id.take(12) + "…")
                        DetailRow(label = "Payment ID", value = claim.paymentId)
                        if (discountPct > 0) {
                            DetailRow(label = "Discount", value = "$discountPct% off original price")
                        }
                        DetailRow(
                            label = "Status",
                            value = when (claim.status) {
                                "PENDING_PICKUP" -> "Awaiting pickup"
                                "COMPLETED"      -> "Picked up successfully"
                                "EXPIRED"        -> "Pickup window missed"
                                "DISPUTED"       -> "Under review"
                                else             -> claim.status
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── Pickup Code Row ────────────────────────────────────────────────────────────

@Composable
private fun PickupCodeRow(claim: Claim) {
    // Show last 6 chars of paymentId as the "pickup code"
    val code = claim.paymentId.takeLast(6).uppercase()
    // Countdown
    val countdownText = countdownText(claim.timestamp.toDate().time + TimeUnit.HOURS.toMillis(4))

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFFFF3E0),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "PICKUP CODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE65100),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Text(
                    code,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFBF360C),
                    letterSpacing = 4.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    Icons.Rounded.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFFE65100)
                )
                Text(
                    countdownText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE65100),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Detail Row ─────────────────────────────────────────────────────────────────

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Status Badge ───────────────────────────────────────────────────────────────

@Composable
private fun StatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status) {
        "PENDING_PICKUP" -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "⏳ Pickup pending")
        "COMPLETED"      -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "✅ Completed")
        "DISPUTED"       -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "⚠️ Disputed")
        "EXPIRED"        -> Triple(Color(0xFFF5F5F5), Color(0xFF757575), "😔 Missed")
        else             -> Triple(Color(0xFFF5F5F5), Color(0xFF757575), status)
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun statusAccentColor(status: String): Color = when (status) {
    "PENDING_PICKUP" -> Color(0xFFE65100)   // deep orange
    "COMPLETED"      -> Color(0xFF2E7D32)   // dark green
    "DISPUTED"       -> Color(0xFFC62828)   // dark red
    else             -> Color(0xFF9E9E9E)   // grey
}

private fun formatTimestamp(ms: Long): String =
    SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(ms))

private fun countdownText(pickupDeadlineMs: Long): String {
    val left = pickupDeadlineMs - System.currentTimeMillis()
    if (left <= 0) return "Window closed"
    val h = TimeUnit.MILLISECONDS.toHours(left)
    val m = TimeUnit.MILLISECONDS.toMinutes(left) % 60
    return if (h > 0) "Pick up within ${h}h ${m}m" else "Pick up within ${m}m!"
}
