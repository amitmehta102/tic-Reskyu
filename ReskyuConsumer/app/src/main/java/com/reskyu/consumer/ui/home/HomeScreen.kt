package com.reskyu.consumer.ui.home

import android.view.MotionEvent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.reskyu.consumer.data.model.DietaryTag
import com.reskyu.consumer.data.model.Listing
import com.reskyu.consumer.ui.navigation.Screen
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.concurrent.TimeUnit
import kotlin.math.*

// ── Reskyu brand colors (matches screenshot) ──────────────────────────────────
private val RGreenDark    = Color(0xFF0D2B1A)   // deep header dark green
private val RGreenMid     = Color(0xFF133922)   // forest green (mid gradient)
private val RGreenAccent  = Color(0xFF2DC653)   // Reskyu green (accent, buttons, chips)
private val RPriceGreen   = Color(0xFF1A9E45)   // slightly darker green for prices
private val RGreenSurface = Color(0xFFEBF7EE)   // very light mint background
private val RGreenOnCard  = Color(0xFF133922)   // forest green text on cards

private const val USER_LAT = 23.2599
private const val USER_LNG = 77.4126

private val MAP_HEIGHT_COLLAPSED = 150.dp
private val MAP_HEIGHT_EXPANDED  = 340.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    innerNavController: NavController,
    outerNavController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val listings       by viewModel.listings.collectAsState()
    val isLoading      by viewModel.isLoading.collectAsState()
    val error          by viewModel.error.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    val displayedListings = remember(listings, selectedFilter) {
        listings.filter { l -> selectedFilter == null || l.dietaryTag == selectedFilter?.name }
    }

    var mapExpanded by remember { mutableStateOf(false) }

    var selectedListing by remember { mutableStateOf<Listing?>(null) }

    // Animate map height between collapsed and expanded
    val mapHeight by animateDpAsState(
        targetValue = if (mapExpanded) MAP_HEIGHT_EXPANDED else MAP_HEIGHT_COLLAPSED,
        animationSpec = tween(durationMillis = 300),
        label = "mapHeight"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RGreenSurface)
    ) {
        // ── Fixed: branded header ──────────────────────────────────────────────
        HomeBanner()

        // ── Fixed: OSM map (animated height, interactive) ──────────────────────
        OsmMapCard(
            listings      = listings,
            mapHeight     = mapHeight,
            isExpanded    = mapExpanded,
            onMarkerClick = { outerNavController.navigate(Screen.DetailListing.createRoute(it.id)) },
            onToggleExpand = { mapExpanded = !mapExpanded }
        )

        // ── Scrollable: filter chips + listing cards ───────────────────────────
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh    = { viewModel.refresh() },
            modifier     = Modifier.weight(1f)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {

                item {
                    DietaryFilterChips(selected = selectedFilter, onSelect = { viewModel.setFilter(it) })
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            "${displayedListings.size} drops nearby",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = RGreenOnCard
                        )
                        TextButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Rounded.Refresh, null, Modifier.size(14.dp), tint = RGreenAccent)
                            Spacer(Modifier.width(4.dp))
                            Text("Refresh", style = MaterialTheme.typography.labelSmall, color = RGreenAccent)
                        }
                    }
                }

                when {
                    error != null -> item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("😕", fontSize = 48.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(error!!, color = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.refresh() },
                                    colors = ButtonDefaults.buttonColors(containerColor = RGreenAccent)
                                ) { Text("Try Again", color = Color.White) }
                            }
                        }
                    }

                    displayedListings.isEmpty() && !isLoading -> item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🍱", fontSize = 48.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (selectedFilter != null) "No listings match your filter"
                                    else "No food drops near you right now",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RGreenOnCard
                                )
                            }
                        }
                    }

                    else -> items(displayedListings, key = { it.id }) { listing ->
                        ListingCard(
                            listing    = listing,
                            distanceKm = haversineKm(USER_LAT, USER_LNG, listing.lat, listing.lng)
                                .takeIf { listing.lat != 0.0 },
                            onClick    = { selectedListing = listing },
                            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    // ── Order Bottom Sheet ────────────────────────────────────────────────────
    selectedListing?.let { listing ->
        OrderBottomSheet(
            listing   = listing,
            onDismiss = { selectedListing = null },
            onConfirm = {
                selectedListing = null
                outerNavController.navigate(Screen.DetailListing.createRoute(listing.id))
            }
        )
    }
}

// ── Header (matches Orders white Surface style) ───────────────────────────────

@Composable
private fun HomeBanner() {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(
                "Today's Food Drops",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = RGreenOnCard
            )
            Text(
                "Rescue surplus meals near you",
                style = MaterialTheme.typography.bodySmall,
                color = RGreenOnCard.copy(alpha = 0.55f)
            )
        }
    }
}

