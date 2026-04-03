package com.reskyu.merchant.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.merchant.data.model.Merchant
import com.reskyu.merchant.data.model.SaveState
import com.reskyu.merchant.ui.components.LoadingOverlay
import com.reskyu.merchant.ui.components.MainBottomBar
import com.reskyu.merchant.ui.navigation.Screen

// ── Brand palette ─────────────────────────────────────────────────────────────
private val GreenDark   = Color(0xFF0C1E13)
private val GreenDeep   = Color(0xFF163823)
private val GreenAccent = Color(0xFF52B788)
private val GreenLight  = Color(0xFF95D5B2)

/**
 * Merchant profile screen — avatar header, editable business details, and sign-out.
 */
@Composable
fun MerchantProfileScreen(
    navController: NavController,
    viewModel: MerchantProfileViewModel = viewModel()
) {
    val merchant   by viewModel.merchant.collectAsState()
    val saveState  by viewModel.saveState.collectAsState()

    var closingTimeInput by remember { mutableStateOf("") }
    var isEditing        by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadProfile() }

    // Sync input when merchant loads
    LaunchedEffect(merchant) {
        merchant?.let { closingTimeInput = it.closingTime }
    }

    // Close edit mode after save succeeds
    LaunchedEffect(saveState) {
        if (saveState is SaveState.Saved) {
            isEditing = false
            viewModel.resetSaveState()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF2F8F4),
        bottomBar = { MainBottomBar(navController = navController, currentRoute = Screen.PROFILE) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // ── Avatar header ─────────────────────────────────────────────
                item { ProfileHeader(merchant = merchant) }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        // ── Business Details ──────────────────────────────────
                        SectionLabel("Business Details")

                        // Business name (read-only)
                        InfoRow(
                            emoji = "🏢",
                            label = "Business Name",
                            value = merchant?.businessName?.ifBlank { "—" } ?: "—"
                        )

                        // Closing time (editable)
                        ClosingTimeRow(
                            currentValue  = merchant?.closingTime?.ifBlank { "Not set" } ?: "Not set",
                            input         = closingTimeInput,
                            onInputChange = { closingTimeInput = it },
                            isEditing     = isEditing,
                            isSaving      = saveState is SaveState.Saving,
                            onEditClick   = { isEditing = true },
                            onSave        = {
                                viewModel.updateClosingTime(closingTimeInput)
                            },
                            onCancel      = {
                                closingTimeInput = merchant?.closingTime ?: ""
                                isEditing = false
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // ── Account ───────────────────────────────────────────
                        SectionLabel("Account")

                        AccountRow(emoji = "📞", label = "Support",             onClick = {})
                        AccountRow(emoji = "📄", label = "Terms of Service",     onClick = {})
                        AccountRow(emoji = "🔒", label = "Privacy Policy",       onClick = {})

                        Spacer(modifier = Modifier.height(8.dp))

                        // ── Sign Out ──────────────────────────────────────────
                        OutlinedButton(
                            onClick = {
                                viewModel.signOut()
                                navController.navigate(Screen.LOGIN) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape  = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFEF4444)
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    Color(0xFFEF4444).copy(alpha = 0.5f)
                                )
                            )
                        ) {
                            Text(
                                text       = "🚪  Sign Out",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // App version
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text     = "Reskyu Merchant · v1.0.0-dev",
                            fontSize = 11.sp,
                            color    = Color(0xFFD1D5DB),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            LoadingOverlay(isVisible = saveState is SaveState.Saving)
        }
    }
}

// ── Profile header ────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeader(merchant: Merchant?) {
    val initial    = merchant?.businessName?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    // trustScore is 0–100; scale to 0–5 stars
    val scoreInt   = merchant?.trustScore ?: 0
    val filled     = (scoreInt / 20).coerceIn(0, 5)
    val starsText  = "★".repeat(filled) + "☆".repeat(5 - filled)
    val scoreLabel = if (scoreInt > 0) "$scoreInt / 100" else ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(GreenDark, GreenDeep)))
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(GreenAccent.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = initial,
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White
                )
            }

            // Business name
            Text(
                text       = merchant?.businessName?.ifBlank { "Dev Merchant" } ?: "Dev Merchant",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )

            // Trust score stars
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text     = starsText,
                    fontSize = 16.sp,
                    color    = Color(0xFFFFD166)
                )
                if (scoreLabel.isNotEmpty()) {
                    Text(
                        text     = scoreLabel,
                        fontSize = 13.sp,
                        color    = GreenLight
                    )
                }
            }

            // Role badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text          = "Reskyu Partner",
                    fontSize      = 12.sp,
                    color         = GreenLight,
                    letterSpacing = 0.5.sp,
                    fontWeight    = FontWeight.Medium
                )
            }
        }
    }
}

// ── Section label ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        fontSize      = 12.sp,
        fontWeight    = FontWeight.SemiBold,
        color         = Color(0xFF6B7280),
        letterSpacing = 0.8.sp,
        modifier      = Modifier.padding(bottom = 2.dp)
    )
}

// ── Info row (read-only) ──────────────────────────────────────────────────────

@Composable
private fun InfoRow(emoji: String, label: String, value: String) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(text = emoji, fontSize = 18.sp)
                Column {
                    Text(
                        text     = label,
                        fontSize = 11.sp,
                        color    = Color(0xFF9CA3AF)
                    )
                    Text(
                        text       = value,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color      = Color(0xFF111827)
                    )
                }
            }
        }
    }
}

// ── Closing time row (editable) ───────────────────────────────────────────────

@Composable
private fun ClosingTimeRow(
    currentValue:  String,
    input:         String,
    onInputChange: (String) -> Unit,
    isEditing:     Boolean,
    isSaving:      Boolean,
    onEditClick:   () -> Unit,
    onSave:        () -> Unit,
    onCancel:      () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(text = "🕐", fontSize = 18.sp)
                    Column {
                        Text(
                            text     = "Closing Time",
                            fontSize = 11.sp,
                            color    = Color(0xFF9CA3AF)
                        )
                        Text(
                            text       = currentValue,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color      = Color(0xFF111827)
                        )
                    }
                }
                if (!isEditing) {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector        = Icons.Rounded.Edit,
                            contentDescription = "Edit closing time",
                            tint               = GreenAccent,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (isEditing) {
                OutlinedTextField(
                    value         = input,
                    onValueChange = onInputChange,
                    label         = { Text("e.g. 10:00 PM") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = RoundedCornerShape(10.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = GreenAccent,
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedLabelColor    = GreenAccent,
                        cursorColor          = GreenAccent
                    )
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick   = onCancel,
                        modifier  = Modifier.weight(1f),
                        shape     = RoundedCornerShape(10.dp)
                    ) { Text("Cancel") }

                    Button(
                        onClick  = onSave,
                        enabled  = !isSaving && input.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = GreenAccent)
                    ) {
                        Text(
                            text  = if (isSaving) "Saving…" else "Save",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ── Account list row ──────────────────────────────────────────────────────────

@Composable
private fun AccountRow(emoji: String, label: String, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick   = onClick
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(text = emoji, fontSize = 18.sp)
                Text(
                    text       = label,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color      = Color(0xFF111827)
                )
            }
            Icon(
                imageVector        = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint               = Color(0xFFD1D5DB),
                modifier           = Modifier.size(20.dp)
            )
        }
    }
}
