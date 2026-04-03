package com.reskyu.merchant.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.merchant.data.model.SaveState
import com.reskyu.merchant.ui.components.LoadingOverlay
import com.reskyu.merchant.ui.navigation.Screen

/**
 * Multi-step onboarding screen using Compose [HorizontalPager].
 * Steps: 1) Business Name  2) Location  3) Closing Time  4) Confirm
 */
@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = viewModel()
) {
    val draft by viewModel.draft.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 4 })
    var businessNameInput by remember { mutableStateOf("") }
    var closingTimeInput by remember { mutableStateOf("") }

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Saved) {
            navController.navigate(Screen.DASHBOARD) {
                popUpTo(Screen.ONBOARDING) { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Step indicator
            Text(
                text = "Step ${pagerState.currentPage + 1} of 4",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                style = MaterialTheme.typography.labelLarge
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false // Controlled programmatically
            ) { page ->
                when (page) {
                    0 -> OnboardingStepBusinessName(
                        value = businessNameInput,
                        onValueChange = { businessNameInput = it }
                    )
                    1 -> OnboardingStepLocation(
                        onLocationPicked = { lat, lng, geoHash ->
                            viewModel.updateLocation(lat, lng, geoHash)
                        }
                    )
                    2 -> OnboardingStepClosingTime(
                        value = closingTimeInput,
                        onValueChange = { closingTimeInput = it }
                    )
                    3 -> OnboardingStepConfirm(draft = draft)
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            // TODO: pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    ) { Text("Back") }
                } else {
                    Spacer(modifier = Modifier.width(0.dp))
                }

                Button(
                    onClick = {
                        when (pagerState.currentPage) {
                            0 -> viewModel.updateBusinessName(businessNameInput)
                            2 -> viewModel.updateClosingTime(closingTimeInput)
                            3 -> viewModel.completeOnboarding()
                        }
                        if (pagerState.currentPage < 3) {
                            // TODO: pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                ) {
                    Text(if (pagerState.currentPage == 3) "Finish" else "Next")
                }
            }
        }

        LoadingOverlay(isVisible = saveState is SaveState.Saving)
    }
}

// ─── Step Composables ──────────────────────────────────────────────────────────

@Composable
private fun OnboardingStepBusinessName(value: String, onValueChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("What's your business name?", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Business Name") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OnboardingStepLocation(onLocationPicked: (Double, Double, String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // TODO: Integrate Google Maps / Location picker here
        Text("📍 Location Picker — Coming soon", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun OnboardingStepClosingTime(value: String, onValueChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("When do you close?", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Closing Time (HH:MM)") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OnboardingStepConfirm(draft: com.reskyu.merchant.data.model.MerchantDraft) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Confirm your details", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Business: ${draft.businessName}")
        Text("Closes at: ${draft.closingTime}")
        Text("Location: ${draft.lat}, ${draft.lng}")
    }
}
