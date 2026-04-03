package com.reskyu.merchant.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reskyu.merchant.data.model.SurplusIqResult

/**
 * Displays the AI-powered SurplusIQ prediction as a Material 3 card.
 * Shown on the Dashboard screen when a prediction is available.
 *
 * @param result    The [SurplusIqResult] returned by SurplusIqRepository.
 * @param modifier  Optional Modifier for layout control.
 */
@Composable
fun SurplusIqBanner(
    result: SurplusIqResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🤖 SurplusIQ Prediction",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Predicted surplus today: ${result.predictedMeals} meals",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (result.reasoning.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = result.reasoning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}
