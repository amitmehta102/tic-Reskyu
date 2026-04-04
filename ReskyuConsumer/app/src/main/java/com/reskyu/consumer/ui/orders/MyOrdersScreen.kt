package com.reskyu.consumer.ui.orders

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.reskyu.consumer.data.model.Claim
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ── Orders palette — exact merchant brand ─────────────────────────────────────
private val LBackground  = Color(0xFFF2F8F4)   // ScreenBg
private val LSurface     = Color.White
private val LAccent      = Color(0xFF52B788)   // GreenAccent
private val LOnAccent    = Color.White
private val LText        = Color(0xFF0C1E13)   // GreenDark
private val LTextSub     = Color(0xFF6B7280)   // grey subtitle (keep)
private val LDivider     = Color(0xFFD4EDDA)   // light green divider (keep)
private val LChipSel     = Color(0xFF52B788)   // GreenAccent
private val LChipUnsel   = Color(0xFFF0F4F2)   // light surface (keep)
private val LIconTint    = Color(0xFF52B788)   // GreenAccent

// Header gradient — matches HomeScreen / merchant dashboard
private val HGrad = listOf(Color(0xFF0C1E13), Color(0xFF163823), Color(0xFF1F5235))
private val HLight = Color(0xFF95D5B2)   // GreenLight for subtitle on dark

