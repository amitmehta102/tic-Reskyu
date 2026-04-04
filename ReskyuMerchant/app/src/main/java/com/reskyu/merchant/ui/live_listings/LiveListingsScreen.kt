package com.reskyu.merchant.ui.live_listings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.reskyu.merchant.ui.components.LoadingOverlay
import com.reskyu.merchant.ui.components.MainBottomBar
import com.reskyu.merchant.ui.navigation.Screen
import com.reskyu.merchant.ui.components.ReskyuHeader
import com.reskyu.merchant.ui.theme.RGreenAccent
import com.reskyu.merchant.ui.theme.RScreenBg
import kotlinx.coroutines.launch

/**
 * Live Listings screen — displays all OPEN / CLOSING listings for the merchant.
 * Real-time Firestore snapshot listener keeps the list up-to-date automatically.
 * Cancel requires a confirmation dialog inside [LiveListingCard].
 */
@Composable
fun LiveListingsScreen(
    navController: NavController,
    viewModel: LiveListingsViewModel = viewModel()
) {
    val listings  by viewModel.listings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error     by viewModel.error.collectAsState()

    val merchantId   = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val snackbarHost = remember { SnackbarHostState() }
    val scope        = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadListings(merchantId)
    }

    // Show errors in a Snackbar
    LaunchedEffect(error) {
        error?.let {
            scope.launch {
                snackbarHost.showSnackbar(
                    message     = it,
                    actionLabel = "Dismiss",
                    duration    = SnackbarDuration.Long
                )
            }
        }
    }

    Scaffold(
        containerColor   = RScreenBg,
        snackbarHost     = { SnackbarHost(hostState = snackbarHost) },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { navController.navigate(Screen.POST_LISTING) },
                containerColor = RGreenAccent,
                contentColor   = Color.White,
                shape          = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector        = Icons.Rounded.Add,
                    contentDescription = "Post new listing"
                )
            }
        },
        bottomBar = { MainBottomBar(navController = navController, currentRoute = Screen.LIVE_LISTINGS) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Header ───────────────────────────────────────────────────────
            ReskyuHeader(
                title    = "Live Listings",
                subtitle = when (listings.size) {
                    0    -> "No active listings"
                    1    -> "1 listing active right now"
                    else -> "${listings.size} listings active right now"
                }
            )

            // ── Body ─────────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                if (!isLoading && listings.isEmpty()) {
                    EmptyListingsState(navController = navController)
                } else {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(listings, key = { it.id }) { listing ->
                            LiveListingCard(
                                listing  = listing,
                                onCancel = { viewModel.cancelListing(it) }
                            )
                        }
                        // Extra bottom padding so FAB doesn't overlap last card
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }

                LoadingOverlay(isVisible = isLoading)
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyListingsState(navController: NavController) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🍽️", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text       = "No Active Listings",
            fontSize   = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Color(0xFF374151)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text      = "Post a food-drop listing to start\nrescuing surplus meals today",
            fontSize  = 14.sp,
            color     = Color(0xFF9CA3AF),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick  = { navController.navigate(Screen.POST_LISTING) },
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = RGreenAccent),
            modifier = Modifier.height(50.dp)
        ) {
            Text(
                text       = "+ Post First Listing",
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = Color.White
            )
        }
    }
}
