package com.reskyu.merchant.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.merchant.ui.components.LoadingOverlay
import com.reskyu.merchant.ui.navigation.Screen
import com.reskyu.merchant.data.model.SaveState

/**
 * Merchant profile screen for viewing and editing business details.
 * Also provides sign-out action.
 */
@Composable
fun MerchantProfileScreen(
    navController: NavController,
    viewModel: MerchantProfileViewModel = viewModel()
) {
    val merchant by viewModel.merchant.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    var closingTimeInput by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    LaunchedEffect(merchant) {
        merchant?.let { closingTimeInput = it.closingTime }
    }

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Saved) {
            isEditing = false
            viewModel.resetSaveState()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Profile", style = MaterialTheme.typography.headlineMedium)

            merchant?.let { m ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Business: ${m.businessName}", style = MaterialTheme.typography.bodyLarge)
                        Text("Trust Score: ${m.trustScore}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isEditing) {
                            OutlinedTextField(
                                value = closingTimeInput,
                                onValueChange = { closingTimeInput = it },
                                label = { Text("Closing Time (HH:MM)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.updateClosingTime(closingTimeInput) }) {
                                Text("Save")
                            }
                        } else {
                            Text("Closes at: ${m.closingTime}", style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { isEditing = true }) { Text("Edit") }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.signOut()
                    navController.navigate(Screen.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Sign Out")
            }
        }

        LoadingOverlay(isVisible = saveState is SaveState.Saving)
    }
}
