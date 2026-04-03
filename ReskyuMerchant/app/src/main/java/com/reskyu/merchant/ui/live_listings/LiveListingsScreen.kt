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
import com.reskyu.merchant.ui.components.LoadingOverlay
import com.reskyu.merchant.ui.components.MainBottomBar
import com.reskyu.merchant.ui.navigation.Screen

private val GreenDark   = Color(0xFF0C1E13)
private val GreenDeep   = Color(0xFF163823)
private val GreenAccent = Color(0xFF52B788)

/**
 * Live Listings screen — displays all OPEN / CLOSING listings for the merchant.
 * Merchants can cancel a listing or navigate to post a new one.
 */
@Composable
fun LiveListingsScreen(
    navController: NavController,
    viewModel: LiveListingsViewModel = viewModel()
) {
    val listings  by viewModel.listings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val merchantId = "merchant_placeholder" // TODO: inject real merchantId

    LaunchedEffect(Unit) {
        viewModel.loadListings(merchantId)
    }

    Scaffold(
        containerColor = Color(0xFFF2F8F4),
        floatingActionButton = {
            FloatingActionButton(
                onClick           = { navController.navigate(Screen.POST_LISTING) },
                containerColor    = GreenAccent,
                contentColor      = Color.White,
                shape             = RoundedCornerShape(16.dp)
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
            ListingsHeader(count = listings.size)

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
                                onCancel = { viewModel.cancelListing(it, merchantId) }
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

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun ListingsHeader(count: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(GreenDark, GreenDeep)))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column {
            Text(
                text       = "Live Listings",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text     = if (count > 0) "$count active right now" else "No active listings",
                fontSize = 13.sp,
                color    = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyListingsState(navController: NavController) {
    Column(
        modifier                = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment     = Alignment.CenterHorizontally,
        verticalArrangement     = Arrangement.Center
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
            onClick        = { navController.navigate(Screen.POST_LISTING) },
            shape          = RoundedCornerShape(14.dp),
            colors         = ButtonDefaults.buttonColors(containerColor = GreenAccent),
            modifier       = Modifier.height(50.dp)
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
