package com.reskyu.merchant.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.merchant.data.model.MerchantDraft
import com.reskyu.merchant.data.model.SaveState
import com.reskyu.merchant.ui.components.LoadingOverlay
import com.reskyu.merchant.ui.navigation.Screen
import kotlinx.coroutines.launch

// ── Brand palette ─────────────────────────────────────────────────────────────
private val GreenDark   = Color(0xFF0C1E13)
private val GreenDeep   = Color(0xFF163823)
private val GreenMid    = Color(0xFF1F5235)
private val GreenAccent = Color(0xFF52B788)
private val GreenLight  = Color(0xFF95D5B2)

// ── Per-page static content ───────────────────────────────────────────────────
private data class PageMeta(val emoji: String, val title: String, val subtitle: String)

private val PAGE_META = listOf(
    PageMeta("🏢", "What's your\nbusiness name?",   "Set up your Reskyu Partner profile"),
    PageMeta("📍", "Where are\nyou located?",        "We'll show you to nearby customers"),
    PageMeta("🕐", "What time do\nyou close?",        "Help customers know when to arrive"),
    PageMeta("✅", "You're all set!",                 "Review your details and go live")
)

private val CLOSING_PRESETS = listOf("6 PM", "8 PM", "10 PM", "12 AM")

/**
 * Multi-step onboarding (4 pages) using [HorizontalPager].
 *
 * **Bug fixed:** `animateScrollToPage` is a suspend function.
 * It now runs inside `rememberCoroutineScope().launch { }`.
 */
@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = viewModel()
) {
    val draft     by viewModel.draft.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    val pagerState    = rememberPagerState(pageCount = { PAGE_META.size })
    val coroutineScope = rememberCoroutineScope()

    var businessNameInput by remember { mutableStateOf("") }
    var closingTimeInput  by remember { mutableStateOf("") }
    var locationPicked    by remember { mutableStateOf(false) }

    // Navigate to Dashboard once onboarding save succeeds
    LaunchedEffect(saveState) {
        if (saveState is SaveState.Saved) {
            navController.navigate(Screen.DASHBOARD) {
                popUpTo(Screen.ONBOARDING) { inclusive = true }
            }
        }
    }

    // Per-page validation gate for "Next"
    val canAdvance = when (pagerState.currentPage) {
        0    -> businessNameInput.isNotBlank()
        1    -> true                             // location optional for now
        2    -> closingTimeInput.isNotBlank()
        else -> true
    }

    // Advance: save step data then scroll to next page (or trigger final save)
    fun advance() {
        coroutineScope.launch {
            when (pagerState.currentPage) {
                0 -> viewModel.updateBusinessName(businessNameInput)
                2 -> viewModel.updateClosingTime(closingTimeInput)
                3 -> {
                    // Fire-and-forget save (works when real auth is wired up)
                    viewModel.completeOnboarding()
                    // Navigate immediately — don't wait for SaveState.Saved
                    navController.navigate(Screen.DASHBOARD) {
                        popUpTo(Screen.ONBOARDING) { inclusive = true }
                    }
                    return@launch
                }
                else -> Unit
            }
            val next = pagerState.currentPage + 1
            if (next < PAGE_META.size) {
                pagerState.animateScrollToPage(next)
            }
        }
    }

    // Go back one page
    fun goBack() {
        coroutineScope.launch {
            val prev = pagerState.currentPage - 1
            if (prev >= 0) pagerState.animateScrollToPage(prev)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GreenDark, GreenDeep, GreenMid)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {

            // ── Top: branding + step dots ─────────────────────────────────────
            Column(
                modifier                = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment     = Alignment.CenterHorizontally,
                verticalArrangement     = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text          = "reskyu",
                    fontSize      = 12.sp,
                    color         = GreenLight,
                    letterSpacing = 4.sp,
                    fontWeight    = FontWeight.Medium
                )
                StepDots(
                    totalPages  = PAGE_META.size,
                    currentPage = pagerState.currentPage
                )
            }

            // ── Pager content ─────────────────────────────────────────────────
            HorizontalPager(
                state          = pagerState,
                modifier       = Modifier.weight(1f),
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    0 -> StepBusinessName(
                        value         = businessNameInput,
                        onValueChange = { businessNameInput = it },
                        meta          = PAGE_META[0]
                    )
                    1 -> StepLocation(
                        locationPicked  = locationPicked,
                        onLocationPick  = {
                            // Placeholder coords (New Delhi) until GPS is integrated
                            viewModel.updateLocation(28.6139, 77.2090, "tt3tgk")
                            locationPicked = true
                        },
                        meta = PAGE_META[1]
                    )
                    2 -> StepClosingTime(
                        value         = closingTimeInput,
                        onValueChange = { closingTimeInput = it },
                        meta          = PAGE_META[2]
                    )
                    3 -> StepConfirm(draft = draft, meta = PAGE_META[3])
                }
            }

            // ── Nav buttons ───────────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = if (pagerState.currentPage > 0)
                    Arrangement.SpaceBetween else Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick  = { goBack() },
                        shape    = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(52.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border   = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(Color.White.copy(alpha = 0.4f))
                        )
                    ) {
                        Text("← Back", fontSize = 15.sp)
                    }
                }

                Button(
                    onClick  = { advance() },
                    enabled  = canAdvance && saveState !is SaveState.Saving,
                    shape    = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(52.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = GreenAccent,
                        contentColor           = Color.White,
                        disabledContainerColor = GreenAccent.copy(alpha = 0.30f),
                        disabledContentColor   = Color.White.copy(alpha = 0.5f)
                    )
                ) {
                    val label = if (pagerState.currentPage == PAGE_META.size - 1)
                        "🚀  Launch" else "Next  →"
                    Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        LoadingOverlay(isVisible = saveState is SaveState.Saving)
    }
}

