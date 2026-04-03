package com.reskyu.consumer.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.consumer.data.model.ImpactStats
import com.reskyu.consumer.ui.navigation.Screen

/**
 * ProfileScreen
 *
 * Full profile view showing:
 *  - Avatar (initials circle) + name + email
 *  - Impact stats grid (Meals / CO₂ / Money saved / Orders)
 *  - Settings rows (notifications, privacy, about, rate)
 *  - Sign Out button
 *
 * Lives inside the bottom nav shell — no back button.
 */
@Composable
fun ProfileScreen(
    navController: NavController,
    outerNavController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val user      by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
    ) {

        // ── Header ───────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(80.dp))
                } else {
                    // Avatar — initials circle
                    val initials = user?.name
                        ?.split(" ")
                        ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        ?.take(2)
                        ?.joinToString("") ?: "?"

                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = user?.name ?: "Reskyu User",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = user?.email ?: user?.phone ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

        // ── Impact Stats Grid ─────────────────────────────────────────────────
        val stats = user?.impactStats ?: ImpactStats()

        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Your Impact 🌍",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ImpactCard(
                    emoji = "🍱",
                    value = "${stats.totalMealsRescued}",
                    label = "Meals Rescued",
                    modifier = Modifier.weight(1f)
                )
                ImpactCard(
                    emoji = "🌿",
                    value = "${stats.co2SavedKg}kg",
                    label = "CO₂ Saved",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ImpactCard(
                    emoji = "💰",
                    value = "₹${stats.moneySaved.toInt()}",
                    label = "Money Saved",
                    modifier = Modifier.weight(1f)
                )
                ImpactCard(
                    emoji = "🏆",
                    value = "${stats.totalMealsRescued}",
                    label = "Total Orders",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))

        // ── Settings Rows ─────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            SettingsRow(icon = Icons.Rounded.Notifications, label = "Notification Preferences") {}
            SettingsRow(icon = Icons.Rounded.PrivacyTip,   label = "Privacy Policy") {}
            SettingsRow(icon = Icons.Rounded.Info,         label = "About Reskyu") {}
            SettingsRow(icon = Icons.Rounded.Star,         label = "Rate the App") {}
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))

        // ── Sign Out ──────────────────────────────────────────────────────────
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            OutlinedButton(
                onClick = {
                    viewModel.signOut()
                    outerNavController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    "Sign Out",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────────

@Composable
private fun ImpactCard(
    emoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
