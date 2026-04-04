package com.reskyu.merchant.ui.post_listing

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.reskyu.merchant.data.model.DietaryTag
import com.reskyu.merchant.data.model.ListingForm
import com.reskyu.merchant.data.model.ListingType
import com.reskyu.merchant.data.model.MysteryBoxType
import com.reskyu.merchant.data.model.PublishState
import com.reskyu.merchant.data.model.UploadState
import com.reskyu.merchant.ui.components.LoadingOverlay
import com.reskyu.merchant.ui.components.MainBottomBar
import com.reskyu.merchant.ui.navigation.Screen
import com.reskyu.merchant.ui.theme.RGreenAccent
import com.reskyu.merchant.ui.theme.RGreenDark
import com.reskyu.merchant.ui.theme.RGreenDeep
import com.reskyu.merchant.ui.theme.RScreenBg
import com.google.firebase.auth.FirebaseAuth

// ── Brand palette ─────────────────────────────────────────────────────────────
private val GreenDark     = RGreenDark
private val GreenDeep     = RGreenDeep
private val GreenAccent   = RGreenAccent
private val MysteryPurple = Color(0xFF6D3FD1)
private val MysteryAmber  = Color(0xFFF4A261)
private val ScreenBg      = RScreenBg

// ── Expiry quick-select options ───────────────────────────────────────────────
private val expiryOptions = listOf(30 to "30 min", 60 to "1 hr", 120 to "2 hrs", 240 to "4 hrs")

