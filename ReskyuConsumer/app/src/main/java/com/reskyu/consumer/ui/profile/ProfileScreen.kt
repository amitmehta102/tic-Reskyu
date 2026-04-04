package com.reskyu.consumer.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.consumer.data.model.ImpactStats
import com.reskyu.consumer.ui.navigation.Screen

/**
 * ProfileScreen — functional version
 *
 * Features:
 *  ── Avatar (initials) + name/email + ✏️ Edit button
 *  ── Edit Profile bottom sheet (name + phone, Save & close)
 *  ── Sign Out with confirmation dialog
 *  ── Impact stats (2×2 grid)
 *  ── Settings rows (notifications, privacy, about, rate)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    outerNavController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val user      by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving  by viewModel.isSaving.collectAsState()
    val saveError by viewModel.saveError.collectAsState()

    var showEditSheet    by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    // Show success snackbar when isSaving flips false
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(isSaving) {
        if (isSaving == false) {
            snackbarHostState.showSnackbar("Profile saved ✓")
            viewModel.clearSaveState()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {

            // ── Avatar section ────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()) {
                if (isLoading) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar circle with initials
                        val initials = user?.name
                            ?.split(" ")
                            ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            ?.take(2)?.joinToString("") ?: "?"

                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                initials,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            user?.name ?: "Reskyu User",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            user?.email ?: user?.phone ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (!user?.phone.isNullOrBlank()) {
                            Text(
                                user!!.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // ── Edit profile button ───────────────────────────────
                        FilledTonalButton(
                            onClick = { showEditSheet = true },
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Edit Profile", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── Impact stats ──────────────────────────────────────────────────
            val stats = user?.impactStats ?: ImpactStats()

            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Your Impact 🌍",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ImpactCard("🍱", "${stats.totalMealsRescued}", "Meals Rescued", Modifier.weight(1f))
                    ImpactCard("🌿", "${stats.co2SavedKg}kg",     "CO₂ Saved",     Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ImpactCard("💰", "₹${stats.moneySaved.toInt()}", "Money Saved",  Modifier.weight(1f))
                    ImpactCard("🏆", "${stats.totalMealsRescued}",    "Total Orders", Modifier.weight(1f))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── Settings rows ──────────────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            SettingsSection(title = "Preferences") {
                SettingsRow(
                    icon  = Icons.Rounded.Notifications,
                    label = "Notification Preferences",
                    sublabel = "Manage drop alerts & updates",
                    onClick = { /* TODO */ }
                )
                SettingsRow(
                    icon  = Icons.Rounded.LocationOn,
                    label = "Location Settings",
                    sublabel = "Adjust your discovery radius",
                    onClick = { /* TODO */ }
                )
            }

            Spacer(Modifier.height(4.dp))
            SettingsSection(title = "Legal") {
                SettingsRow(
                    icon     = Icons.Rounded.PrivacyTip,
                    label    = "Privacy Policy",
                    onClick  = { /* TODO */ }
                )
                SettingsRow(
                    icon    = Icons.Rounded.Info,
                    label   = "About Reskyu",
                    sublabel = "Version 1.0.0",
                    onClick = { /* TODO */ }
                )
                SettingsRow(
                    icon    = Icons.Rounded.Star,
                    label   = "Rate the App",
                    onClick = { /* TODO */ }
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── Sign Out ──────────────────────────────────────────────────────
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                OutlinedButton(
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.error.copy(alpha = .5f)
                    )
                ) {
                    Icon(Icons.Rounded.ExitToApp, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Edit Profile Bottom Sheet ─────────────────────────────────────────────
    if (showEditSheet) {
        EditProfileSheet(
            currentName  = user?.name  ?: "",
            currentPhone = user?.phone ?: "",
            isSaving     = isSaving == true,
            saveError    = saveError,
            onSave       = { name, phone ->
                viewModel.updateProfile(name, phone)
                showEditSheet = false
            },
            onDismiss    = { showEditSheet = false }
        )
    }

    // ── Sign Out Confirmation Dialog ──────────────────────────────────────────
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            icon = { Icon(Icons.Rounded.ExitToApp, null) },
            title = { Text("Sign Out?") },
            text  = { Text("You'll need to sign in again to access your orders and profile.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                        outerNavController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Main.route) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Sign Out") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Edit Profile Bottom Sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(
    currentName:  String,
    currentPhone: String,
    isSaving:     Boolean,
    saveError:    String?,
    onSave:       (name: String, phone: String) -> Unit,
    onDismiss:    () -> Unit
) {
    var name  by remember { mutableStateOf(currentName) }
    var phone by remember { mutableStateOf(currentPhone) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Edit Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name") },
                leadingIcon = { Icon(Icons.Rounded.Person, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                isError = saveError != null
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone number") },
                leadingIcon = { Icon(Icons.Rounded.Phone, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("+91 XXXXX XXXXX") }
            )

            if (saveError != null) {
                Text(
                    saveError,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = { onSave(name, phone) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Changes", style = MaterialTheme.typography.labelLarge)
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Cancel") }
        }
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f),
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        content()
    }
}

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
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon:     ImageVector,
    label:    String,
    sublabel: String? = null,
    onClick:  () -> Unit
) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!sublabel.isNullOrBlank()) {
                    Text(
                        sublabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
