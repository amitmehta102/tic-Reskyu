package com.reskyu.merchant.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.StarHalf
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
import com.reskyu.merchant.ui.theme.RGreenAccent
import com.reskyu.merchant.ui.theme.RGreenDark
import com.reskyu.merchant.ui.theme.RGreenDeep
import com.reskyu.merchant.ui.theme.RGreenLight
import com.reskyu.merchant.ui.theme.RScreenBg

// ── Brand palette ─────────────────────────────────────────────────────────────
private val GreenDark   = RGreenDark
private val GreenDeep   = RGreenDeep
private val GreenAccent = RGreenAccent
private val GreenLight  = RGreenLight

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
        containerColor = RScreenBg,
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
    val initial = merchant?.businessName?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    // ── Rating calculation ─────────────────────────────────────────────────
    val ratingCount = merchant?.ratingCount ?: 0
    val ratingSum   = merchant?.ratingSum   ?: 0
    val hasRating   = ratingCount >= 5
    val avgRating   = if (hasRating) ratingSum.toFloat() / ratingCount else 0f

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
            // ── Avatar circle ──────────────────────────────────────────────
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

            // ── Rating OR "New Restaurant" ─────────────────────────────────
            if (hasRating) {
                RatingStars(avg = avgRating, count = ratingCount)
            } else {
                NewRestaurantBadge(ratingCount = ratingCount)
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

/**
 * 5-star rating row using Material vector icons.
 * Each star is evaluated individually:
 *   fill ≥ 0.75 → filled   (Star)
 *   fill ≥ 0.25 → half     (StarHalf)
 *   fill < 0.25 → empty    (StarBorder)
 *
 * This recomposes automatically whenever [avg] or [count] change.
 */
@Composable
private fun RatingStars(avg: Float, count: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // ── 5 individual star icons ────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (i in 1..5) {
                    val fill = (avg - (i - 1)).coerceIn(0f, 1f)
                    val icon = when {
                        fill >= 0.75f -> Icons.Rounded.Star        // full
                        fill >= 0.25f -> Icons.Rounded.StarHalf    // half
                        else          -> Icons.Rounded.StarBorder  // empty
                    }
                    Icon(
                        imageVector        = icon,
                        contentDescription = null,
                        tint               = Color(0xFFFFD166),
                        modifier           = Modifier.size(22.dp)
                    )
                }
            }

            // ── Numeric average ────────────────────────────────────────────
            Text(
                text       = String.format("%.1f", avg),
                fontSize   = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )
        }

        Text(
            text     = "Based on $count ratings",
            fontSize = 11.sp,
            color    = GreenLight.copy(alpha = 0.75f)
        )
    }
}

/** "New Restaurant" pill shown when ratingCount < 5. */
@Composable
private fun NewRestaurantBadge(ratingCount: Int) {
    val remaining = (5 - ratingCount).coerceAtLeast(0)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFFFD166).copy(alpha = 0.18f))
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                text          = "⭐ New Restaurant",
                fontSize      = 12.sp,
                color         = Color(0xFFFFD166),
                letterSpacing = 0.3.sp,
                fontWeight    = FontWeight.SemiBold
            )
        }
        Text(
            text     = if (ratingCount == 0) "Get your first rating from customers"
                       else "$remaining more ${if (remaining == 1) "rating" else "ratings"} to show avg score",
            fontSize = 11.sp,
            color    = GreenLight.copy(alpha = 0.65f)
        )
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