@Composable
fun PostListingScreen(
    navController: NavController,
    viewModel: PostListingViewModel = viewModel()
) {
    val form         by viewModel.form.collectAsState()
    val publishState by viewModel.publishState.collectAsState()
    val uploadState  by viewModel.uploadState.collectAsState()

    val isMysteryBox = form.listingType == ListingType.MYSTERY_BOX

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

    val canPublish = if (isMysteryBox) {
        form.heroItem.isNotBlank() &&
                form.discountedPrice > 0.0 &&
                form.priceRangeMin > 0.0 &&
                form.priceRangeMax >= form.priceRangeMin &&
                form.mealsAvailable >= 1 &&
                form.itemCount >= 1 &&
                publishState !is PublishState.Publishing
    } else {
        form.heroItem.isNotBlank() &&
                form.discountedPrice > 0.0 &&
                form.mealsAvailable >= 1 &&
                publishState !is PublishState.Publishing
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { viewModel.resetUploadState() }
    }

    Scaffold(containerColor = Color(0xFFF2F8F4)) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // ── Top bar ───────────────────────────────────────────────────
                item(key = "header") { PostListingHeader(onBack = { navController.navigateUp() }) }

                item(key = "form") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {

                        // ① Listing type toggle ────────────────────────────────
                        ListingTypeToggle(
                            selected = form.listingType,
                            onSelect = { viewModel.updateForm { copy(listingType = it) } }
                        )

                        // ② Image picker ──────────────────────────────────────
                        ImagePickerSection(
                            form        = form,
                            uploadState = uploadState,
                            onPick      = { imagePicker.launch("image/*") },
                            onRetry     = { viewModel.retryUpload() }
                        )

                        val focusManager = LocalFocusManager.current

                        if (isMysteryBox) {
                            // ── MYSTERY BOX FORM ─────────────────────────────

                            // ③ Box type ──────────────────────────────────────
                            FormSection(label = "BOX TYPE") {
                                BoxTypeSelector(
                                    selected = form.boxType,
                                    onSelect = { viewModel.updateForm { copy(boxType = it) } }
                                )
                            }

                            // ④ Hero / revealed item ──────────────────────────
                            FormSection(label = "REVEALED ITEM") {
                                OutlinedTextField(
                                    value           = form.heroItem,
                                    onValueChange   = { viewModel.updateForm { copy(heroItem = it) } },
                                    placeholder     = { Text("e.g. Pizza, Burger, Pasta…") },
                                    modifier        = Modifier.fillMaxWidth(),
                                    singleLine      = true,
                                    shape           = RoundedCornerShape(12.dp),
                                    colors          = greenFieldColors(),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    supportingText  = { Text("This item is shown to customers; the rest is a surprise", fontSize = 11.sp) }
                                )
                                // ⚡ Quick-select food name chips
                                QuickFoodChips(
                                    suggestions = MYSTERY_FOOD_SUGGESTIONS,
                                    onSelect    = { viewModel.updateForm { copy(heroItem = it) } }
                                )
                            }

                            // ⑤ Dietary tag ────────────────────────────────────
                            FormSection(label = "DIETARY TAG") {
                                DietaryTagSelector(
                                    selected = form.dietaryTag,
                                    onSelect = { viewModel.updateForm { copy(dietaryTag = it) } },
                                    includeMilk = true
                                )
                            }

                            // ⑥ Boxes available ───────────────────────────────
                            FormSection(label = "BOXES AVAILABLE") {
                                MealsStepper(
                                    count       = form.mealsAvailable,
                                    onDecrement = { if (form.mealsAvailable > 1) viewModel.updateForm { copy(mealsAvailable = mealsAvailable - 1) } },
                                    onIncrement = { viewModel.updateForm { copy(mealsAvailable = mealsAvailable + 1) } }
                                )
                            }

                            // ⑦ Items per box ─────────────────────────────────
                            FormSection(label = "ITEMS PER BOX") {
                                MealsStepper(
                                    count       = form.itemCount,
                                    onDecrement = { if (form.itemCount > 1) viewModel.updateForm { copy(itemCount = itemCount - 1) } },
                                    onIncrement = { viewModel.updateForm { copy(itemCount = itemCount + 1) } }
                                )
                            }

                            // ⑧ Content value range ───────────────────────────
                            FormSection(label = "CONTENT VALUE RANGE  (what's inside is worth)") {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value         = if (form.priceRangeMin > 0) form.priceRangeMin.toInt().toString() else "",
                                        onValueChange = { viewModel.updateForm { copy(priceRangeMin = it.toDoubleOrNull() ?: 0.0) } },
                                        label         = { Text("Min ₹") },
                                        modifier      = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                        singleLine    = true,
                                        shape         = RoundedCornerShape(12.dp),
                                        colors        = greenFieldColors()
                                    )
                                    OutlinedTextField(
                                        value         = if (form.priceRangeMax > 0) form.priceRangeMax.toInt().toString() else "",
                                        onValueChange = { viewModel.updateForm { copy(priceRangeMax = it.toDoubleOrNull() ?: 0.0) } },
                                        label         = { Text("Max ₹") },
                                        modifier      = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                        singleLine    = true,
                                        shape         = RoundedCornerShape(12.dp),
                                        colors        = greenFieldColors()
                                    )
                                }
                                if (form.priceRangeMin > 0 && form.priceRangeMax >= form.priceRangeMin) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "You're offering ₹${form.priceRangeMin.toInt()}–₹${form.priceRangeMax.toInt()} worth of food",
                                        fontSize = 12.sp,
                                        color    = GreenAccent,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // ⑨ Selling price ─────────────────────────────────
                            FormSection(label = "SELLING PRICE  (what customer pays)") {
                                OutlinedTextField(
                                    value           = if (form.discountedPrice > 0) form.discountedPrice.toInt().toString() else "",
                                    onValueChange   = { viewModel.updateForm { copy(discountedPrice = it.toDoubleOrNull() ?: 0.0) } },
                                    label           = { Text("Selling Price ₹") },
                                    modifier        = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    singleLine      = true,
                                    shape           = RoundedCornerShape(12.dp),
                                    colors          = greenFieldColors()
                                )
                                if (form.priceRangeMin > 0 && form.discountedPrice > 0) {
                                    val savings = form.priceRangeMin - form.discountedPrice
                                    if (savings > 0) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Customer saves at least ₹${savings.toInt()} 🎉",
                                            fontSize = 12.sp,
                                            color    = GreenAccent,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // ⑩ Expiry ────────────────────────────────────────
                            FormSection(label = "LISTING EXPIRES IN") {
                                ExpirySelector(
                                    selected = form.expiresInMinutes,
                                    onSelect = { viewModel.updateForm { copy(expiresInMinutes = it) } }
                                )
                            }

                            // ℹ️ Mystery Box info card ─────────────────────────
                            MysteryBoxInfoCard()

                        } else {
                            // ── REGULAR LISTING FORM ─────────────────────────

                            // ③ Item name
                            FormSection(label = "ITEM NAME") {
                                OutlinedTextField(
                                    value           = form.heroItem,
                                    onValueChange   = { viewModel.updateForm { copy(heroItem = it) } },
                                    placeholder     = { Text("e.g. Assorted Pastries") },
                                    modifier        = Modifier.fillMaxWidth(),
                                    singleLine      = true,
                                    shape           = RoundedCornerShape(12.dp),
                                    colors          = greenFieldColors(),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    )
                                )
                                // ⚡ Quick-select food name chips
                                QuickFoodChips(
                                    suggestions = REGULAR_FOOD_SUGGESTIONS,
                                    onSelect    = { viewModel.updateForm { copy(heroItem = it) } }
                                )
                            }

                            // ④ Dietary tag
                            FormSection(label = "DIETARY TAG") {
                                DietaryTagSelector(
                                    selected    = form.dietaryTag,
                                    onSelect    = { viewModel.updateForm { copy(dietaryTag = it) } },
                                    includeMilk = false
                                )
                            }

                            // ⑤ Meals stepper
                            FormSection(label = "MEALS AVAILABLE") {
                                MealsStepper(
                                    count       = form.mealsAvailable,
                                    onDecrement = { if (form.mealsAvailable > 1) viewModel.updateForm { copy(mealsAvailable = mealsAvailable - 1) } },
                                    onIncrement = { viewModel.updateForm { copy(mealsAvailable = mealsAvailable + 1) } }
                                )
                            }

                            // ⑥ Pricing
                            FormSection(label = "PRICING") {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value         = if (form.originalPrice > 0) form.originalPrice.toInt().toString() else "",
                                        onValueChange = { viewModel.updateForm { copy(originalPrice = it.toDoubleOrNull() ?: 0.0) } },
                                        label         = { Text("Original ₹") },
                                        modifier      = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                        singleLine    = true,
                                        shape         = RoundedCornerShape(12.dp),
                                        colors        = greenFieldColors()
                                    )
                                    OutlinedTextField(
                                        value           = if (form.discountedPrice > 0) form.discountedPrice.toInt().toString() else "",
                                        onValueChange   = { viewModel.updateForm { copy(discountedPrice = it.toDoubleOrNull() ?: 0.0) } },
                                        label           = { Text("Discounted ₹") },
                                        modifier        = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                        singleLine      = true,
                                        shape           = RoundedCornerShape(12.dp),
                                        colors          = greenFieldColors()
                                    )
                                }
                                if (form.originalPrice > 0 && form.discountedPrice > 0 && form.discountedPrice < form.originalPrice) {
                                    val pct = ((1 - form.discountedPrice / form.originalPrice) * 100).toInt()
                                    Spacer(Modifier.height(6.dp))
                                    Text("✅  $pct% off", fontSize = 12.sp, color = GreenAccent, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // ⑦ Expiry
                            FormSection(label = "LISTING EXPIRES IN") {
                                ExpirySelector(
                                    selected = form.expiresInMinutes,
                                    onSelect = { viewModel.updateForm { copy(expiresInMinutes = it) } }
                                )
                            }
                        }

                        // Error message
                        if (publishState is PublishState.Error) {
                            Text(
                                text     = (publishState as PublishState.Error).message,
                                color    = Color(0xFFE63946),
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE63946).copy(alpha = 0.08f))
                                    .padding(12.dp)
                            )
                        }

                        // Publish button
                        Button(
                            onClick  = {
                                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                viewModel.publishListing(merchantId = uid)
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            enabled  = canPublish,
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor         = if (isMysteryBox) MysteryPurple else GreenAccent,
                                contentColor           = Color.White,
                                disabledContainerColor = if (isMysteryBox) MysteryPurple.copy(alpha = 0.38f) else Color(0xFFB0D4C3),
                                disabledContentColor   = Color.White.copy(alpha = 0.6f)
                            )
                        ) {
                            if (publishState is PublishState.Publishing) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.5.dp)
                                    Text("Publishing…", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Text(
                                    text       = if (isMysteryBox) "🎁  Publish Mystery Box" else "🚀  Publish Listing",
                                    fontSize   = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }
                    }
                }
            }

            LoadingOverlay(isVisible = publishState is PublishState.Publishing)
        }
    }
}

