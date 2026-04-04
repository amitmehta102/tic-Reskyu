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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reskyu.consumer.data.model.Claim
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val OC_Surface         = Color.White
private val OC_Text            = Color(0xFF0C1E13)   // GreenDark
private val OC_TextSub         = Color(0xFF5A7A65)   // muted sage (keep)
private val OC_Outline         = Color(0xFFB0CABB)   // soft outline (keep)
private val OC_Primary         = Color(0xFF52B788)   // GreenAccent — price / icon
private val OC_DividerAlpha    = Color(0xFFD4EDDA)   // light green divider (keep)

@Composable
fun OrderCard(
    claim: Claim,
    modifier: Modifier = Modifier,
    onCardClick: (() -> Unit)? = null,   // if set, click opens external dialog instead of expanding
    onRate: (Int) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var userRating by remember(claim.id) { mutableStateOf(claim.rating) }

    val accentColor = statusAccentColor(claim.status)
    val savedAmount  = (claim.originalPrice - claim.amount).coerceAtLeast(0.0)
    val discountPct  = if (claim.originalPrice > 0)
        ((savedAmount / claim.originalPrice) * 100).toInt() else 0

    Card(
        modifier = modifier.clickable {
            if (onCardClick != null) onCardClick() else expanded = !expanded
        },
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = OC_Surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {

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
                            tint = OC_TextSub
                        )
                        Text(
                            claim.businessName,
                            style = MaterialTheme.typography.labelMedium,
                            color = OC_TextSub,
                            maxLines = 1
                        )
                    }
                    StatusBadge(status = claim.status)
                }

                Text(
                    claim.heroItem,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OC_Text,
                    lineHeight = 18.sp
                )

                if (claim.status == "PENDING_PICKUP") {
                    PickupCodeRow(claim = claim)
                }

                HorizontalDivider(color = OC_DividerAlpha)

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
                            color = OC_Primary
                        )
                        if (claim.originalPrice > claim.amount) {
                            Text(
                                "₹${claim.originalPrice.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = TextDecoration.LineThrough,
                                color = OC_TextSub
                            )
                        }
                    }

                    if (savedAmount > 0) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Color(0xFFD1FAE5),   // very light mint
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "💚 Saved ₹${savedAmount.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF059669),   // emerald
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

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
                            tint = OC_Outline
                        )
                        Text(
                            formatTimestamp(claim.timestamp.toDate().time),
                            style = MaterialTheme.typography.labelSmall,
                            color = OC_Outline
                        )
                    }

                    if (onCardClick == null) {
                        Icon(
                            if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "More details",
                            modifier = Modifier.size(16.dp),
                            tint = OC_Outline
                        )
                    }
                }

                if (claim.status == "REFUNDED") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE3F2FD)
                    ) {
                        Text(
                            "↩️ Refund processed · amount returned to your original payment method",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1565C0),
                            modifier = androidx.compose.ui.Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                if (claim.status != "PENDING_PICKUP") {
                    RatingRow(
                        rating = userRating,
                        onRate = { stars ->
                            userRating = stars
                            onRate(stars)
                        }
                    )
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        HorizontalDivider(color = OC_DividerAlpha)
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
                                "REFUNDED"       -> "Refund processed"
                                else             -> claim.status
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickupCodeRow(claim: Claim) {
    val code = claim.paymentId.takeLast(6).uppercase()
    val countdownText = countdownText(
        if (claim.pickupDeadlineMs > 0) claim.pickupDeadlineMs
        else claim.timestamp.toDate().time + TimeUnit.HOURS.toMillis(4)
    )

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

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = OC_TextSub
        )
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = OC_Text
        )
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status) {
        "PENDING_PICKUP" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "⏳ Pickup pending")
        "COMPLETED"      -> Triple(Color(0xFFE8F5E9), Color(0xFF1B5E20), "✅ Completed")
        "DISPUTED"       -> Triple(Color(0xFFFCE4EC), Color(0xFFC62828), "⚠️ Disputed")
        "REFUNDED"       -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "↩️ Refunded")
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

private fun statusAccentColor(status: String): Color = when (status) {
    "PENDING_PICKUP" -> Color(0xFFFB923C)   // soft amber-orange
    "COMPLETED"      -> Color(0xFF34D399)   // soft emerald green
    "DISPUTED"       -> Color(0xFFF87171)   // soft rose red
    "REFUNDED"       -> Color(0xFF60A5FA)   // soft sky blue
    else             -> Color(0xFFD1D5DB)   // light grey
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

@Composable
fun RatingRow(
    rating: Int,
    onRate: (Int) -> Unit
) {
    val starFilled   = Color(0xFFFFC107)   // gold
    val starEmpty    = Color(0xFFDDD9CC)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            if (rating == 0) "Rate this order" else "Thanks for rating!",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (rating == 0) Color(0xFF555555) else Color(0xFF2E7D32)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 1..5) {
                val filled  = i <= rating
                val scale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (filled) 1.15f else 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
                    ),
                    label = "star_scale_$i"
                )
                Icon(
                    imageVector = if (filled) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    contentDescription = "$i star",
                    tint = if (filled) starFilled else starEmpty,
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clickable { onRate(i) }
                )
            }
        }
    }
}
