package com.reskyu.consumer.ui.claim

import android.app.Activity
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.razorpay.Checkout
import com.reskyu.consumer.BuildConfig
import com.reskyu.consumer.RazorpayPaymentBus
import com.reskyu.consumer.RazorpayResult
import com.reskyu.consumer.data.model.Listing
import com.reskyu.consumer.data.model.PaymentState
import com.reskyu.consumer.ui.components.LoadingOverlay
import com.reskyu.consumer.ui.navigation.Screen
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimScreen(
    listingId: String,
    initialQuantity: Int = 1,
    navController: NavController,
    viewModel: ClaimViewModel = viewModel()
) {
    val listing       by viewModel.listing.collectAsState()
    val paymentState  by viewModel.paymentState.collectAsState()
    val context       = LocalContext.current
    val razorpayBus   by RazorpayPaymentBus.result.collectAsState()

    // Hoisted so both the Scaffold content and the sticky Pay button can read it
    // Seeded from initialQuantity passed through navigation (from listing card stepper)
    var quantity     by remember { mutableStateOf(initialQuantity.coerceAtLeast(1)) }
    val totalPayable = (listing?.discountedPrice ?: 0.0) * quantity

    LaunchedEffect(listingId) { viewModel.loadListing(listingId) }

    LaunchedEffect(paymentState) {
        if (paymentState is PaymentState.Success) {
            val claimId = (paymentState as PaymentState.Success).paymentId
            navController.navigate(Screen.Confirmation.createRoute(claimId)) {
                popUpTo(Screen.Main.route) { saveState = false }
            }
        }
    }

    // Open Razorpay checkout sheet when ViewModel emits the event
    LaunchedEffect(Unit) {
        viewModel.openCheckoutEvent.collect { event ->
            try {
                Checkout.preload(context)
                val checkout = Checkout()
                checkout.setKeyID(BuildConfig.RAZORPAY_KEY_ID)
                val options = JSONObject().apply {
                    put("name", "Reskyu")
                    put("description", "${event.businessName} — ${event.heroItem}")
                    put("order_id", event.orderId)
                    put("amount", event.amount)
                    put("currency", "INR")
                    put("prefill", JSONObject().apply {
                        put("email", event.email)
                    })
                    put("theme", JSONObject().apply {
                        put("color", "#2DC653")
                    })
                }
                checkout.open(context as Activity, options)
            } catch (e: Exception) {
                viewModel.onPaymentFailed("Failed to open payment: ${e.message}")
            }
        }
    }

    // Handle Razorpay payment result from MainActivity via the bus
    LaunchedEffect(razorpayBus) {
        val l = listing ?: return@LaunchedEffect
        when (val result = razorpayBus) {
            is RazorpayResult.Success -> {
                RazorpayPaymentBus.reset()
                viewModel.onPaymentSuccess(result.paymentId, result.signature, l)
            }
            is RazorpayResult.Failure -> {
                RazorpayPaymentBus.reset()
                viewModel.onPaymentFailed(result.reason)
            }
            is RazorpayResult.Idle -> { /* nothing */ }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Checkout") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            listing?.let { l ->
                val maxQty       = l.mealsLeft.coerceAtLeast(1)
                // quantity is hoisted at composable level
                val totalOriginal= l.originalPrice  * quantity
                val savings      = totalOriginal - totalPayable

                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // ── Listing mini-card ─────────────────────────────────────
                    ListingMiniCard(listing = l)

                    // ── Order breakdown ───────────────────────────────────────
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Order Summary",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            HorizontalDivider()

                            // ── Quantity stepper ──────────────────────────────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Portions",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                QuantityStepper(
                                    quantity    = quantity,
                                    max         = maxQty,
                                    onDecrement = { if (quantity > 1) quantity-- },
                                    onIncrement = { if (quantity < maxQty) quantity++ }
                                )
                            }

                            HorizontalDivider()

                            PriceRow(
                                label = l.heroItem,
                                value = "₹${l.originalPrice.toInt()}" +
                                        if (quantity > 1) " × $quantity" else ""
                            )
                            PriceRow(
                                label = "Reskyu Discount",
                                value = "−₹${savings.toInt()}",
                                valueColor = MaterialTheme.colorScheme.primary
                            )

                            HorizontalDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth().animateContentSize(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Total Payable",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "₹${totalPayable.toInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // You save highlight
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "You're saving ₹${savings.toInt()} on this order!",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // ── Impact preview ────────────────────────────────────────
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val co2 = String.format("%.1f", 2.5 * quantity)
                            ImpactPill(emoji = "🌍", label = "${co2}kg CO₂ saved")
                            ImpactPill(emoji = "🍱", label = "$quantity meal${if (quantity > 1) "s" else ""} rescued")
                            ImpactPill(emoji = "💚", label = "Planet thanks you!")
                        }
                    }

                    // ── Payment method badge ───────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Secured by Razorpay · UPI · Cards · Wallets",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // ── Error message ─────────────────────────────────────────
                    if (paymentState is PaymentState.Failed) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "❌ ${(paymentState as PaymentState.Failed).reason}",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            } ?: Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // ── Sticky Pay Button ─────────────────────────────────────────────────
        listing?.let { l ->
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { viewModel.initiatePayment(l, quantity) },
                        enabled = paymentState == PaymentState.Idle || paymentState is PaymentState.Failed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .height(56.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Pay ₹${totalPayable.toInt()} Securely" +
                                    if (quantity > 1) " (×$quantity)" else "",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (paymentState is PaymentState.Processing) LoadingOverlay()
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────────

@Composable
private fun ListingMiniCard(listing: Listing) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (listing.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = listing.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("🍱", fontSize = 28.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    listing.businessName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    listing.heroItem,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Pickup • ${listing.mealsLeft} left",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PriceRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

@Composable
private fun ImpactPill(emoji: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// ── Quantity Stepper ────────────────────────────────────────────────────────────────────

@Composable
private fun QuantityStepper(
    quantity: Int,
    max: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // − button
        FilledIconButton(
            onClick = onDecrement,
            enabled = quantity > 1,
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor   = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
        }

        // Quantity badge
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 40.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$quantity",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // + button
        FilledIconButton(
            onClick = onIncrement,
            enabled = quantity < max,
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
        }
    }
}
