package com.reskyu.consumer.ui.home

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.reskyu.consumer.NotificationDeepLinkBus
import com.reskyu.consumer.data.model.DietaryTag
import com.reskyu.consumer.data.model.Listing
import com.reskyu.consumer.ui.navigation.Screen
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import java.util.concurrent.TimeUnit
import kotlin.math.*

private val RGreenDark    = Color(0xFF0C1E13)   // header top  / exact merchant GreenDark
private val RGreenDeep    = Color(0xFF163823)   // header mid  / exact merchant GreenDeep
private val RGreenMid     = Color(0xFF1F5235)   // header btm  / exact merchant GreenMid
private val RGreenAccent  = Color(0xFF52B788)   // CTA / chips / exact merchant GreenAccent
private val RGreenLight   = Color(0xFF95D5B2)   // subtitle on dark / merchant GreenLight
private val RPriceGreen   = Color(0xFF1F5235)   // price text  / = GreenMid
private val RGreenSurface = Color(0xFFF2F8F4)   // screen bg   / exact merchant ScreenBg
private val RGreenOnCard  = Color(0xFF0C1E13)   // card text   / = GreenDark

private val MAP_HEIGHT_COLLAPSED = 150.dp
private val MAP_HEIGHT_EXPANDED  = 340.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    innerNavController: NavController,
    outerNavController: NavController,
    viewModel: HomeViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val listings       by viewModel.listings.collectAsState()
    val isLoading      by viewModel.isLoading.collectAsState()
    val error          by viewModel.error.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val merchantRatings by viewModel.merchantRatings.collectAsState()

    val userLat        by viewModel.userLat.collectAsState()
    val userLng        by viewModel.userLng.collectAsState()

    val displayedListings = remember(listings, selectedFilter) {
        listings.filter { l -> selectedFilter == null || l.dietaryTag == selectedFilter?.name }
    }

    var mapExpanded by remember { mutableStateOf(false) }
    var selectedListing by remember { mutableStateOf<Listing?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                   || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val pendingListingId by NotificationDeepLinkBus.pendingListingId.collectAsState()
    LaunchedEffect(pendingListingId) {
        pendingListingId?.let { listingId ->
            NotificationDeepLinkBus.consume()  // clear first to prevent loop
            outerNavController.navigate(
                com.reskyu.consumer.ui.navigation.Screen.DetailListing.createRoute(listingId)
            )
        }
    }

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
        HomeBanner(onNotificationsClick = { innerNavController.navigate(Screen.Notifications.route) })

        OsmMapCard(
            listings      = listings,
            userLat       = userLat,
            userLng       = userLng,
            mapHeight     = mapHeight,
            isExpanded    = mapExpanded,
            onMarkerClick = { outerNavController.navigate(Screen.DetailListing.createRoute(it.id)) },
            onToggleExpand = { mapExpanded = !mapExpanded }
        )

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
                            listing       = listing,
                            distanceKm    = haversineKm(userLat, userLng, listing.lat, listing.lng)
                                .takeIf { listing.lat != 0.0 },
                            merchantRating = merchantRatings[listing.merchantId],
                            onClick       = { selectedListing = listing },
                            modifier      = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    selectedListing?.let { listing ->
        OrderBottomSheet(
            listing   = listing,
            onDismiss = { selectedListing = null },
            onConfirm = { qty ->
                selectedListing = null
                outerNavController.navigate(Screen.Claim.createRoute(listing.id, qty))
            }
        )
    }
}

