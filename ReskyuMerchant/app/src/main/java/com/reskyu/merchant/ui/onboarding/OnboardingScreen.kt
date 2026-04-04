package com.reskyu.merchant.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.merchant.data.model.MerchantDraft
import com.reskyu.merchant.data.model.SaveState
import com.reskyu.merchant.ui.components.LoadingOverlay
import com.reskyu.merchant.ui.navigation.Screen
import com.reskyu.merchant.ui.theme.RGreenAccent
import com.reskyu.merchant.ui.theme.RGreenDark
import com.reskyu.merchant.ui.theme.RGreenDeep
import com.reskyu.merchant.ui.theme.RGreenLight
import com.reskyu.merchant.ui.theme.RGreenMid
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

// ── Brand palette ─────────────────────────────────────────────────────────────
private val GreenDark   = RGreenDark
private val GreenDeep   = RGreenDeep
private val GreenMid    = RGreenMid
private val GreenAccent = RGreenAccent
private val GreenLight  = RGreenLight

// ── Per-page static content ───────────────────────────────────────────────────
private data class PageMeta(val emoji: String, val title: String, val subtitle: String)

private val PAGE_META = listOf(
    PageMeta("🏢", "What's your\nbusiness name?",   "Set up your Reskyu Partner profile"),
    PageMeta("📍", "Where are\nyou located?",        "We'll show you to nearby customers"),
    PageMeta("🕐", "What time do\nyou close?",        "Help customers know when to arrive"),
    PageMeta("✅", "You're all set!",                 "Review your details and go live")
)



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
    val draft        by viewModel.draft.collectAsState()
    val saveState    by viewModel.saveState.collectAsState()
    val locationState by viewModel.locationState.collectAsState()
    val mapCenter    by viewModel.mapCenter.collectAsState()

    val pagerState     = rememberPagerState(pageCount = { PAGE_META.size })
    val coroutineScope = rememberCoroutineScope()
    val context        = LocalContext.current

    var businessNameInput by remember { mutableStateOf("") }
    var closingTimeInput  by remember { mutableStateOf("") }

    // Permission launcher — fires fetchLocation when granted
    val locationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.fetchLocation(context)
    }

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
                .imePadding()
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
                        locationState   = locationState,
                        mapCenter       = mapCenter,
                        onMapMoved      = { lat, lng -> viewModel.onMapCenterChanged(lat, lng) },
                        onConfirmMap    = { lat, lng -> viewModel.reverseGeocode(lat, lng) },
                        onGpsClick      = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) viewModel.fetchLocation(context)
                            else locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
                        meta = PAGE_META[1]
                    )
                    2 -> StepClosingTime(
                        value         = closingTimeInput,
                        onValueChange = { closingTimeInput = it },
                        meta          = PAGE_META[2]
                    )
                    3 -> StepConfirm(draft = draft, locationState = locationState, meta = PAGE_META[3])
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

// ── Compact page shell — keyboard-safe, scrollable (used for input slides) ──────

@Composable
private fun CompactPageShell(
    meta:    PageMeta,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(text = meta.emoji, fontSize = 48.sp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text       = meta.title,
                fontSize   = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                textAlign  = TextAlign.Center,
                lineHeight = 30.sp
            )
            Text(
                text      = meta.subtitle,
                fontSize  = 14.sp,
                color     = Color.White.copy(alpha = 0.58f),
                textAlign = TextAlign.Center
            )
        }
        content()
        Spacer(Modifier.height(8.dp))
    }
}

// ── Step 0: Business Name ─────────────────────────────────────────────────────