// ── Listing Type Toggle ───────────────────────────────────────────────────────

@Composable
private fun ListingTypeToggle(selected: ListingType, onSelect: (ListingType) -> Unit) {
    val tabs = listOf(ListingType.REGULAR to "Regular Listing", ListingType.MYSTERY_BOX to "🎁 Mystery Box")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFE8F5EE))
            .padding(4.dp)
    ) {
        tabs.forEach { (type, label) ->
            val isSelected = selected == type
            val bgColor    = if (isSelected && type == ListingType.MYSTERY_BOX) MysteryPurple
                             else if (isSelected) GreenAccent
                             else Color.Transparent
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(bgColor)
                    .clickable { onSelect(type) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = label,
                    fontSize   = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (isSelected) Color.White else Color(0xFF6B7280)
                )
            }
        }
    }
}

// ── Box Type Selector ─────────────────────────────────────────────────────────

@Composable
private fun BoxTypeSelector(selected: MysteryBoxType, onSelect: (MysteryBoxType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // First row: first 3 types
            MysteryBoxType.entries.take(3).forEach { type ->
                BoxTypeChip(type = type, selected = selected == type, onSelect = onSelect, modifier = Modifier.weight(1f))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Second row: remaining types
            MysteryBoxType.entries.drop(3).forEach { type ->
                BoxTypeChip(type = type, selected = selected == type, onSelect = onSelect, modifier = Modifier.weight(1f))
            }
            // Fill remaining space if odd count
            if (MysteryBoxType.entries.drop(3).size % 2 != 0) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BoxTypeChip(type: MysteryBoxType, selected: Boolean, onSelect: (MysteryBoxType) -> Unit, modifier: Modifier = Modifier) {
    val bg     = if (selected) MysteryPurple.copy(alpha = 0.12f) else Color.White
    val border = if (selected) MysteryPurple else Color(0xFFE5E7EB)
    val text   = if (selected) MysteryPurple else Color(0xFF6B7280)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(12.dp))
            .clickable { onSelect(type) }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(type.emoji, fontSize = 22.sp)
            Text(type.label, fontSize = 10.sp, color = text, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, textAlign = TextAlign.Center)
        }
    }
}

