package com.reskyu.merchant.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reskyu.merchant.data.model.MerchantClaim
import java.text.SimpleDateFormat
import java.util.*

/**
 * A single claim card in the Order Management list.
 * Shows claim details and Complete / Dispute action buttons.
 *
 * @param claim        The [MerchantClaim] to display.
 * @param onComplete   Called when merchant confirms pickup.
 * @param onDispute    Called when merchant raises a dispute.
 */
@Composable
fun ClaimCard(
    claim: MerchantClaim,
    onComplete: (String) -> Unit,
    onDispute: (String) -> Unit
) {
    val dateFormatted = remember(claim.timestamp) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            .format(Date(claim.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = claim.heroItem,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "₹${claim.amount.toInt()} · $dateFormatted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Payment: ${claim.paymentId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (claim.status == "PENDING_PICKUP") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onComplete(claim.id) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Complete") }

                    OutlinedButton(
                        onClick = { onDispute(claim.id) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Dispute") }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(onClick = {}, label = { Text(claim.status) })
            }
        }
    }
}

@Composable
private fun remember(timestamp: Long, block: () -> String): String {
    return androidx.compose.runtime.remember(timestamp) { block() }
}
