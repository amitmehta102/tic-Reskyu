package com.reskyu.merchant.ui.post_listing

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.merchant.data.model.DietaryTag
import com.reskyu.merchant.data.model.ListingForm
import com.reskyu.merchant.data.model.PublishState
import com.reskyu.merchant.data.model.UploadState
import com.reskyu.merchant.ui.components.LoadingOverlay
import com.reskyu.merchant.ui.navigation.Screen

// ── Brand palette ─────────────────────────────────────────────────────────────
private val GreenDark   = Color(0xFF0C1E13)
private val GreenDeep   = Color(0xFF163823)
private val GreenAccent = Color(0xFF52B788)

// ── Expiry quick-select options ───────────────────────────────────────────────
private val expiryOptions = listOf(30 to "30 min", 60 to "1 hr", 120 to "2 hrs", 240 to "4 hrs")

/**
 * Post New Listing screen — full form to publish a food-drop listing.
 *
 * Sections:
 *  1. 📷  Image picker
 *  2. Item name
 *  3. Dietary tag chips
 *  4. Meals stepper
 *  5. Pricing (original + discounted)
 *  6. Expiry quick-select
 *  7. Publish button
 */
@Composable
fun PostListingScreen(
    navController: NavController,
    viewModel: PostListingViewModel = viewModel()
) {
    val form         by viewModel.form.collectAsState()
    val publishState by viewModel.publishState.collectAsState()
    val uploadState  by viewModel.uploadState.collectAsState()

    // Gallery image picker launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.uploadImage(it.toString()) } }

    // Navigate to Live Listings once published successfully
    LaunchedEffect(publishState) {
        if (publishState is PublishState.Live) {
            navController.navigate(Screen.LIVE_LISTINGS) {
                popUpTo(Screen.POST_LISTING) { inclusive = true }
            }
            viewModel.resetPublishState()
        }
    }

    val canPublish = form.heroItem.isNotBlank() &&
            form.discountedPrice > 0.0 &&
            form.mealsAvailable >= 1 &&
            publishState !is PublishState.Publishing

    Scaffold(
        containerColor = Color(0xFFF2F8F4)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier        = Modifier.fillMaxSize(),
                contentPadding  = PaddingValues(bottom = 32.dp)
            ) {

                // ── Top bar ───────────────────────────────────────────────────
                item { PostListingHeader(onBack = { navController.navigateUp() }) }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {

                        // ① Image picker ──────────────────────────────────────
                        ImagePickerSection(
                            form        = form,
                            uploadState = uploadState,
                            onPick      = { imagePicker.launch("image/*") }
                        )

                        // ② Item name ─────────────────────────────────────────
                        FormSection(label = "Item Name") {
                            OutlinedTextField(
                                value         = form.heroItem,
                                onValueChange = { viewModel.updateForm { copy(heroItem = it) } },
                                placeholder   = { Text("e.g. Assorted Pastries") },
                                modifier      = Modifier.fillMaxWidth(),
                                singleLine    = true,
                                shape         = RoundedCornerShape(12.dp),
                                colors        = greenFieldColors()
                            )
                        }

                        // ③ Dietary tag ────────────────────────────────────────
                        FormSection(label = "Dietary Tag") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DietaryTag.entries.forEach { tag ->
                                    val selected = form.dietaryTag == tag
                                    val tagColor = when (tag) {
                                        DietaryTag.VEG     -> Color(0xFF2D6A4F)
                                        DietaryTag.NON_VEG -> Color(0xFFE63946)
                                        DietaryTag.VEGAN   -> Color(0xFF457B9D)
                                    }
                                    FilterChip(
                                        selected = selected,
                                        onClick  = { viewModel.updateForm { copy(dietaryTag = tag) } },
                                        label    = { Text(tag.name, fontSize = 13.sp) },
                                        colors   = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = tagColor.copy(alpha = 0.15f),
                                            selectedLabelColor     = tagColor,
                                            containerColor         = Color.White,
                                            labelColor             = Color(0xFF6B7280)
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled            = true,
                                            selected           = selected,
                                            selectedBorderColor = tagColor,
                                            borderColor        = Color(0xFFE5E7EB)
                                        )
                                    )
                                }
                            }
                        }

                        // ④ Meals stepper ─────────────────────────────────────
                        FormSection(label = "Meals Available") {
                            MealsStepper(
                                count       = form.mealsAvailable,
                                onDecrement = { if (form.mealsAvailable > 1) viewModel.updateForm { copy(mealsAvailable = mealsAvailable - 1) } },
                                onIncrement = { viewModel.updateForm { copy(mealsAvailable = mealsAvailable + 1) } }
                            )
                        }

                        // ⑤ Pricing ────────────────────────────────────────────
                        FormSection(label = "Pricing") {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value         = if (form.originalPrice > 0) form.originalPrice.toInt().toString() else "",
                                    onValueChange = { viewModel.updateForm { copy(originalPrice = it.toDoubleOrNull() ?: 0.0) } },
                                    label         = { Text("Original ₹") },
                                    modifier      = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine    = true,
                                    shape         = RoundedCornerShape(12.dp),
                                    colors        = greenFieldColors()
                                )
                                OutlinedTextField(
                                    value         = if (form.discountedPrice > 0) form.discountedPrice.toInt().toString() else "",
                                    onValueChange = { viewModel.updateForm { copy(discountedPrice = it.toDoubleOrNull() ?: 0.0) } },
                                    label         = { Text("Discounted ₹") },
                                    modifier      = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine    = true,
                                    shape         = RoundedCornerShape(12.dp),
                                    colors        = greenFieldColors()
                                )
                            }
                            // Discount % hint
                            if (form.originalPrice > 0 && form.discountedPrice > 0 && form.discountedPrice < form.originalPrice) {
                                val pct = ((1 - form.discountedPrice / form.originalPrice) * 100).toInt()
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text      = "✅  $pct% off",
                                    fontSize  = 12.sp,
                                    color     = GreenAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // ⑥ Expiry ────────────────────────────────────────────
                        FormSection(label = "Listing Expires In") {
                            ExpirySelector(
                                selected = form.expiresInMinutes,
                                onSelect = { viewModel.updateForm { copy(expiresInMinutes = it) } }
                            )
                        }

                        // Error message
                        if (publishState is PublishState.Error) {
                            Text(
                                text      = (publishState as PublishState.Error).message,
                                color     = Color(0xFFE63946),
                                fontSize  = 13.sp,
                                modifier  = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE63946).copy(alpha = 0.08f))
                                    .padding(12.dp)
                            )
                        }

                        // ⑦ Publish button ────────────────────────────────────
                        Button(
                            onClick  = {
                                viewModel.publishListing(
                                    merchantId   = "merchant_placeholder",
                                    businessName = "My Business",
                                    geoHash      = "wx4g"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            enabled  = canPublish,
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor         = GreenAccent,
                                contentColor           = Color.White,
                                disabledContainerColor = Color(0xFFB0D4C3),
                                disabledContentColor   = Color.White
                            )
                        ) {
                            Text(
                                text       = "🚀  Publish Listing",
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }
            }

            LoadingOverlay(isVisible = publishState is PublishState.Publishing)
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun PostListingHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(GreenDark, GreenDeep)))
            .statusBarsPadding()
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector        = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint               = Color.White,
                    modifier           = Modifier.size(20.dp)
                )
            }
            Text(
                text       = "Post New Listing",
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White
            )
        }
    }
}

