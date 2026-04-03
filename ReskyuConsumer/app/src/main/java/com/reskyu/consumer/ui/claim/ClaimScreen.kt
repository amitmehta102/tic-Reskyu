package com.reskyu.consumer.ui.claim

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.consumer.data.model.PaymentState
import com.reskyu.consumer.ui.components.LoadingOverlay
import com.reskyu.consumer.ui.components.ReskyuButton
import com.reskyu.consumer.ui.navigation.Screen

/**
 * ClaimScreen (Checkout UI)
 *
 * Shows an order summary before the user confirms payment via Razorpay.
 * Layout: Order summary card → breakdown of price saved → Pay button.
 *
 * Payment Flow:
 *  1. User taps "Pay ₹X" → [ClaimViewModel.initiatePayment] called
 *  2. [PaymentState.Processing] → Razorpay SDK opens payment sheet
 *  3. On success → ViewModel creates Claim in Firestore → [PaymentState.Success]
 *  4. Navigate to [Screen.Confirmation]
 *
 * ⚠️ Razorpay integration requires a running Activity (not just Composable) —
 *    you'll pass the Activity reference to the Razorpay SDK from MainActivity.
 *
 * @param listingId     The listing being claimed
 * @param navController For navigation to confirmation or back
 * @param viewModel     Injected [ClaimViewModel]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimScreen(
    listingId: String,
    navController: NavController,
    viewModel: ClaimViewModel = viewModel()
) {
    val listing by viewModel.listing.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()

    LaunchedEffect(listingId) {
        viewModel.loadListing(listingId)
    }

    // Navigate to confirmation on successful payment
    LaunchedEffect(paymentState) {
        if (paymentState is PaymentState.Success) {
            val claimId = (paymentState as PaymentState.Success).paymentId
            navController.navigate(Screen.Confirmation.createRoute(claimId)) {
                popUpTo(Screen.Home.route)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Confirm & Pay") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            listing?.let { l ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Order summary card
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Order Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Divider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(l.heroItem); Text("₹${l.discountedPrice.toInt()}")
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("You save", color = MaterialTheme.colorScheme.primary)
                                Text("₹${(l.originalPrice - l.discountedPrice).toInt()}", color = MaterialTheme.colorScheme.primary)
                            }
                            Divider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total", fontWeight = FontWeight.Bold)
                                Text("₹${l.discountedPrice.toInt()}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Error message
                    if (paymentState is PaymentState.Failed) {
                        Text(
                            text = (paymentState as PaymentState.Failed).reason,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    ReskyuButton(
                        text = "Pay ₹${l.discountedPrice.toInt()} via Razorpay",
                        onClick = { viewModel.initiatePayment(l) },
                        enabled = paymentState is PaymentState.Idle || paymentState is PaymentState.Failed
                    )
                }
            } ?: run {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            }
        }

        // Loading overlay during payment
        if (paymentState is PaymentState.Processing) {
            LoadingOverlay()
        }
    }
}
