package com.reskyu.consumer.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * ReskyuButton
 *
 * Standardized primary CTA button used throughout the app.
 * Applies the app's primary color, rounded corners, and consistent height.
 *
 * @param text      Button label text
 * @param onClick   Click callback
 * @param modifier  Optional layout modifier
 * @param enabled   Whether the button is interactive (default: true)
 */
@Composable
fun ReskyuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
