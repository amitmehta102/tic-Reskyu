package com.reskyu.consumer.ui.confirmation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.consumer.ui.navigation.Screen

/**
 * ConfirmationScreen
 *
 * Shown after a successful payment and claim creation.
 * Displays a "ticket" card with:
 *  - Success checkmark icon
 *  - Business name & food item
 *  - Amount paid
 *  - Razorpay payment ID (for reference)
 *  - Pickup deadline time
 *
 * CTAs:
 *  - "View My Orders" → navigates to [Screen.MyOrders]
 *  - "Back to Home"  → navigates to [Screen.Home]
 *
 * @param claimId       The created claim document ID
 * @param navController For post-confirmation navigation
 * @param viewModel     Injected [ConfirmationViewModel]
 */
@Composable
fun ConfirmationScreen(
    claimId: String,
    navController: NavController,
    viewModel: ConfirmationViewModel = viewModel()
) {
    val ticketState by viewModel.ticketState.collectAsState()

    LaunchedEffect(claimId) {
        viewModel.loadTicket(claimId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Claim Confirmed! 🎉", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Show this at pickup", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(24.dp))

        // Ticket card
        ticketState?.let { ticket ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ticket.businessName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(ticket.heroItem, style = MaterialTheme.typography.bodyLarge)
                    Divider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Amount Paid"); Text("₹${ticket.amount.toInt()}", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Payment ID"); Text(ticket.paymentId, style = MaterialTheme.typography.labelSmall)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pick up"); Text(ticket.pickupByTime, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        } ?: CircularProgressIndicator()

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { navController.navigate(Screen.MyOrders.route) { popUpTo(Screen.Home.route) } },
            modifier = Modifier.fillMaxWidth()
        ) { Text("View My Orders") }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Back to Home") }
    }
}
