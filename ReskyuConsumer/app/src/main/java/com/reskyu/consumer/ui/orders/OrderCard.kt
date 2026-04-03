package com.reskyu.consumer.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reskyu.consumer.data.model.Claim
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * OrderCard
 *
 * A card composable representing a single claim in the My Orders list.
 * Displays the business name, food item, amount paid, and claim status badge.
 *
 * @param claim  The [Claim] data to render
 */
@Composable
fun OrderCard(claim: Claim) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = claim.businessName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                StatusBadge(status = claim.status)
            }

            Text(claim.heroItem, style = MaterialTheme.typography.bodyMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "₹${claim.amount.toInt()} paid",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatTimestamp(claim.timestamp.toDate().time),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val color = when (status) {
        "PENDING_PICKUP" -> MaterialTheme.colorScheme.tertiary
        "COMPLETED"      -> MaterialTheme.colorScheme.primary
        "DISPUTED"       -> MaterialTheme.colorScheme.error
        else             -> MaterialTheme.colorScheme.outline
    }
    val label = when (status) {
        "PENDING_PICKUP" -> "Pending"
        "COMPLETED"      -> "Done"
        "DISPUTED"       -> "Disputed"
        else             -> status
    }
    Text(
        text = label,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

private fun formatTimestamp(ms: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(ms))
}
