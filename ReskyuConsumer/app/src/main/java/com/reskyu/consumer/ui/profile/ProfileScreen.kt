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

// ── Profile screen palette (app-wide mint theme) ───────────────────────────────
private val PRBackground  = Color(0xFFEBF7EE)   // very light mint
private val PRSurface     = Color.White
private val PRText        = Color(0xFF133922)   // dark forest green
private val PRTextSub     = Color(0xFF5A7A65)   // muted sage
private val PRAccent      = Color(0xFF2DC653)   // Reskyu green
private val PRPriceGreen  = Color(0xFF1A9E45)   // darker price green
private val PRDivider     = Color(0xFFD4EDDA)   // light green divider
private val PRIconBg      = Color(0xFFE8F5E9)   // light green icon background
private val PRError       = Color(0xFFD32F2F)   // error red

/**
 * ProfileScreen — redesigned to match app-wide mint + forest-green theme
 *
 * Layout:
 *  ── White Surface header (title + subtitle, matches Home/Orders/Alerts)
 *  ── Avatar (initials circle) + name/email + Edit button on mint background
 *  ── Impact stats: 2×2 grid of white shadow cards
 *  ── Settings grouped sections with green icon circles
 *  ── Sign Out outlined button at the bottom
 *  ── Edit Profile bottom sheet (themed)
 *  ── Sign Out confirmation AlertDialog
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

    var showEditSheet     by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(isSaving) {
        if (isSaving == false) {
            snackbarHostState.showSnackbar("Profile saved ✓")
            viewModel.clearSaveState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = PRBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(PRBackground)
        ) {

            // ── White Surface header — PINNED (never scrolls) ─────────────────
            Surface(
                modifier        = Modifier.fillMaxWidth(),
                color           = PRSurface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        "Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = PRText
                    )
                    Text(
                        "Manage your account & impact",
                        style = MaterialTheme.typography.bodySmall,
                        color = PRText.copy(alpha = 0.55f)
                    )
                }
            }

            // ── Scrollable body below the pinned header ────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {

            // ── Avatar + identity card ─────────────────────────────────────────
            if (isLoading) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PRAccent)
                }
            } else {
                Surface(
                    modifier        = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    color           = PRSurface,
                    shape           = RoundedCornerShape(20.dp),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar initials circle
                        val initials = user?.name
                            ?.split(" ")
                            ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            ?.take(2)?.joinToString("") ?: "?"

                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(PRAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                initials,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            user?.name ?: "Reskyu User",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PRText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            user?.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = PRTextSub,
                            textAlign = TextAlign.Center
                        )
                        if (!user?.phone.isNullOrBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                user!!.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = PRTextSub
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        // Edit profile button
                        Button(
                            onClick = { showEditSheet = true },
                            shape  = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PRAccent,
                                contentColor   = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Rounded.Edit, null, Modifier.size(15.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Edit Profile", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Impact stats ───────────────────────────────────────────────────
            val stats = user?.impactStats ?: ImpactStats()

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "YOUR IMPACT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PRTextSub,
                    letterSpacing = 1.sp
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = PRDivider, thickness = 1.dp)
            }
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileImpactCard(
                    emoji    = "🍱",
                    value    = "${stats.totalMealsRescued}",
                    label    = "Meals Rescued",
                    modifier = Modifier.weight(1f)
                )
                ProfileImpactCard(
                    emoji    = "🌿",
                    value    = "${stats.co2SavedKg}kg",
                    label    = "CO₂ Saved",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileImpactCard(
                    emoji    = "💰",
                    value    = "₹${stats.moneySaved.toInt()}",
                    label    = "Money Saved",
                    modifier = Modifier.weight(1f)
                )
                ProfileImpactCard(
                    emoji    = "🏆",
                    value    = "${stats.totalMealsRescued}",
                    label    = "Total Orders",
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Settings: Preferences ──────────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            ProfileSectionLabel("PREFERENCES")
            Surface(
                modifier        = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color           = PRSurface,
                shape           = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp
            ) {
                Column {
                    ProfileSettingsRow(
                        icon     = Icons.Rounded.Notifications,
                        label    = "Notification Preferences",
                        sublabel = "Manage drop alerts & updates",
                        onClick  = { /* TODO */ }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 68.dp), color = PRDivider, thickness = 0.5.dp)
                    ProfileSettingsRow(
                        icon     = Icons.Rounded.LocationOn,
                        label    = "Location Settings",
                        sublabel = "Adjust your discovery radius",
                        onClick  = { /* TODO */ }
                    )
                }
            }

            // ── Settings: Legal ────────────────────────────────────────────────
            Spacer(Modifier.height(12.dp))
            ProfileSectionLabel("LEGAL & APP")
            Surface(
                modifier        = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color           = PRSurface,
                shape           = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp
            ) {
                Column {
                    ProfileSettingsRow(
                        icon    = Icons.Rounded.PrivacyTip,
                        label   = "Privacy Policy",
                        onClick = { /* TODO */ }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 68.dp), color = PRDivider, thickness = 0.5.dp)
                    ProfileSettingsRow(
                        icon     = Icons.Rounded.Info,
                        label    = "About Reskyu",
                        sublabel = "Version 1.0.0",
                        onClick  = { /* TODO */ }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 68.dp), color = PRDivider, thickness = 0.5.dp)
                    ProfileSettingsRow(
                        icon    = Icons.Rounded.Star,
                        label   = "Rate the App",
                        onClick = { /* TODO */ }
                    )
                }
            }

            // ── Sign Out ───────────────────────────────────────────────────────
            Spacer(Modifier.height(20.dp))
            Surface(
                modifier        = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color           = PRSurface,
                shape           = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp
            ) {
                ProfileSettingsRow(
                    icon     = Icons.Rounded.ExitToApp,
                    label    = "Sign Out",
                    iconBg   = Color(0xFFFFEBEE),
                    iconTint = PRError,
                    labelColor = PRError,
                    showChevron = false,
                    onClick  = { showSignOutDialog = true }
                )
            }

            Spacer(Modifier.height(32.dp))
            }   // end scrollable Column
        }       // end outer Column
    }           // end Scaffold

    // ── Edit Profile Bottom Sheet ──────────────────────────────────────────────
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
            onDismiss = { showEditSheet = false }
        )
    }

    // ── Sign Out Confirmation Dialog ───────────────────────────────────────────
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            containerColor   = PRSurface,
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFFFFEBEE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ExitToApp, null, tint = PRError, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text("Sign Out?", fontWeight = FontWeight.Bold, color = PRText)
            },
            text = {
                Text(
                    "You'll need to sign in again to access your orders and profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PRTextSub
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                        outerNavController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Main.route) { inclusive = true }
                        }
                    },
                    shape  = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PRError)
                ) { Text("Sign Out", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = PRTextSub)
                }
            }
        )
    }
}