// ── Dietary Tag Selector (shared for both modes) ──────────────────────────────

@Composable
private fun DietaryTagSelector(selected: DietaryTag, onSelect: (DietaryTag) -> Unit, includeMilk: Boolean) {
    val tags = if (includeMilk) DietaryTag.entries else DietaryTag.entries.filter { it != DietaryTag.CONTAINS_MILK }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { tag ->
            val isSelected = selected == tag
            val tagColor = when (tag) {
                DietaryTag.VEG           -> Color(0xFF2D6A4F)
                DietaryTag.NON_VEG       -> Color(0xFFE63946)
                DietaryTag.VEGAN         -> Color(0xFF457B9D)
                DietaryTag.CONTAINS_MILK -> Color(0xFF9B7FD4)
            }
            FilterChip(
                selected = isSelected,
                onClick  = { onSelect(tag) },
                label    = { Text("${tag.emoji} ${tag.label}", fontSize = 12.sp) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = tagColor.copy(alpha = 0.15f),
                    selectedLabelColor     = tagColor,
                    containerColor         = Color.White,
                    labelColor             = Color(0xFF6B7280)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled             = true,
                    selected            = isSelected,
                    selectedBorderColor = tagColor,
                    borderColor         = Color(0xFFE5E7EB)
                )
            )
        }
    }
}

// ── Mystery Box Info Card ─────────────────────────────────────────────────────

