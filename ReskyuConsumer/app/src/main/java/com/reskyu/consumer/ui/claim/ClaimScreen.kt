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
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

// ── Checkout brand palette ────────────────────────────────────────────────────
private val CC_Bg        = Color(0xFFF2F8F4)   // ScreenBg
private val CC_Surface   = Color.White
private val CC_Accent    = Color(0xFF52B788)   // GreenAccent
private val CC_Dark      = Color(0xFF0C1E13)   // GreenDark
private val CC_Mid       = Color(0xFF1F5235)   // GreenMid
private val CC_TextSub   = Color(0xFF5A7A65)   // muted sage
private val CC_Divider   = Color(0xFFD4EDDA)   // light green divider
private val CC_Light     = Color(0xFF95D5B2)   // GreenLight (header subtitle)
private val CC_ImpactBg  = Color(0xFFE8F5EE)   // very light mint
private val CC_SaveBg    = Color(0xFFD1FAE5)   // savings highlight bg
private val CC_SaveText  = Color(0xFF065F46)   // savings highlight text
private val CC_Grad      = listOf(Color(0xFF0C1E13), Color(0xFF163823), Color(0xFF1F5235))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimScreen(
    listingId: String,
    initialQuantity: Int = 1,
    navController: NavController,
    viewModel: ClaimViewModel = viewModel()
) {
    val listing      by viewModel.listing.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()
    val context      = LocalContext.current
    val razorpayBus  by RazorpayPaymentBus.result.collectAsState()

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
                    put("prefill", JSONObject().apply { put("email", event.email) })
                    put("theme", JSONObject().apply { put("color", "#2DC653") })
                }
                checkout.open(context as Activity, options)
            } catch (e: Exception) {
                viewModel.onPaymentFailed("Failed to open payment: ${e.message}")
            }
        }
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CC_Bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Dark gradient header ──────────────────────────────────────────
            CheckoutHeader(onBack = { navController.popBackStack() })

            // ── Scrollable body ───────────────────────────────────────────────
            listing?.let { l ->
                val maxQty       = l.mealsLeft.coerceAtLeast(1)
                val totalOriginal = l.originalPrice * quantity
                val savings      = totalOriginal - totalPayable

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp)
                        .padding(top = 14.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // ── Listing mini-card ─────────────────────────────────────
                    ListingMiniCard(listing = l)

                    // ── Order summary card ────────────────────────────────────
                    SummaryCard(
                        listing       = l,
                        quantity      = quantity,
                        maxQty        = maxQty,
                        totalOriginal = totalOriginal,
                        totalPayable  = totalPayable,
                        savings       = savings,
                        onDecrement   = { if (quantity > 1) quantity-- },
                        onIncrement   = { if (quantity < maxQty) quantity++ }
                    )

                    // ── Impact card ───────────────────────────────────────────
                    ImpactCard(quantity = quantity)

                    // ── Secure payment badge ──────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = CC_TextSub
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Secured by Razorpay · UPI · Cards · Wallets",
                            style = MaterialTheme.typography.labelSmall,
                            color = CC_TextSub
                        )
                    }

                    // ── Payment error ─────────────────────────────────────────
                    if (paymentState is PaymentState.Failed) {
                        Surface(
                            color  = Color(0xFFFFEDED),
                            shape  = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "❌  ${(paymentState as PaymentState.Failed).reason}",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }
                }
            } ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CC_Accent)
            }
        }

        // ── Sticky Pay button ─────────────────────────────────────────────────
        listing?.let { l ->
            Surface(
                modifier        = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color           = CC_Bg,
                shadowElevation = 16.dp,
                tonalElevation  = 0.dp
            ) {
                Button(
                    onClick  = { viewModel.initiatePayment(l, quantity) },
                    enabled  = paymentState == PaymentState.Idle || paymentState is PaymentState.Failed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor         = CC_Mid,
                        contentColor           = Color.White,
                        disabledContainerColor = CC_Accent.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Pay ₹${totalPayable.toInt()} Securely" +
                                if (quantity > 1) "  ×$quantity" else "",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 16.sp
                    )
                }
            }
        }

        if (paymentState is PaymentState.Processing) LoadingOverlay()
    }
}

// ── Dark gradient header ──────────────────────────────────────────────────────

@Composable
private fun CheckoutHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation    = 8.dp,
                shape        = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                ambientColor = Color(0x401F5235),
                spotColor    = Color(0x601F5235)
            )
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.verticalGradient(CC_Grad))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(end = 20.dp)
                .padding(top = 8.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint     = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    "Checkout 🛒",
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
                Text(
                    "Complete your food rescue",
                    style = MaterialTheme.typography.bodySmall,
                    color = CC_Light
                )
            }
        }
    }
}

// ── Listing mini-card ─────────────────────────────────────────────────────────

