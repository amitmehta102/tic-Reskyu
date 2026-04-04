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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.consumer.data.model.DietaryTag
import com.reskyu.consumer.data.model.ImpactStats
import com.reskyu.consumer.ui.navigation.Screen

private val PRBackground  = Color(0xFFF2F8F4)
private val PRSurface     = Color.White
private val PRText        = Color(0xFF0C1E13)
private val PRTextSub     = Color(0xFF5A7A65)
private val PRAccent      = Color(0xFF52B788)
private val PRPriceGreen  = Color(0xFF1F5235)
private val PRDivider     = Color(0xFFD4EDDA)
private val PRIconBg      = Color(0xFFE8F5E9)
private val PRError       = Color(0xFFD32F2F)
private val PRLight       = Color(0xFF95D5B2)
private val PRGrad        = listOf(Color(0xFF0C1E13), Color(0xFF163823), Color(0xFF1F5235))

private val NotifTags = listOf(
    DietaryTag.VEG     to "Veg 🥗",
    DietaryTag.NON_VEG to "Non-Veg 🍗",
    DietaryTag.VEGAN   to "Vegan 🌱",
    DietaryTag.BAKERY  to "Bakery 🥐",
    DietaryTag.SWEETS  to "Sweets 🍮"
)

private val RadiusOptions = listOf(2, 5, 10, 20, 50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    outerNavController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val user              by viewModel.user.collectAsState()
    val isLoading         by viewModel.isLoading.collectAsState()
    val isSaving          by viewModel.isSaving.collectAsState()
    val saveError         by viewModel.saveError.collectAsState()
    val privacyPolicy     by viewModel.privacyPolicy.collectAsState()
    val isPolicyLoading   by viewModel.isPolicyLoading.collectAsState()

    var showEditSheet           by remember { mutableStateOf(false) }
    var showSignOutDialog       by remember { mutableStateOf(false) }
    var showNotifPrefsSheet     by remember { mutableStateOf(false) }
    var showLocationSheet       by remember { mutableStateOf(false) }
    var showPrivacySheet        by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(isSaving) {
        if (isSaving == false) {
            snackbarHostState.showSnackbar("Profile saved ✓")
            viewModel.clearSaveState()
        }
    }

    Scaffold(
        snackbarHost        = { SnackbarHost(snackbarHostState) },
        containerColor      = PRBackground,
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(PRBackground)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation    = 8.dp,
                        shape        = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                        ambientColor = Color(0x401F5235),
                        spotColor    = Color(0x601F5235)
                    )
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Brush.verticalGradient(PRGrad))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 18.dp, bottom = 22.dp)
                ) {
                    Text(
                        "Profile 👤",
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Manage your account & impact",
                        style = MaterialTheme.typography.bodySmall,
                        color = PRLight
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {

                if (isLoading) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = PRAccent) }
                } else {
                    Surface(
                        modifier        = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        color           = PRSurface,
                        shape           = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            val initials = user?.name
                                ?.split(" ")
                                ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                ?.take(2)?.joinToString("") ?: "?"

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(PRAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    initials,
                                    style      = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    user?.name ?: "Reskyu User",
                                    style      = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color      = PRText,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis
                                )
                                Text(
                                    user?.email ?: "",
                                    style    = MaterialTheme.typography.bodySmall,
                                    color    = PRTextSub,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!user?.phone.isNullOrBlank()) {
                                    Text(
                                        user!!.phone,
                                        style    = MaterialTheme.typography.labelSmall,
                                        color    = PRTextSub,
                                        maxLines = 1
                                    )
                                }
                            }

                            FilledIconButton(
                                onClick = { showEditSheet = true },
                                modifier = Modifier.size(38.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = PRAccent,
                                    contentColor   = Color.White
                                )
                            ) {
                                Icon(Icons.Rounded.Edit, "Edit", Modifier.size(16.dp))
                            }
                        }
                    }
                }

                val stats = user?.impactStats ?: ImpactStats()

                Spacer(Modifier.height(8.dp))
                ProfileSectionLabel("YOUR IMPACT")
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileImpactCard("🍱", "${stats.totalMealsRescued}", "Meals Rescued", Modifier.weight(1f))
                    ProfileImpactCard("🌿", "${stats.co2SavedKg}kg", "CO₂ Saved", Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileImpactCard("💰", "₹${stats.moneySaved.toInt()}", "Money Saved", Modifier.weight(1f))
                    ProfileImpactCard("🏆", "${stats.totalMealsRescued}", "Total Orders", Modifier.weight(1f))
                }

                Spacer(Modifier.height(24.dp))
                ProfileSectionLabel("PREFERENCES")
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier        = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color           = PRSurface,
                    shape           = RoundedCornerShape(16.dp),
                    shadowElevation = 1.dp
                ) {
                    Column {
                        ProfileSettingsRow(
                            icon     = Icons.Rounded.Notifications,
                            label    = "Notification Preferences",
                            sublabel = buildString {
                                val prefs = user?.notificationPrefs ?: emptyList()
                                if (prefs.isEmpty()) append("All food categories")
                                else append(prefs.joinToString(", ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } })
                            },
                            onClick  = { showNotifPrefsSheet = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 68.dp), color = PRDivider, thickness = 0.5.dp)
                        ProfileSettingsRow(
                            icon     = Icons.Rounded.LocationOn,
                            label    = "Discovery Radius",
                            sublabel = "${user?.discoveryRadiusKm ?: 2} km — tap to discover more",
                            onClick  = { showLocationSheet = true }
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                ProfileSectionLabel("LEGAL & APP")
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier        = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color           = PRSurface,
                    shape           = RoundedCornerShape(16.dp),
                    shadowElevation = 1.dp
                ) {
                    Column {
                        ProfileSettingsRow(
                            icon    = Icons.Rounded.PrivacyTip,
                            label   = "Privacy Policy",
                            onClick = {
                                viewModel.loadPrivacyPolicy()
                                showPrivacySheet = true
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 68.dp), color = PRDivider, thickness = 0.5.dp)
                        ProfileSettingsRow(
                            icon     = Icons.Rounded.Info,
                            label    = "About Reskyu",
                            sublabel = "Version 1.0.0",
                            onClick  = { }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Surface(
                    modifier        = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color           = PRSurface,
                    shape           = RoundedCornerShape(16.dp),
                    shadowElevation = 1.dp
                ) {
                    ProfileSettingsRow(
                        icon        = Icons.Rounded.ExitToApp,
                        label       = "Sign Out",
                        iconBg      = Color(0xFFFFEBEE),
                        iconTint    = PRError,
                        labelColor  = PRError,
                        showChevron = false,
                        onClick     = { showSignOutDialog = true }
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

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

    if (showNotifPrefsSheet) {
        NotificationPrefsSheet(
            currentPrefs = user?.notificationPrefs ?: emptyList(),
            onSave       = { prefs ->
                viewModel.updateNotificationPrefs(prefs)
                showNotifPrefsSheet = false
            },
            onDismiss = { showNotifPrefsSheet = false }
        )
    }

    if (showLocationSheet) {
        LocationSettingsSheet(
            currentRadius = user?.discoveryRadiusKm ?: 2,
            onSave        = { radius ->
                viewModel.updateDiscoveryRadius(radius)
                showLocationSheet = false
            },
            onDismiss = { showLocationSheet = false }
        )
    }

    if (showPrivacySheet) {
        PrivacyPolicySheet(
            content   = privacyPolicy,
            isLoading = isPolicyLoading,
            onDismiss = { showPrivacySheet = false }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            containerColor   = PRSurface,
            icon = {
                Box(
                    modifier = Modifier.size(52.dp).background(Color(0xFFFFEBEE), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.ExitToApp, null, tint = PRError, modifier = Modifier.size(26.dp)) }
            },
            title = { Text("Sign Out?", fontWeight = FontWeight.Bold, color = PRText) },
            text  = {
                Text(
                    "You'll need to sign in again to access your orders and profile.",
                    style = MaterialTheme.typography.bodySmall, color = PRTextSub
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

@Composable
private fun ProfileSectionLabel(text: String) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text,
            style         = MaterialTheme.typography.labelSmall,
            fontWeight    = FontWeight.Bold,
            color         = PRTextSub,
            letterSpacing = 0.8.sp
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = PRDivider, thickness = 1.dp)
    }
}

@Composable
private fun ProfileImpactCard(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier        = modifier,
        color           = PRSurface,
        shape           = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(emoji, fontSize = 24.sp)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = PRPriceGreen)
            Text(label, style = MaterialTheme.typography.labelSmall, color = PRTextSub, textAlign = TextAlign.Center)
        }
    }
}

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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(iconBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, Modifier.size(19.dp), tint = iconTint) }

            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = labelColor)
                if (!sublabel.isNullOrBlank()) {
                    Text(sublabel, style = MaterialTheme.typography.labelSmall, color = PRTextSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (showChevron) {
                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(17.dp), tint = PRTextSub)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationPrefsSheet(
    currentPrefs: List<String>,
    onSave:       (List<String>) -> Unit,
    onDismiss:    () -> Unit
) {
    var selected by remember { mutableStateOf(currentPrefs.toSet()) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PRSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Notification Preferences 🔔", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PRText)
            Text(
                "Choose which food categories you want alerts for. Leave all unselected to receive everything.",
                style = MaterialTheme.typography.bodySmall, color = PRTextSub
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NotifTags.forEach { (tag, label) ->
                    val isOn = tag.name in selected
                    Surface(
                        onClick = {
                            selected = if (isOn) selected - tag.name else selected + tag.name
                        },
                        shape  = RoundedCornerShape(12.dp),
                        color  = if (isOn) PRAccent.copy(alpha = 0.12f) else Color(0xFFF5F5F5),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                                color = if (isOn) PRPriceGreen else PRText)
                            Switch(
                                checked  = isOn,
                                onCheckedChange = { on ->
                                    selected = if (on) selected + tag.name else selected - tag.name
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor       = Color.White,
                                    checkedTrackColor       = PRAccent,
                                    uncheckedThumbColor     = PRTextSub,
                                    uncheckedTrackColor     = Color(0xFFE0E0E0)
                                )
                            )
                        }
                    }
                }
            }

            if (selected.isEmpty()) {
                Surface(color = Color(0xFFE8F5EE), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "✅  You'll receive notifications for ALL food categories.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.labelMedium, color = PRPriceGreen, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Button(
                onClick  = { onSave(selected.toList()) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = PRAccent, contentColor = Color.White)
            ) { Text("Save Preferences", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) }

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = PRTextSub, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationSettingsSheet(
    currentRadius: Int,
    onSave:        (Int) -> Unit,
    onDismiss:     () -> Unit
) {
    var selected by remember { mutableStateOf(currentRadius) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PRSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Discovery Radius 📍", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PRText)
            Text(
                "Expand your search area to discover more restaurants. A larger radius may show listings farther away from you.",
                style = MaterialTheme.typography.bodySmall, color = PRTextSub
            )

            val rows = RadiusOptions.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { km ->
                            val isOn = km == selected
                            Surface(
                                onClick  = { selected = km },
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(12.dp),
                                color    = if (isOn) PRAccent else Color(0xFFF0F0F0)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "${km} km",
                                        style      = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color      = if (isOn) Color.White else PRText
                                    )
                                    Text(
                                        when (km) {
                                            2    -> "Nearby"
                                            5    -> "Wider"
                                            10   -> "City"
                                            20   -> "Regional"
                                            else -> "Extended"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isOn) Color.White.copy(0.8f) else PRTextSub
                                    )
                                }
                            }
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Surface(color = Color(0xFFE8F5EE), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Info, null, Modifier.size(15.dp), tint = PRPriceGreen)
                    Text(
                        "Pull to refresh on the home screen after changing your radius to discover more restaurants.",
                        style = MaterialTheme.typography.labelSmall, color = PRPriceGreen
                    )
                }
            }

            Button(
                onClick  = { onSave(selected) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = PRAccent, contentColor = Color.White)
            ) { Text("Save Radius", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) }

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = PRTextSub, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacyPolicySheet(
    content:   String?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = PRSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Privacy Policy 🔒", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PRText)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, "Close", tint = PRTextSub)
                }
            }
            HorizontalDivider(color = PRDivider)

            when {
                isLoading -> {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = PRAccent)
                            Text("Loading policy…", style = MaterialTheme.typography.bodySmall, color = PRTextSub)
                        }
                    }
                }
                content.isNullOrBlank() -> {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(32.dp)) {
                            Text("🔒", fontSize = 40.sp)
                            Text("Privacy policy not available yet.", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold, color = PRText, textAlign = TextAlign.Center)
                            Text("Please check back later or contact us at support@reskyu.app",
                                style = MaterialTheme.typography.bodySmall, color = PRTextSub, textAlign = TextAlign.Center)
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Text(content, style = MaterialTheme.typography.bodySmall, color = PRText, lineHeight = 20.sp)
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

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

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PRSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Edit Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PRText)
            Text("Update your display name and phone number.", style = MaterialTheme.typography.bodySmall, color = PRTextSub)

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Display Name") },
                leadingIcon = { Icon(Icons.Rounded.Person, null, tint = PRAccent) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(), isError = saveError != null,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PRAccent, focusedLabelColor = PRAccent, cursorColor = PRAccent)
            )
            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                label = { Text("Phone number") },
                leadingIcon = { Icon(Icons.Rounded.Phone, null, tint = PRAccent) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("+91 XXXXX XXXXX") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PRAccent, focusedLabelColor = PRAccent, cursorColor = PRAccent)
            )
            if (saveError != null) Text(saveError, style = MaterialTheme.typography.labelSmall, color = PRError)

            Spacer(Modifier.height(2.dp))
            Button(
                onClick = { onSave(name, phone) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp), enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = PRAccent, contentColor = Color.White)
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Save Changes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = PRTextSub, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