// ── Image picker ──────────────────────────────────────────────────────────────

@Composable
private fun ImagePickerSection(
    form:        ListingForm,
    uploadState: UploadState,
    onPick:      () -> Unit
) {
    val hasImage  = form.imageUri.isNotEmpty()
    val borderClr = if (hasImage) GreenAccent else Color(0xFFD1D5DB)
    val bgClr     = if (hasImage) GreenAccent.copy(alpha = 0.06f) else Color(0xFFF9FAFB)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgClr)
            .border(width = 1.5.dp, color = borderClr, shape = RoundedCornerShape(16.dp))
            .clickable { onPick() },
        contentAlignment = Alignment.Center
    ) {
        when (uploadState) {
            is UploadState.Uploading -> CircularProgressIndicator(color = GreenAccent)
            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = if (hasImage) "✅" else "📷", fontSize = 40.sp)
                    Text(
                        text       = if (hasImage) "Photo selected" else "Add a photo",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (hasImage) GreenAccent else Color(0xFF374151)
                    )
                    Text(
                        text    = if (hasImage) "Tap to change" else "Tap to select from gallery",
                        fontSize = 12.sp,
                        color   = Color(0xFF9CA3AF)
                    )
                }
            }
        }
    }
}

// ── Form section wrapper ──────────────────────────────────────────────────────

@Composable
private fun FormSection(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text          = label,
            fontSize      = 12.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = Color(0xFF6B7280),
            letterSpacing = 0.8.sp
        )
        content()
    }
}

// ── Meals stepper ─────────────────────────────────────────────────────────────

@Composable
private fun MealsStepper(count: Int, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(
        modifier          = Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Decrement
        Box(
            modifier         = Modifier
                .size(48.dp)
                .clickable(enabled = count > 1, onClick = onDecrement)
                .background(if (count > 1) GreenAccent.copy(alpha = 0.10f) else Color(0xFFF9FAFB)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "−",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Medium,
                color      = if (count > 1) GreenDeep else Color(0xFFD1D5DB)
            )
        }

        // Count display
        Box(
            modifier         = Modifier
                .width(72.dp)
                .height(48.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "$count",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = GreenDeep
            )
        }

        // Increment
        Box(
            modifier         = Modifier
                .size(48.dp)
                .clickable(onClick = onIncrement)
                .background(GreenAccent.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "+",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Medium,
                color      = GreenDeep
            )
        }
    }
}

// ── Expiry selector ───────────────────────────────────────────────────────────

@Composable
private fun ExpirySelector(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        expiryOptions.forEach { (minutes, label) ->
            val isSelected = selected == minutes
            FilterChip(
                selected = isSelected,
                onClick  = { onSelect(minutes) },
                label    = { Text(label, fontSize = 13.sp) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GreenAccent.copy(alpha = 0.15f),
                    selectedLabelColor     = GreenDeep,
                    containerColor         = Color.White,
                    labelColor             = Color(0xFF6B7280)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled             = true,
                    selected            = isSelected,
                    selectedBorderColor = GreenAccent,
                    borderColor         = Color(0xFFE5E7EB)
                )
            )
        }
    }
}

// ── Text field colours ────────────────────────────────────────────────────────

@Composable
private fun greenFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = GreenAccent,
    unfocusedBorderColor = Color(0xFFE5E7EB),
    focusedLabelColor    = GreenAccent,
    unfocusedLabelColor  = Color(0xFF9CA3AF),
    focusedTextColor     = Color(0xFF111827),
    unfocusedTextColor   = Color(0xFF111827),
    cursorColor          = GreenAccent
)