@Composable
private fun ListingMiniCard(listing: Listing) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CC_Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Food image / emoji fallback
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFECF9F3)),
                contentAlignment = Alignment.Center
            ) {
                if (listing.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model            = listing.imageUrl,
                        contentDescription = null,
                        contentScale     = ContentScale.Crop,
                        modifier         = Modifier.fillMaxSize()
                    )
                } else {
                    Text("🍱", fontSize = 30.sp)
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // Business name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.Store, null, Modifier.size(12.dp), tint = CC_Accent)
                    Text(
                        listing.businessName,
                        style = MaterialTheme.typography.labelMedium,
                        color = CC_Accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    listing.heroItem,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = CC_Dark
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.Restaurant, null, Modifier.size(11.dp), tint = CC_TextSub)
                    Text(
                        "Pickup  •  ${listing.mealsLeft} portions left",
                        style = MaterialTheme.typography.labelSmall,
                        color = CC_TextSub
                    )
                }
            }

            // Price chip
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "₹${listing.discountedPrice.toInt()}",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color      = CC_Mid
                )
                Text(
                    "₹${listing.originalPrice.toInt()}",
                    style          = MaterialTheme.typography.labelSmall,
                    color          = CC_TextSub,
                    textDecoration = TextDecoration.LineThrough
                )
            }
        }
    }
}

// ── Order summary card ────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(
    listing: Listing,
    quantity: Int,
    maxQty: Int,
    totalOriginal: Double,
    totalPayable: Double,
    savings: Double,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CC_Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Order Summary",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = CC_Dark
            )
            HorizontalDivider(color = CC_Divider)

            // Quantity stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Portions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CC_TextSub
                )
                QuantityStepper(
                    quantity    = quantity,
                    max         = maxQty,
                    onDecrement = onDecrement,
                    onIncrement = onIncrement
                )
            }

            HorizontalDivider(color = CC_Divider)

            // Price rows
            PriceRow(
                label = listing.heroItem,
                value = "₹${listing.originalPrice.toInt()}" +
                        if (quantity > 1) " × $quantity" else ""
            )
            PriceRow(
                label      = "Reskyu Discount",
                value      = "−₹${savings.toInt()}",
                valueColor = CC_Accent
            )

            HorizontalDivider(color = CC_Divider)

            // Total row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Total Payable",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = CC_Dark
                )
                Text(
                    "₹${totalPayable.toInt()}",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color      = CC_Mid
                )
            }

            // Savings highlight
            Surface(
                color    = CC_SaveBg,
                shape    = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint     = CC_SaveText,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        "You're saving ₹${savings.toInt()} on this order!",
                        style      = MaterialTheme.typography.labelMedium,
                        color      = CC_SaveText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── Impact card ───────────────────────────────────────────────────────────────

@Composable
private fun ImpactCard(quantity: Int) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CC_ImpactBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val co2 = String.format("%.1f", 2.5 * quantity)
            ImpactPill(emoji = "🌍", label = "${co2}kg CO₂")
            VerticalDivider(modifier = Modifier.height(32.dp), color = CC_Divider)
            ImpactPill(emoji = "🍱", label = "$quantity meal${if (quantity > 1) "s" else ""}")
            VerticalDivider(modifier = Modifier.height(32.dp), color = CC_Divider)
            ImpactPill(emoji = "💚", label = "Planet loves you")
        }
    }
}

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
private fun PriceRow(
    label: String,
    value: String,
    valueColor: Color = CC_Dark
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = CC_TextSub)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

@Composable
private fun ImpactPill(emoji: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(
            label,
            style      = MaterialTheme.typography.labelSmall,
            color      = CC_Mid,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 10.sp
        )
    }
}

// ── Quantity Stepper ──────────────────────────────────────────────────────────

@Composable
private fun QuantityStepper(
    quantity: Int,
    max: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilledIconButton(
            onClick  = onDecrement,
            enabled  = quantity > 1,
            modifier = Modifier.size(32.dp),
            shape    = CircleShape,
            colors   = IconButtonDefaults.filledIconButtonColors(
                containerColor         = CC_ImpactBg,
                contentColor           = CC_Mid,
                disabledContainerColor = Color(0xFFE5E7EB)
            )
        ) {
            Icon(Icons.Rounded.Remove, "Decrease", Modifier.size(16.dp))
        }

        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 44.dp)
                .border(1.dp, CC_Divider, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "$quantity",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color      = CC_Dark
            )
        }

        FilledIconButton(
            onClick  = onIncrement,
            enabled  = quantity < max,
            modifier = Modifier.size(32.dp),
            shape    = CircleShape,
            colors   = IconButtonDefaults.filledIconButtonColors(
                containerColor         = CC_Accent,
                contentColor           = Color.White,
                disabledContainerColor = Color(0xFFE5E7EB)
            )
        ) {
            Icon(Icons.Rounded.Add, "Increase", Modifier.size(16.dp))
        }
    }
}
