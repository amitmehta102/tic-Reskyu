package com.reskyu.merchant.ui.orders

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reskyu.merchant.data.model.MerchantClaim
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Status mapping ────────────────────────────────────────────────────────────

private fun statusColor(status: String) = when (status) {
    "PENDING_PICKUP" -> Color(0xFFF59E0B)
    "COMPLETED"      -> Color(0xFF10B981)
    "DISPUTED"       -> Color(0xFFEF4444)
    else             -> Color(0xFF9CA3AF)
}

private fun statusLabel(status: String) = when (status) {
    "PENDING_PICKUP" -> "Pending"
    "COMPLETED"      -> "Completed"
    "DISPUTED"       -> "Disputed"
    else             -> status
}

private val GreenDeep   = Color(0xFF163823)
private val GreenAccent = Color(0xFF52B788)

/**
 * A single claim card in the Order Management screen.
 *
 * Layout:
 *  - Top:    hero item name (left)  +  status badge (right)
 *  - Middle: amount · formatted date
 *  - Middle: truncated payment ID
 *  - Bottom (PENDING only): divider + Confirm Pickup / Dispute buttons
 */
@Composable
fun ClaimCard(
    claim:      MerchantClaim,
    onComplete: (String) -> Unit,
    onDispute:  (String) -> Unit
) {
    val sColor = remember(claim.status) { statusColor(claim.status) }
    val sLabel = remember(claim.status) { statusLabel(claim.status) }

    val formattedDate = remember(claim.timestamp) {
        SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
            .format(Date(claim.timestamp))
    }

    val paymentDisplay = remember(claim.paymentId) {
        if (claim.paymentId.length > 22) claim.paymentId.take(22) + "…"
        else claim.paymentId.ifBlank { "—" }
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Row 1: Item name + status badge ───────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Text(
                    text       = claim.heroItem.ifBlank { "Order" },
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFF111827),
                    modifier   = Modifier.weight(1f).padding(end = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(sColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text       = sLabel,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = sColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Row 2: Amount · Date ──────────────────────────────────────────
            Text(
                text     = "₹${claim.amount.toInt()}  ·  $formattedDate",
                fontSize = 13.sp,
                color    = Color(0xFF6B7280)
            )

            // ── Row 3: Payment ID ─────────────────────────────────────────────
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text     = "Pay ID: $paymentDisplay",
                fontSize = 12.sp,
                color    = Color(0xFFB0B8C4)
            )

            // ── Actions: only for PENDING_PICKUP ──────────────────────────────
            if (claim.status == "PENDING_PICKUP") {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color    = Color(0xFFF3F4F6)
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Confirm Pickup
                    Button(
                        onClick  = { onComplete(claim.id) },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = GreenAccent,
                            contentColor   = Color.White
                        )
                    ) {
                        Text(
                            text       = "✓  Confirm",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Dispute
                    OutlinedButton(
                        onClick  = { onDispute(claim.id) },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                Color(0xFFEF4444).copy(alpha = 0.4f)
                            )
                        )
                    ) {
                        Text(
                            text       = "Dispute",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