// ── Section label ──────────────────────────────────────────────────────────────

@Composable
private fun ProfileSectionLabel(text: String) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = PRTextSub,
            letterSpacing = 0.8.sp
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = PRDivider, thickness = 1.dp)
    }
}

// ── Impact card ────────────────────────────────────────────────────────────────

@Composable
private fun ProfileImpactCard(
    emoji:    String,
    value:    String,
    label:    String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier        = modifier,
        color           = PRSurface,
        shape           = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, fontSize = 26.sp)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = PRPriceGreen
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = PRTextSub,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Settings row ───────────────────────────────────────────────────────────────

@Composable
private fun ProfileSettingsRow(
    icon:        ImageVector,
    label:       String,
    sublabel:    String?    = null,
    iconBg:      Color      = PRIconBg,
    iconTint:    Color      = PRText,
    labelColor:  Color      = PRText,
    showChevron: Boolean    = true,
    onClick:     () -> Unit
) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconTint
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = labelColor
                )
                if (!sublabel.isNullOrBlank()) {
                    Text(
                        sublabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = PRTextSub
                    )
                }
            }

            if (showChevron) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = PRTextSub
                )
            }
        }
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = PRSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Sheet handle area already provided by ModalBottomSheet
            Text(
                "Edit Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PRText
            )
            Text(
                "Update your display name and phone number.",
                style = MaterialTheme.typography.bodySmall,
                color = PRTextSub
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name") },
                leadingIcon = { Icon(Icons.Rounded.Person, null, tint = PRAccent) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                isError = saveError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = PRAccent,
                    focusedLabelColor    = PRAccent,
                    cursorColor          = PRAccent
                )
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone number") },
                leadingIcon = { Icon(Icons.Rounded.Phone, null, tint = PRAccent) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("+91 XXXXX XXXXX") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = PRAccent,
                    focusedLabelColor    = PRAccent,
                    cursorColor          = PRAccent
                )
            )

            if (saveError != null) {
                Text(
                    saveError,
                    style = MaterialTheme.typography.labelSmall,
                    color = PRError
                )
            }

            Spacer(Modifier.height(2.dp))

            Button(
                onClick = { onSave(name, phone) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape  = RoundedCornerShape(14.dp),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PRAccent,
                    contentColor   = Color.White
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Changes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = PRTextSub, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
