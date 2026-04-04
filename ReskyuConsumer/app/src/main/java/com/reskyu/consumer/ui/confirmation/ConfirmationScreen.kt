package com.reskyu.consumer.ui.confirmation

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.reskyu.consumer.ui.navigation.Screen

/**
 * ConfirmationScreen
 *
 * Shown after a successful claim. Features:
 *  - Animated scale+fade entrance on the success icon
 *  - Dashed-border "ticket" card (mimics a real pickup ticket)
 *  - Ticket rows: business, item, amount, payment ID, pickup deadline
 *  - "View My Orders" and "Back to Home" CTAs
 */
@Composable
fun ConfirmationScreen(
    claimId: String,
    navController: NavController,
    viewModel: ConfirmationViewModel = viewModel()
) {
    val ticketState by viewModel.ticketState.collectAsState()

    LaunchedEffect(claimId) { viewModel.loadTicket(claimId) }

    // Entrance animations
    val iconScale  = remember { Animatable(0f) }
    val bodyAlpha  = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        iconScale.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        bodyAlpha.animateTo(1f, tween(400))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Animated success icon ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(iconScale.value)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Success",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Claim Confirmed! 🎉",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F5235),   // GreenMid — on-white brand emphasis
            modifier = Modifier.alpha(bodyAlpha.value)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Show this ticket at pickup",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(bodyAlpha.value)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Ticket Card ───────────────────────────────────────────────────────
        ticketState?.let { ticket ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(bodyAlpha.value)
            ) {
                // Top half of ticket
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    ticket.businessName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    ticket.heroItem,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "CONFIRMED",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(16.dp))

                        // Ticket rows
                        TicketRow(label = "Amount Paid", value = "₹${ticket.amount.toInt()}", bold = true)
                        Spacer(Modifier.height(8.dp))
                        TicketRow(label = "Pickup Before", value = ticket.pickupByTime, highlight = true)
                        Spacer(Modifier.height(8.dp))
                        TicketRow(label = "Reference", value = ticket.paymentId, small = true)
                    }
                }

                // ── Dashed tear line ──────────────────────────────────────────
                DashedDivider()

                // Bottom half of ticket (QR placeholder / emoji)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Real QR code from claimId
                            QrCodeImage(
                                content = ticket.claimId,
                                size    = 120.dp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Show this at the counter",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } ?: CircularProgressIndicator()

        Spacer(modifier = Modifier.height(32.dp))

        // ── Impact summary row ────────────────────────────────────────────────
        ticketState?.let {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(bodyAlpha.value),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🌍", fontSize = 22.sp)
                        Text("2.5kg", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("CO₂ Saved", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🍱", fontSize = 22.sp)
                        Text("1 meal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Rescued", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💰", fontSize = 22.sp)
                        Text("₹${ticketState?.let { (it.amount + (it.amount * 0.6)).toInt() } ?: 0}",
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Saved", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── CTAs ──────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(bodyAlpha.value),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Main.route) { inclusive = false }
                    }
                },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Rounded.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Home")
            }
            Button(
                onClick = {
                    // Signal MainScreen to switch inner nav to Orders tab
                    com.reskyu.consumer.TabNavigationBus.navigateTo(Screen.MyOrders.route)
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Main.route) { inclusive = false }
                    }
                },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Rounded.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("My Orders")
            }
        }
    }
}

// ── Helper Composables ─────────────────────────────────────────────────────────

@Composable
private fun TicketRow(
    label: String,
    value: String,
    bold: Boolean = false,
    highlight: Boolean = false,
    small: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = when {
                highlight -> MaterialTheme.colorScheme.error
                bold -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun DashedDivider() {
    val color = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
        )
    }
}

// ── QR Code ───────────────────────────────────────────────────────────────────

/**
 * Composable that renders a square QR code for [content] using ZXing.
 * The bitmap is generated once and memoised; on error a plain grey box is shown.
 */
@Composable
private fun QrCodeImage(content: String, size: Dp, modifier: Modifier = Modifier) {
    val bitmap = remember(content) { generateQrBitmap(content, 512) }

    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "QR Code",
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size)
        )
    } else {
        // Fallback if ZXing encoding fails
        Box(
            modifier = modifier
                .size(size)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("QR", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun generateQrBitmap(content: String, px: Int): Bitmap? {
    return try {
        val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, px, px)
        Bitmap.createBitmap(px, px, Bitmap.Config.RGB_565).also { bmp ->
            for (x in 0 until px) {
                for (y in 0 until px) {
                    bmp.setPixel(x, y, if (bits[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
                }
            }
        }
    } catch (_: Exception) { null }
}