@Composable
private fun StepBusinessName(value: String, onValueChange: (String) -> Unit, meta: PageMeta) {
    CompactPageShell(meta = meta) {
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
private fun StepLocation(
    locationState: LocationState,
    mapCenter:     Pair<Double, Double>,
    onMapMoved:    (Double, Double) -> Unit,
    onConfirmMap:  (Double, Double) -> Unit,
    onGpsClick:    () -> Unit,
    meta:          PageMeta
) {
    val isFetching = locationState is LocationState.Fetching

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        // ── Compact header ────────────────────────────────────────────────────
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = meta.emoji, fontSize = 40.sp)
            Text(
                text       = meta.title,
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                textAlign  = TextAlign.Center,
                lineHeight = 28.sp
            )
            Text(
                text      = meta.subtitle,
                fontSize  = 13.sp,
                color     = Color.White.copy(alpha = 0.58f),
                textAlign = TextAlign.Center
            )
        }

        // ── OSMDroid map with fixed crosshair ─────────────────────────────────
        Box(
            modifier        = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        isTilesScaledToDpi = true
                        controller.setZoom(if (mapCenter == Pair(20.5937, 78.9629)) 5.0 else 15.0)
                        controller.setCenter(GeoPoint(mapCenter.first, mapCenter.second))
                        addMapListener(object : org.osmdroid.events.MapListener {
                            override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                                val c = this@apply.mapCenter
                                onMapMoved(c.latitude, c.longitude)
                                return false
                            }
                            override fun onZoom(event: org.osmdroid.events.ZoomEvent?) = false
                        })
                    }
                },
                update = { mapView ->
                    val curLat = mapView.mapCenter.latitude
                    val curLng = mapView.mapCenter.longitude
                    val dLat   = Math.abs(curLat - mapCenter.first)
                    val dLng   = Math.abs(curLng - mapCenter.second)
                    if (dLat > 0.0005 || dLng > 0.0005) {
                        mapView.controller.animateTo(GeoPoint(mapCenter.first, mapCenter.second))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Fixed crosshair pin at center
            Text(
                text     = "⊕",
                fontSize = 28.sp,
                color    = Color(0xFFE63946),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // ── Address pill (shown when captured) ────────────────────────────────
        if (locationState is LocationState.Captured) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GreenAccent.copy(alpha = 0.16f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector        = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint               = GreenAccent,
                    modifier           = Modifier.size(18.dp)
                )
                Text(
                    text       = locationState.display,
                    fontSize   = 13.sp,
                    color      = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ── Confirm button ────────────────────────────────────────────────────
        Button(
            onClick  = { onConfirmMap(mapCenter.first, mapCenter.second) },
            enabled  = !isFetching,
            shape    = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = GreenAccent,
                contentColor           = Color.White,
                disabledContainerColor = GreenAccent.copy(alpha = 0.35f),
                disabledContentColor   = Color.White.copy(alpha = 0.5f)
            )
        ) {
            if (isFetching) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(18.dp),
                    color       = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Getting address…", fontWeight = FontWeight.SemiBold)
            } else {
                Text("✓  Confirm this location", fontWeight = FontWeight.SemiBold)
            }
        }

        // ── GPS snap button ───────────────────────────────────────────────────
        OutlinedButton(
            onClick  = onGpsClick,
            enabled  = !isFetching,
            shape    = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border   = ButtonDefaults.outlinedButtonBorder.copy(
                brush = SolidColor(Color.White.copy(alpha = 0.35f))
            )
        ) {
            Icon(Icons.Rounded.LocationOn, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Snap to GPS", fontSize = 14.sp)
        }

        Spacer(Modifier.height(4.dp))
    }
}


// ── Step 2: Closing Time ──────────────────────────────────────────────────────

private val CLOSING_PRESETS = listOf("7 PM", "8 PM", "9 PM", "10 PM", "11 PM", "12 AM")

@Composable
private fun StepClosingTime(value: String, onValueChange: (String) -> Unit, meta: PageMeta) {
    CompactPageShell(meta = meta) {
        // Two rows of 3 chips — equal width, no overflow
        listOf(CLOSING_PRESETS.take(3), CLOSING_PRESETS.drop(3)).forEach { rowPresets ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowPresets.forEach { preset ->
                    val isSelected = value == preset
                    FilterChip(
                        selected = isSelected,
                        onClick  = { onValueChange(preset) },
                        label    = { Text(preset, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
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
        }

        // Custom time input
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            label         = { Text("Or type a custom time, e.g. 9:30 PM") },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            shape         = RoundedCornerShape(14.dp),
            colors        = darkFieldColors()
        )
    }
}

// ── Step 3: Confirm ───────────────────────────────────────────────────────────

@Composable
private fun StepConfirm(draft: MerchantDraft, locationState: LocationState, meta: PageMeta) {
    PageShell(meta = meta) {
        val locationDisplay = when {
            locationState is LocationState.Captured -> locationState.display
            draft.lat != 0.0 -> "%.4f°, %.4f°".format(draft.lat, draft.lng)
            else             -> "Not set"
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ConfirmRow(Icons.Rounded.Store,      GreenAccent,       "Business Name", draft.businessName.ifBlank { "Not entered" })
            ConfirmRow(Icons.Rounded.LocationOn, Color(0xFF5BA4D5), "Location",      locationDisplay)
            ConfirmRow(Icons.Rounded.Schedule,   Color(0xFFFFD166), "Closing Time",  draft.closingTime.ifBlank { "Not entered" })
        }
    }
}

@Composable
private fun ConfirmRow(icon: ImageVector, iconTint: Color, label: String, value: String) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = iconTint,
                modifier           = Modifier.size(20.dp)
            )
        }
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
