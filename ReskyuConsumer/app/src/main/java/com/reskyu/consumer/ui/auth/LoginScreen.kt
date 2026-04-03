package com.reskyu.consumer.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.consumer.data.model.LoginState
import com.reskyu.consumer.ui.components.LoadingOverlay
import com.reskyu.consumer.ui.components.ReskyuButton
import com.reskyu.consumer.ui.navigation.Screen

/**
 * LoginScreen
 *
 * Entry point for unauthenticated users. Handles phone OTP sign-in flow
 * (or Google Sign-In — adapt based on your auth strategy).
 *
 * State management:
 *  - Observes [LoginState] from [LoginViewModel]
 *  - Shows [LoadingOverlay] during auth
 *  - Navigates to [Screen.Home] on [LoginState.Success], removing login from back stack
 *
 * @param navController  NavController for post-login navigation
 * @param viewModel      Injected LoginViewModel
 */
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = viewModel()
) {
    val loginState by viewModel.loginState.collectAsState()
    var phoneNumber by remember { mutableStateOf("") }

    // Observe state changes for navigation
    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Main UI ───────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to Reskyu",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Rescue food. Save money. Help the planet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Phone number input
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                placeholder = { Text("+91 98765 43210") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error message
            if (loginState is LoginState.Error) {
                Text(
                    text = (loginState as LoginState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            ReskyuButton(
                text = "Send OTP",
                onClick = { viewModel.sendOtp(phoneNumber) },
                enabled = loginState !is LoginState.Loading && phoneNumber.isNotBlank()
            )

            // TODO: Add OTP input field (show after sendOtp succeeds)
            // TODO: Add "Verify OTP" button
            // TODO: Add Google Sign-In option if required
        }

        // ── Loading Overlay ───────────────────────────────────────────────────
        if (loginState is LoginState.Loading) {
            LoadingOverlay()
        }
    }
}