/**
 * MyOrdersScreen — dark gradient header matching HomeScreen + dialog ticket for current orders
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(
    navController: NavController,
    viewModel: MyOrdersViewModel = viewModel()
) {
    val allClaims by viewModel.allClaims.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTab    by remember { mutableStateOf(0) }       // 0=Current, 1=Past
    var selectedOrder  by remember { mutableStateOf<Claim?>(null) }

    val currentOrders   = remember(allClaims) { allClaims.filter { it.status == "PENDING_PICKUP" } }
    val pastOrders      = remember(allClaims) { allClaims.filter { it.status != "PENDING_PICKUP" } }
    val displayedOrders = if (selectedTab == 0) currentOrders else pastOrders

    val totalSaved   = allClaims.sumOf { (it.originalPrice - it.amount).coerceAtLeast(0.0) }
    val mealsRescued = allClaims.count { it.status == "COMPLETED" }

    // Order detail dialog — shown when a current order card is tapped
    selectedOrder?.let { claim ->
        OrderDetailDialog(claim = claim, onDismiss = { selectedOrder = null })
    }

    Column(modifier = Modifier.fillMaxSize().background(LBackground)) {

        // ── Dark gradient header — pinned, never scrolls ───────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Brush.verticalGradient(HGrad))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 18.dp, bottom = 22.dp)
            ) {
                Text(
                    "My Orders 🛍️",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Your food rescue history",
                    style = MaterialTheme.typography.bodySmall,
                    color = HLight
                )
            }
        }

        // ── Scrollable list below the pinned header ────────────────────────────
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh    = { viewModel.refresh() },
            modifier     = Modifier.weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LBackground)
            ) {

                // ── 3 stat chips ────────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LightStatChip(
                            icon     = Icons.Rounded.LocalDining,
                            value    = "$mealsRescued",
                            label    = "Rescued",
                            modifier = Modifier.weight(1f)
                        )
                        LightStatChip(
                            icon     = Icons.Rounded.Savings,
                            value    = "₹${totalSaved.toInt()}",
                            label    = "Saved",
                            modifier = Modifier.weight(1f)
                        )
                        LightStatChip(
                            icon     = Icons.Rounded.Receipt,
                            value    = "${allClaims.size}",
                            label    = "Total",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Current / Past pill tabs ─────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LightPillTab(
                            label    = "Current",
                            count    = currentOrders.size,
                            selected = selectedTab == 0,
                            onClick  = { selectedTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        LightPillTab(
                            label    = "Past",
                            count    = pastOrders.size,
                            selected = selectedTab == 1,
                            onClick  = { selectedTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Content ──────────────────────────────────────────────────────
                if (displayedOrders.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (selectedTab == 0) "🛍️" else "🌟", fontSize = 52.sp)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    if (selectedTab == 0) "No current orders" else "No past orders yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LText,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    if (selectedTab == 0) "Claim a food drop from Home!"
                                    else "Your rescued meals will appear here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LTextSub,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    item { Spacer(Modifier.height(4.dp)) }
                    items(displayedOrders, key = { it.id }) { claim ->
                        val isCurrent = claim.status == "PENDING_PICKUP"
                        OrderCard(
                            claim       = claim,
                            modifier    = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 5.dp),
                            // current orders → open dialog; past orders → inline expand
                            onCardClick = if (isCurrent) ({ selectedOrder = claim }) else null,
                            onRate      = { stars -> viewModel.submitRating(claim.id, claim.merchantId, stars) }
                        )
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }   // end LazyColumn
        }       // end PullToRefreshBox
    }           // end outer Column
}

// ── Order Detail Dialog ───────────────────────────────────────────────────────

@Composable
private fun OrderDetailDialog(claim: Claim, onDismiss: () -> Unit) {
    val savedAmount = (claim.originalPrice - claim.amount).coerceAtLeast(0.0)
    val pickupCode  = claim.paymentId.takeLast(6).uppercase()

    // Countdown timer
    val deadlineMs = claim.pickupDeadlineMs
        ?: (claim.timestamp?.toDate()?.time?.plus(TimeUnit.HOURS.toMillis(1)))
    var timeLeftMs by remember { mutableStateOf(
        deadlineMs?.minus(System.currentTimeMillis()) ?: 0L
    ) }
    LaunchedEffect(deadlineMs) {
        while (true) {
            timeLeftMs = (deadlineMs?.minus(System.currentTimeMillis())) ?: 0L
            kotlinx.coroutines.delay(1000L)
        }
    }
    val countdownText = formatCountdown(timeLeftMs)
    val isUrgent = timeLeftMs in 1..TimeUnit.MINUTES.toMillis(30)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(12.dp),
            colors = CardDefaults.cardColors(containerColor = LSurface)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // ── Dark gradient header ────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Brush.verticalGradient(HGrad))
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                claim.businessName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                claim.heroItem,
                                style = MaterialTheme.typography.bodySmall,
                                color = HLight
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // ── Body ───────────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // QR code
                    QrCodeImageLocal(content = claim.id, size = 160.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Show this QR at the counter",
                        style = MaterialTheme.typography.labelSmall,
                        color = LTextSub
                    )

                    Spacer(Modifier.height(14.dp))

                    // Pickup code + countdown row
                    val urgentColor = if (isUrgent) Color(0xFFE65100) else LAccent
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = urgentColor.copy(alpha = 0.09f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "PICKUP CODE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = urgentColor,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    pickupCode,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = urgentColor
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Icon(
                                    Icons.Rounded.AccessTime,
                                    contentDescription = "Time left",
                                    tint = urgentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    countdownText,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = urgentColor
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = LDivider)
                    Spacer(Modifier.height(12.dp))

                    // Transaction details
                    DetailRow("Amount Paid",     "₹${claim.amount.toInt()}")
                    DetailRow("Original Price",  "₹${claim.originalPrice.toInt()}")
                    DetailRow("You Saved",       "₹${savedAmount.toInt()}")
                    DetailRow("Date",            formatClaimDate(claim.timestamp))
                    claim.paymentId.takeIf { it.isNotBlank() }?.let {
                        DetailRow("Payment ID", it.take(24) + if (it.length > 24) "…" else "")
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1F5235)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Helper composables / functions ────────────────────────────────────────────

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = LTextSub)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = LText
        )
    }
}

private fun formatCountdown(ms: Long): String {
    if (ms <= 0) return "Expired"
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return when {
        h > 0  -> "${h}h ${m}m"
        m > 0  -> "${m}m ${s}s"
        else   -> "${s}s"
    }
}

private fun formatClaimDate(ts: com.google.firebase.Timestamp?): String {
    if (ts == null) return "—"
    val sdf = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
    return sdf.format(ts.toDate())
}

@Composable
private fun QrCodeImageLocal(content: String, size: Dp, modifier: Modifier = Modifier) {
    val bitmap = remember(content) { generateQrBitmapLocal(content, 512) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "QR Code",
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .background(color = Color(0xFFF0F4F2), shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("QR", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun generateQrBitmapLocal(content: String, px: Int): Bitmap? {
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

// ── Light Pill Tab ────────────────────────────────────────────────────────────

@Composable
private fun LightPillTab(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick         = onClick,
        modifier        = modifier,
        shape           = RoundedCornerShape(12.dp),
        color           = if (selected) LChipSel else LChipUnsel,
        shadowElevation = if (selected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 11.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) LOnAccent else LTextSub
            )
            if (count > 0) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(
                            color = if (selected) Color.White.copy(alpha = 0.28f) else LDivider,
                            shape = CircleShape
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) LOnAccent else LTextSub,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// ── Light Stat Chip ───────────────────────────────────────────────────────────

@Composable
private fun LightStatChip(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier        = modifier,
        shape           = RoundedCornerShape(14.dp),
        color           = LSurface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = LIconTint)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = LText)
            Text(label, style = MaterialTheme.typography.labelSmall, color = LTextSub)
        }
    }
}
