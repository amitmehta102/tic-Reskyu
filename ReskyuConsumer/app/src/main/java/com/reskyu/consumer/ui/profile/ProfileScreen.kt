package com.reskyu.consumer.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.consumer.ui.navigation.Screen

/**
 * ProfileScreen
 *
 * Displays the consumer's profile and their environmental impact stats.
 * Sections:
 *  - Avatar + Name + Email
 *  - Impact Stats card: Meals rescued, CO₂ saved, Money saved
 *  - Sign Out button
 *
 * @param navController  For post-signout navigation back to [Screen.Login]
 * @param viewModel      Injected [ProfileViewModel]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Profile") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                // Avatar placeholder
                Surface(
                    modifier = Modifier.size(88.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp))
                    }
                }

                Text(user?.name ?: "User", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(user?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Impact Stats card
                user?.impactStats?.let { stats ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Your Impact", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Divider()

                            ImpactStatRow(label = "🍱 Meals Rescued", value = "${stats.totalMealsRescued}")
                            ImpactStatRow(label = "🌍 CO₂ Saved", value = "${stats.co2SavedKg} kg")
                            ImpactStatRow(label = "💰 Money Saved", value = "₹${stats.moneySaved.toInt()}")
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(
                    onClick = {
                        viewModel.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out")
                }
            }
        }
    }
}

@Composable
private fun ImpactStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
    }
}