@Composable
private fun MysteryBoxInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = MysteryPurple.copy(alpha = 0.07f)),
        border   = CardDefaults.outlinedCardBorder().copy(width = 0.dp)
    ) {
        Row(
            modifier  = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.Top
        ) {
            Icon(
                imageVector        = Icons.Rounded.Info,
                contentDescription = null,
                tint               = MysteryPurple,
                modifier           = Modifier.size(20.dp).padding(top = 1.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "What's in a Mystery Box?",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MysteryPurple
                )
                Text(
                    "Customers can see the revealed item and the value range. The rest of the items are a delightful surprise — revealed only at pickup. This creates urgency and helps you clear surplus quickly.",
                    fontSize = 12.sp,
                    color    = Color(0xFF6B7280),
                    lineHeight = 17.sp
                )
            }
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
    onPick:      () -> Unit,
    onRetry:     () -> Unit = {}
) {
    val displayUri = form.imageUrl.ifBlank { form.imageUri }
    val hasImage   = displayUri.isNotEmpty()
    val isError    = uploadState is UploadState.Error

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (hasImage) Color.Transparent
                else if (isError) Color(0xFFE63946).copy(alpha = 0.05f)
                else Color(0xFFF9FAFB)
            )
            .border(
                width  = 1.5.dp,
                color  = when {
                    isError  -> Color(0xFFE63946)
                    hasImage -> GreenAccent
                    else     -> Color(0xFFD1D5DB)
                },
                shape  = RoundedCornerShape(16.dp)
            )
            .clickable { if (isError) onRetry() else onPick() },
        contentAlignment = Alignment.Center
    ) {
        if (hasImage) {
            AsyncImage(
                model              = displayUri,
                contentDescription = "Listing image",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            )
        }

        when (uploadState) {
            is UploadState.Uploading -> {
                Box(
                    modifier         = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                        Text("Uploading image…", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            is UploadState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("❌", fontSize = 32.sp)
                    Text("Upload failed", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE63946))
                    Text(
                        text     = (uploadState as UploadState.Error).message.take(60).let { if (it.length == 60) "$it…" else it },
                        fontSize  = 10.sp,
                        color     = Color(0xFF9CA3AF),
                        modifier  = Modifier.padding(horizontal = 16.dp)
                    )
                    Text("🔄  Tap to retry", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE63946))
                }
            }
            else -> {
                if (!hasImage) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📷", fontSize = 40.sp)
                        Text("Add a photo", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
                        Text("Tap to select from gallery", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text("Tap to change", fontSize = 11.sp, color = Color.White)
                    }
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
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = Color(0xFF6B7280),
            letterSpacing = 0.8.sp
        )
        content()
    }
}

// ── Meals / items stepper ─────────────────────────────────────────────────────

@Composable
private fun MealsStepper(count: Int, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(
        modifier          = Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier
                .size(48.dp)
                .clickable(enabled = count > 1, onClick = onDecrement)
                .background(if (count > 1) GreenAccent.copy(alpha = 0.10f) else Color(0xFFF9FAFB)),
            contentAlignment = Alignment.Center
        ) {
            Text("−", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = if (count > 1) GreenDeep else Color(0xFFD1D5DB))
        }
        Box(
            modifier         = Modifier.width(72.dp).height(48.dp).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text("$count", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GreenDeep)
        }
        Box(
            modifier         = Modifier
                .size(48.dp)
                .clickable(onClick = onIncrement)
                .background(GreenAccent.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = GreenDeep)
        }
    }
}

// ── Expiry selector ───────────────────────────────────────────────────────────

@Composable
private fun ExpirySelector(selected: Int, onSelect: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

// ── Quick food name suggestions ────────────────────────────────────────────────

private val REGULAR_FOOD_SUGGESTIONS = listOf(
    "Egg Rice", "Veg Biryani", "Chicken Biryani", "Butter Paneer Masala",
    "Butter Chicken", "Dal Makhani", "Chole Bhature", "Samosa",
    "Masala Dosa", "Idli Sambhar", "Paneer Tikka", "Chicken Curry",
    "Mutton Biryani", "Aloo Paratha", "Rajma Rice", "Pav Bhaji",
    "Veg Pulao", "Fried Rice", "Noodles", "Pasta",
    "Pizza", "Burger", "Sandwich", "Wrap"
)

private val MYSTERY_FOOD_SUGGESTIONS = listOf(
    "Pizza", "Burger", "Pasta", "Biryani", "Sushi",
    "Sandwich", "Wrap", "Tacos", "Noodles", "Salad",
    "Paneer Dish", "Chicken Dish", "Assorted Snacks", "Dessert", "Soup"
)

/**
 * Horizontally scrollable chip row for quick-filling item name fields.
 * Chips auto-select the current value to make it visually clear what's typed.
 */
@Composable
private fun QuickFoodChips(
    suggestions : List<String>,
    onSelect    : (String) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier            = Modifier.fillMaxWidth(),
        contentPadding      = PaddingValues(top = 8.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(suggestions) { name ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(GreenAccent.copy(alpha = 0.10f))
                    .border(1.dp, GreenAccent.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
                    .clickable { onSelect(name) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text       = name,
                    fontSize   = 12.sp,
                    color      = GreenDeep,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
