package com.reskyu.consumer.ui.home

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.consumer.data.model.DietaryTag
import com.reskyu.consumer.data.model.Listing
import com.reskyu.consumer.ui.navigation.Screen
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.*

/** User's fixed dev location (Bhopal) — replace with real GPS later */
private const val USER_LAT = 23.2599
private const val USER_LNG = 77.4126

/**
 * HomeScreen
 *
 * Layout (top → bottom):
 *  ── Clean header: greeting + avatar
 *  ── Search bar
 *  ── Dietary filter chips
 *  ── OSM map card with drop markers (FREE, no API key)
 *  ── Listing cards with distance badges
 */
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

    var searchQuery by remember { mutableStateOf("") }

    val displayedListings = remember(listings, searchQuery, selectedFilter) {
        listings
            .filter { l ->
                searchQuery.isBlank() ||
                l.heroItem.contains(searchQuery, ignoreCase = true) ||
                l.businessName.contains(searchQuery, ignoreCase = true)
            }
            .filter { l -> selectedFilter == null || l.dietaryTag == selectedFilter?.name }
    }

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            // ── Header ─────────────────────────────────────────────────────────
            item { HomeHeader() }

            // ── Search bar ─────────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search food, restaurants…") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 6.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            // ── Dietary chips ──────────────────────────────────────────────────
            item {
                DietaryFilterChips(
                    selected = selectedFilter,
                    onSelect = { viewModel.setFilter(it) }
                )
            }

            // ── OpenStreetMap Card ─────────────────────────────────────────────
            item {
                OsmDropsMapCard(
                    listings = listings,
                    onMarkerClick = { listing ->
                        outerNavController.navigate(Screen.DetailListing.createRoute(listing.id))
                    }
                )
            }

            // ── Section header ─────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${displayedListings.size} drops nearby",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Rounded.Refresh, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Refresh", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // ── Error / Empty / Cards ──────────────────────────────────────────
            when {
                error != null -> item {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("😕", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(error!!, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { viewModel.refresh() }) { Text("Try Again") }
                        }
                    }
                }

                displayedListings.isEmpty() && !isLoading -> item {
                    Box(
                        Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🍱", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (selectedFilter != null || searchQuery.isNotBlank())
                                    "No listings match your filter"
                                else "No food drops near you right now",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                else -> items(displayedListings, key = { it.id }) { listing ->
                    ListingCard(
                        listing = listing,
                        distanceKm = haversineKm(USER_LAT, USER_LNG, listing.lat, listing.lng)
                            .takeIf { listing.lat != 0.0 },
                        onClick = {
                            outerNavController.navigate(Screen.DetailListing.createRoute(listing.id))
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

// ── Header ─────────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    "Nearby",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                "Today's Food Drops",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "R",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── OSM Map Card (osmdroid, no API key) ───────────────────────────────────────

@Composable
private fun OsmDropsMapCard(
    listings: List<Listing>,
    onMarkerClick: (Listing) -> Unit
) {
    val context = LocalContext.current

    // Init osmdroid config (user-agent required by OSM tile policy)
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)  // OpenStreetMap standard tiles
                    setMultiTouchControls(true)
                    controller.setZoom(14.5)
                    controller.setCenter(GeoPoint(USER_LAT, USER_LNG))
                    isTilesScaledToDpi = true
                    // Prevent the map from stealing scroll from the LazyColumn
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN,
                            MotionEvent.ACTION_MOVE -> v.parent.requestDisallowInterceptTouchEvent(true)
                            MotionEvent.ACTION_UP,
                            MotionEvent.ACTION_CANCEL -> v.parent.requestDisallowInterceptTouchEvent(false)
                        }
                        false
                    }
                }
            },
            update = { mapView ->
                // Clear old markers and re-add
                mapView.overlays.clear()

                // User location marker (blue tinted default icon)
                val userMarker = Marker(mapView).apply {
                    position = GeoPoint(USER_LAT, USER_LNG)
                    title = "You are here"
                    snippet = "Your location"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(userMarker)

                // Food drop markers
                listings.filter { it.lat != 0.0 || it.lng != 0.0 }.forEach { listing ->
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(listing.lat, listing.lng)
                        title = listing.businessName
                        snippet = "₹${listing.discountedPrice.toInt()} · ${listing.mealsLeft} left"
                        setOnMarkerClickListener { _, _ ->
                            onMarkerClick(listing)
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }

                mapView.invalidate()
            }
        )
    }
}

// ── Dietary Filter Chips ───────────────────────────────────────────────────────

@Composable
private fun DietaryFilterChips(
    selected: DietaryTag?,
    onSelect: (DietaryTag?) -> Unit
) {
    val filters = listOf(null) + DietaryTag.values().toList()
    val labels = mapOf(
        null to "All 🍽️",
        DietaryTag.VEG to "Veg 🥗",
        DietaryTag.NON_VEG to "Non-Veg 🍗",
        DietaryTag.VEGAN to "Vegan 🌱",
        DietaryTag.JAIN to "Jain ⚪"
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(if (selected == filter) null else filter) },
                label = { Text(labels[filter] ?: "", style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}

// ── Distance Utilities ─────────────────────────────────────────────────────────

/** Haversine formula: returns straight-line distance in km */
fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    if (lat2 == 0.0 && lon2 == 0.0) return 0.0
    val R = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)
    return R * 2 * atan2(sqrt(a), sqrt(1 - a))
}

/** Formats distance as "450m" or "1.2 km" */
fun formatDistance(km: Double?): String? {
    if (km == null || km == 0.0) return null
    return if (km < 1.0) "${(km * 1000).toInt()}m" else "%.1f km".format(km)
}