// ── OSM Map Card — animated height, fully interactive ─────────────────────────

@Composable
private fun OsmMapCard(
    listings: List<Listing>,
    mapHeight: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean,
    onMarkerClick: (Listing) -> Unit,
    onToggleExpand: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { Configuration.getInstance().userAgentValue = context.packageName }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 6.dp, bottom = 4.dp)
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth().height(mapHeight),
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory  = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)       // pinch-to-zoom + pan
                        setBuiltInZoomControls(false)     // no +/- buttons
                        controller.setZoom(14.5)
                        controller.setCenter(GeoPoint(USER_LAT, USER_LNG))
                        isTilesScaledToDpi = true
                        // Let map handle its own scroll; allow parent to take over when needed
                        setOnTouchListener { v, event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN,
                                MotionEvent.ACTION_MOVE  -> v.parent.requestDisallowInterceptTouchEvent(true)
                                MotionEvent.ACTION_UP,
                                MotionEvent.ACTION_CANCEL -> v.parent.requestDisallowInterceptTouchEvent(false)
                            }
                            false
                        }
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()
                    mapView.overlays.add(Marker(mapView).apply {
                        position = GeoPoint(USER_LAT, USER_LNG)
                        title    = "You are here"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    })
                    listings.filter { it.lat != 0.0 || it.lng != 0.0 }.forEach { listing ->
                        mapView.overlays.add(Marker(mapView).apply {
                            position = GeoPoint(listing.lat, listing.lng)
                            title    = listing.businessName
                            snippet  = "₹${listing.discountedPrice.toInt()} · ${listing.mealsLeft} left"
                            setOnMarkerClickListener { _, _ -> onMarkerClick(listing); true }
                        })
                    }
                    mapView.invalidate()
                }
            )
        }

        // ── Expand / Collapse chip — ONLY button that triggers resize ──────────
        Surface(
            onClick   = onToggleExpand,
            modifier  = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 8.dp),
            shape = RoundedCornerShape(50),
            color = RGreenDark.copy(alpha = 0.80f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    if (isExpanded) Icons.Rounded.Close else Icons.Rounded.LocationOn,
                    contentDescription = if (isExpanded) "Collapse map" else "Expand map",
                    tint   = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    if (isExpanded) "Collapse" else "⤢ Expand",
                    style  = MaterialTheme.typography.labelSmall,
                    color  = Color.White
                )
            }
        }
    }
}

// ── Dietary Filter Chips ──────────────────────────────────────────────────────

@Composable
private fun DietaryFilterChips(selected: DietaryTag?, onSelect: (DietaryTag?) -> Unit) {
    val filters = listOf(null) + DietaryTag.values().toList()
    val labels  = mapOf(
        null               to "All 🍽️",
        DietaryTag.VEG     to "Veg 🥗",
        DietaryTag.NON_VEG to "Non-Veg 🍗",
        DietaryTag.VEGAN   to "Vegan 🌱",
        DietaryTag.JAIN    to "Jain ⚪"
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            FilterChip(
                selected = selected == filter,
                onClick  = { onSelect(if (selected == filter) null else filter) },
                label    = { Text(labels[filter] ?: "", style = MaterialTheme.typography.labelMedium) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RGreenAccent,
                    selectedLabelColor     = Color.White
                )
            )
        }
    }
}

// ── Distance Utilities ────────────────────────────────────────────────────────

fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    if (lat2 == 0.0 && lon2 == 0.0) return 0.0
    val R    = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a    = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)
    return R * 2 * atan2(sqrt(a), sqrt(1 - a))
}

fun formatDistance(km: Double?): String? {
    if (km == null || km == 0.0) return null
    return if (km < 1.0) "${(km * 1000).toInt()}m" else "%.1f km".format(km)
}

// ── Order Bottom Sheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderBottomSheet(
    listing: Listing,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val sheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val discountPct = if (listing.originalPrice > 0)
        ((listing.originalPrice - listing.discountedPrice) / listing.originalPrice * 100).toInt()
    else 0
    val timeLeftMs = listing.expiresAt.toDate().time - System.currentTimeMillis()
    val savings    = listing.originalPrice - listing.discountedPrice

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color(0xFFEBF7EE),
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // ── Hero image ──────────────────────────────────────────────────────
            if (listing.imageUrl.isNotBlank()) {
                AsyncImage(
                    model              = listing.imageUrl,
                    contentDescription = listing.heroItem,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFD4EAD9)),
                    contentAlignment = Alignment.Center
                ) { Text("🍱", fontSize = 48.sp) }
            }
            Spacer(Modifier.height(16.dp))

            // ── Restaurant row ──────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(RGreenDark.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Restaurant, null, tint = RGreenDark, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(listing.businessName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RGreenOnCard)
                    Text(
                        listing.dietaryTag.lowercase().replaceFirstChar { it.uppercase() } +
                                if (discountPct > 0) "  ·  $discountPct% off" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4B7A5A)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFB2DFBB))
            Spacer(Modifier.height(14.dp))

            // ── What's in the bag ───────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.ShoppingBag, null, tint = RGreenAccent, modifier = Modifier.size(18.dp))
                Text("What's in the bag", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = RGreenOnCard)
            }
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(listing.heroItem, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = RGreenOnCard, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(50), color = if (listing.mealsLeft <= 2) Color(0xFFFFF3E0) else Color(0xFFD4EAD9)) {
                    Text(
                        "${listing.mealsLeft} left",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (listing.mealsLeft <= 2) Color(0xFFE65100) else RGreenOnCard,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (timeLeftMs > 0) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.AccessTime, null, tint = Color(0xFF888888), modifier = Modifier.size(12.dp))
                    val h = TimeUnit.MILLISECONDS.toHours(timeLeftMs)
                    val m = TimeUnit.MILLISECONDS.toMinutes(timeLeftMs) % 60
                    Text(
                        if (h <= 0) "Expires in ${m}m" else "Expires in ${h}h ${m}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (timeLeftMs < TimeUnit.HOURS.toMillis(1)) MaterialTheme.colorScheme.error else Color(0xFF888888)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFB2DFBB))
            Spacer(Modifier.height(14.dp))

            // ── Price breakdown ─────────────────────────────────────────────────
            Text("Price Breakdown", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = RGreenOnCard)
            Spacer(Modifier.height(8.dp))
            PriceRow("Original price", "₹${listing.originalPrice.toInt()}", strikethrough = true)
            PriceRow("Discount ($discountPct%)", "-₹${savings.toInt()}", valueColor = RPriceGreen)
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFFB2DFBB))
            Spacer(Modifier.height(6.dp))
            PriceRow("You Pay", "₹${listing.discountedPrice.toInt()}", bold = true, valueColor = RPriceGreen)

            Spacer(Modifier.height(20.dp))

            // ── Confirm Payment ─────────────────────────────────────────────────
            Button(
                onClick  = onConfirm,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = RGreenAccent)
            ) {
                Text(
                    "Confirm Payment  ·  ₹${listing.discountedPrice.toInt()}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PriceRow(
    label: String,
    value: String,
    bold: Boolean = false,
    strikethrough: Boolean = false,
    valueColor: Color = Color(0xFF4B7A5A)
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF555555), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(
                textDecoration = if (strikethrough) androidx.compose.ui.text.style.TextDecoration.LineThrough
                                 else androidx.compose.ui.text.style.TextDecoration.None
            ),
            color = valueColor,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}