// ── Animated step dots ────────────────────────────────────────────────────────

@Composable
private fun StepDots(totalPages: Int, currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        repeat(totalPages) { i ->
            val width by animateDpAsState(
                targetValue = if (i == currentPage) 24.dp else 8.dp,
                label       = "dot_width_$i"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(
                        if (i == currentPage) Color.White
                        else Color.White.copy(alpha = 0.28f)
                    )
            )
        }
    }
}

// ── Shared page shell ─────────────────────────────────────────────────────────

@Composable
private fun PageShell(
    meta:    PageMeta,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Hero emoji
        Text(text = meta.emoji, fontSize = 72.sp)

        // Title + subtitle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text       = meta.title,
                fontSize   = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                textAlign  = TextAlign.Center,
                lineHeight = 36.sp
            )
            Text(
                text      = meta.subtitle,
                fontSize  = 15.sp,
                color     = Color.White.copy(alpha = 0.58f),
                textAlign = TextAlign.Center
            )
        }

        // Page-specific input content
        content()
    }
}

// ── Step 0: Business Name ─────────────────────────────────────────────────────

@Composable
private fun StepBusinessName(value: String, onValueChange: (String) -> Unit, meta: PageMeta) {
    PageShell(meta = meta) {
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            label         = { Text("e.g. Green Bites Café") },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            shape         = RoundedCornerShape(14.dp),
            colors        = darkFieldColors()
        )
    }
}

// ── Step 1: Location ──────────────────────────────────────────────────────────

@Composable
private fun StepLocation(locationPicked: Boolean, onLocationPick: () -> Unit, meta: PageMeta) {
    PageShell(meta = meta) {
        if (locationPicked) {
            // Confirmed state
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GreenAccent.copy(alpha = 0.16f))
                    .padding(16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("✅", fontSize = 24.sp)
                Column {
                    Text(
                        "Location confirmed",
                        fontWeight = FontWeight.SemiBold,
                        color      = GreenAccent
                    )
                    Text(
                        "New Delhi, India (placeholder)",
                        fontSize = 12.sp,
                        color    = Color.White.copy(alpha = 0.55f)
                    )
                }
            }
        } else {
            Button(
                onClick  = onLocationPick,
                shape    = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.12f),
                    contentColor   = Color.White
                )
            ) {
                Icon(
                    imageVector        = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use My Location", fontWeight = FontWeight.SemiBold)
            }
            Text(
                text      = "Enables geo-based listing discovery.\nFull GPS integration coming soon.",
                fontSize  = 12.sp,
                color     = Color.White.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Step 2: Closing Time ──────────────────────────────────────────────────────

@Composable
private fun StepClosingTime(value: String, onValueChange: (String) -> Unit, meta: PageMeta) {
    PageShell(meta = meta) {
        // Quick-pick chips
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CLOSING_PRESETS.forEach { preset ->
                val isSelected = value == preset
                FilterChip(
                    selected = isSelected,
                    onClick  = { onValueChange(preset) },
                    label    = { Text(preset, fontSize = 12.sp) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GreenAccent.copy(alpha = 0.22f),
                        selectedLabelColor     = Color.White,
                        containerColor         = Color.White.copy(alpha = 0.08f),
                        labelColor             = Color.White.copy(alpha = 0.65f)
                    ),
                    border   = FilterChipDefaults.filterChipBorder(
                        enabled             = true,
                        selected            = isSelected,
                        selectedBorderColor = GreenAccent,
                        borderColor         = Color.White.copy(alpha = 0.20f)
                    )
                )
            }
        }

        // Custom time input
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            label         = { Text("Or enter custom time") },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            shape         = RoundedCornerShape(14.dp),
            colors        = darkFieldColors()
        )
    }
}

// ── Step 3: Confirm ───────────────────────────────────────────────────────────

@Composable
private fun StepConfirm(draft: MerchantDraft, meta: PageMeta) {
    PageShell(meta = meta) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ConfirmRow("🏢", "Business Name", draft.businessName.ifBlank { "Not entered" })
            ConfirmRow("📍", "Location",      if (draft.lat != 0.0) "New Delhi, India" else "Not set")
            ConfirmRow("🕐", "Closing Time",  draft.closingTime.ifBlank { "Not entered" })
        }
    }
}

@Composable
private fun ConfirmRow(emoji: String, label: String, value: String) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text          = label,
                fontSize      = 11.sp,
                color         = GreenLight,
                letterSpacing = 0.5.sp
            )
            Text(
                text       = value,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White
            )
        }
    }
}

// ── Text field colours for dark background ─────────────────────────────────────

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor     = GreenAccent,
    unfocusedBorderColor   = Color.White.copy(alpha = 0.28f),
    focusedLabelColor      = GreenAccent,
    unfocusedLabelColor    = Color.White.copy(alpha = 0.55f),
    focusedTextColor       = Color.White,
    unfocusedTextColor     = Color.White,
    cursorColor            = GreenAccent,
    focusedContainerColor  = Color.White.copy(alpha = 0.06f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
)