@Composable
private fun HomeBanner(onNotificationsClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                Brush.verticalGradient(listOf(RGreenDark, RGreenDeep, RGreenMid))
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Today's Food Drops 🍱",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Rescue surplus meals near you",
                    style = MaterialTheme.typography.bodySmall,
                    color = RGreenLight
                )
            }

            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                Icon(
                    imageVector        = Icons.Rounded.Notifications,
                    contentDescription = "Notifications",
                    tint               = Color.White,
                    modifier           = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun OsmMapCard(
    listings: List<Listing>,
    userLat: Double,
    userLng: Double,
    mapHeight: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean,
    onMarkerClick: (Listing) -> Unit,
    onToggleExpand: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { Configuration.getInstance().userAgentValue = context.packageName }

    val radarOverlay = remember { RadarPulseOverlay(GeoPoint(userLat, userLng)) }
    LaunchedEffect(userLat, userLng) { radarOverlay.updateCenter(GeoPoint(userLat, userLng)) }

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
                        setMultiTouchControls(true)
                        setBuiltInZoomControls(false)
                        controller.setZoom(15.0)
                        controller.setCenter(GeoPoint(userLat, userLng))
                        isTilesScaledToDpi = true
                        setOnTouchListener { v, event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN,
                                MotionEvent.ACTION_MOVE  -> v.parent.requestDisallowInterceptTouchEvent(true)
                                MotionEvent.ACTION_UP,
                                MotionEvent.ACTION_CANCEL -> v.parent.requestDisallowInterceptTouchEvent(false)
                            }
                            false
                        }
                        radarOverlay.startAnimation(this)
                    }
                },
                update = { mapView ->
                    val userPoint = GeoPoint(userLat, userLng)
                    mapView.controller.setCenter(userPoint)
                    mapView.overlays.clear()
                    mapView.overlays.add(radarOverlay)
                    mapView.overlays.add(Marker(mapView).apply {
                        position = userPoint
                        title    = "You are here"
                        icon     = userLocationIcon(context)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
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
            DisposableEffect(Unit) { onDispose { radarOverlay.stopAnimation() } }
        }

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

@Composable
private fun DietaryFilterChips(selected: DietaryTag?, onSelect: (DietaryTag?) -> Unit) {
    val filters = listOf(null, DietaryTag.VEG, DietaryTag.NON_VEG, DietaryTag.VEGAN,
                         DietaryTag.BAKERY, DietaryTag.SWEETS)
    val labels  = mapOf(
        null               to "All 🍽️",
        DietaryTag.VEG     to "Veg 🥗",
        DietaryTag.NON_VEG to "Non-Veg 🍗",
        DietaryTag.VEGAN   to "Vegan 🌱",
        DietaryTag.BAKERY  to "Bakery 🥐",
        DietaryTag.SWEETS  to "Sweets 🍮"
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderBottomSheet(
    listing: Listing,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit     // carries the selected quantity
) {
    val sheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val discountPct = if (listing.originalPrice > 0)
        ((listing.originalPrice - listing.discountedPrice) / listing.originalPrice * 100).toInt()
    else 0
    val timeLeftMs = listing.expiresAt.toDate().time - System.currentTimeMillis()
    val maxQty     = listing.mealsLeft.coerceAtLeast(1)

    var quantity    by remember { mutableStateOf(1) }
    val savings     = (listing.originalPrice - listing.discountedPrice) * quantity
    val totalPrice  = listing.discountedPrice * quantity

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

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.ShoppingBag, null, tint = RGreenAccent, modifier = Modifier.size(18.dp))
                Text(
                    if (listing.isMysteryBox) "What's in the box?" else "What's in the bag",
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = RGreenOnCard
                )
            }
            Spacer(Modifier.height(8.dp))

            if (listing.isMysteryBox) {
                Surface(
                    color = Color(0xFFF3EEFF),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "🎁 It's a surprise!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5C35C7)
                        )
                        Text(
                            "Contents vary — every box is unique. You'll know when you pick it up!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7C5CBF)
                        )
                        if (listing.heroItem.isNotBlank()) {
                            Text(
                                "💬 Hint: ${listing.heroItem.trim()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF5C35C7)
                            )
                        }
                        if (listing.boxType.isNotBlank()) {
                            Text(
                                "🏷️ Box type: ${listing.boxType.lowercase().replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF7C5CBF)
                            )
                        }
                    }
                }

            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        listing.heroItem,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = RGreenOnCard,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick  = { if (quantity > 1) quantity-- },
                            modifier = Modifier.size(28.dp),
                            enabled  = quantity > 1
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Less",
                                modifier = Modifier.size(16.dp),
                                tint = if (quantity > 1) RGreenAccent else Color(0xFFB0CABB))
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = RGreenAccent.copy(alpha = 0.12f)) {
                            Text("$quantity",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = RGreenAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                        IconButton(
                            onClick  = { if (quantity < maxQty) quantity++ },
                            modifier = Modifier.size(28.dp),
                            enabled  = quantity < maxQty
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "More",
                                modifier = Modifier.size(16.dp),
                                tint = if (quantity < maxQty) RGreenAccent else Color(0xFFB0CABB))
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "${listing.mealsLeft} portion${if (listing.mealsLeft != 1) "s" else ""} available",
                style = MaterialTheme.typography.labelSmall,
                color = if (listing.mealsLeft <= 2) Color(0xFFE65100) else Color(0xFF888888)
            )

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

            Text("Price Breakdown", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = RGreenOnCard)
            Spacer(Modifier.height(8.dp))
            if (listing.isMysteryBox) {
                if (listing.priceRangeMin > 0 || listing.priceRangeMax > 0) {
                    PriceRow("Box value (est.)", "₹${listing.priceRangeMin.toInt()}–₹${listing.priceRangeMax.toInt()}")
                }
                if (listing.itemCount > 0) {
                    PriceRow("Items in box", "${listing.itemCount} items")
                }
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = Color(0xFFB2DFBB))
                Spacer(Modifier.height(6.dp))
                PriceRow("You Pay", "₹${(listing.discountedPrice * quantity).toInt()}", bold = true, valueColor = Color(0xFF5C35C7))
            } else {
                PriceRow("Original price", "₹${(listing.originalPrice * quantity).toInt()}", strikethrough = true)
                PriceRow("Discount ($discountPct%)", "-₹${savings.toInt()}", valueColor = RPriceGreen)
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = Color(0xFFB2DFBB))
                Spacer(Modifier.height(6.dp))
                PriceRow("You Pay", "₹${totalPrice.toInt()}", bold = true, valueColor = RPriceGreen)
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick  = { onConfirm(quantity) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = RGreenAccent)
            ) {
                Text(
                    "Confirm Payment  ·  ₹${totalPrice.toInt()}" +
                        if (quantity > 1) "  (×$quantity)" else "",
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

private class RadarPulseOverlay(
    private var center: GeoPoint,
    private val radiusMeters: Double = 2000.0
) : Overlay() {

    private var animProgress = 0f
    private var attachedMap: MapView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val ticker  = object : Runnable {
        override fun run() {
            animProgress = (animProgress + 0.02f) % 1f
            attachedMap?.invalidate()
            handler.postDelayed(this, 50L)
        }
    }

    fun startAnimation(map: MapView) { attachedMap = map; handler.post(ticker) }
    fun stopAnimation()              { handler.removeCallbacks(ticker); attachedMap = null }
    fun updateCenter(c: GeoPoint)    { center = c }

    override fun draw(canvas: android.graphics.Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val proj = mapView.projection
        val sp   = Point(); proj.toPixels(center, sp)
        val cx = sp.x.toFloat(); val cy = sp.y.toFloat()

        val off = GeoPoint(
            center.latitude,
            center.longitude + radiusMeters / (111_320.0 * cos(Math.toRadians(center.latitude)))
        )
        val op = Point(); proj.toPixels(off, op)
        val rPx = abs(op.x - sp.x).toFloat()

        canvas.drawCircle(cx, cy, rPx, AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            style = AndroidPaint.Style.FILL; color = AndroidColor.argb(18, 45, 198, 83)
        })
        canvas.drawCircle(cx, cy, rPx, AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            style = AndroidPaint.Style.STROKE; strokeWidth = 2.5f
            color = AndroidColor.argb(140, 45, 198, 83)
        })
        for (i in 0..2) {
            val phase = (animProgress + i / 3f) % 1f
            canvas.drawCircle(cx, cy, rPx * 0.40f * phase,
                AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    style = AndroidPaint.Style.STROKE
                    strokeWidth = 3.5f - 2f * phase
                    color = AndroidColor.argb((220 * (1f - phase)).toInt(), 45, 198, 83)
                })
        }
    }
}

private fun userLocationIcon(context: android.content.Context): BitmapDrawable {
    val dp = context.resources.displayMetrics.density
    val sz = (52 * dp).toInt()
    val bm = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
    val cv = AndroidCanvas(bm)
    val cx = sz / 2f; val cy = sz / 2f
    cv.drawCircle(cx, cy, sz * 0.46f, AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(55, 220, 40, 40); style = AndroidPaint.Style.FILL
    })
    cv.drawCircle(cx, cy, sz * 0.30f, AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE; style = AndroidPaint.Style.FILL
    })
    cv.drawCircle(cx, cy, sz * 0.20f, AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(210, 35, 35); style = AndroidPaint.Style.FILL
    })
    return BitmapDrawable(context.resources, bm)
}
