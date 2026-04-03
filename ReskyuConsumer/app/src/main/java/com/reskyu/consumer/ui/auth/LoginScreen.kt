package com.reskyu.consumer.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.consumer.data.model.LoginState
import com.reskyu.consumer.ui.components.LoadingOverlay
import com.reskyu.consumer.ui.navigation.Screen

/**
 * LoginScreen
 *
 * Unified auth screen supporting two methods:
 *  Tab 0 — Phone (OTP): 2-step, phone number then 6-digit code
 *  Tab 1 — Email: toggle between Sign In and Sign Up (adds Name field)
 *
 * On success → navigates to [Screen.Main], clearing the auth back stack.
 */
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = viewModel()
) {
    val loginState by viewModel.loginState.collectAsState()
    val otpSent by viewModel.otpSent.collectAsState()
    val focusManager = LocalFocusManager.current
    val otpFocusRequester = remember { FocusRequester() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("📱  Phone", "✉️  Email")

    // Email form state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSignUp by remember { mutableStateOf(false) }

    // Phone form state
    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }

    // Navigate on success
    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    // Auto-focus OTP field
    LaunchedEffect(otpSent) {
        if (otpSent) try { otpFocusRequester.requestFocus() } catch (_: Exception) {}
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Coloured header band ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .background(MaterialTheme.colorScheme.primaryContainer)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            // ── Branding ──────────────────────────────────────────────────────
            Text(
                text = "Welcome to Reskyu 🍱",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Rescue surplus food. Save money.\nHelp the planet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Auth Card ─────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    // Tab selector
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = {
                                    selectedTab = index
                                    viewModel.resetState()
                                    otp = ""
                                    viewModel.resetOtp()
                                },
                                text = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Phone Tab ─────────────────────────────────────────────
                    if (selectedTab == 0) {
                        PhoneAuthSection(
                            phoneNumber = phoneNumber,
                            onPhoneChange = { if (it.length <= 13) phoneNumber = it },
                            otp = otp,
                            onOtpChange = { if (it.length <= 6) otp = it },
                            otpSent = otpSent,
                            otpFocusRequester = otpFocusRequester,
                            loginState = loginState,
                            onSendOtp = {
                                focusManager.clearFocus()
                                viewModel.sendOtp(phoneNumber)
                            },
                            onVerifyOtp = {
                                focusManager.clearFocus()
                                viewModel.verifyOtp(otp)
                            },
                            onResend = {
                                otp = ""
                                viewModel.resetOtp()
                                viewModel.sendOtp(phoneNumber)
                            }
                        )
                    }

                    // ── Email Tab ─────────────────────────────────────────────
                    if (selectedTab == 1) {
                        EmailAuthSection(
                            name = name,
                            onNameChange = { name = it },
                            email = email,
                            onEmailChange = { email = it },
                            password = password,
                            onPasswordChange = { password = it },
                            passwordVisible = passwordVisible,
                            onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                            isSignUp = isSignUp,
                            loginState = loginState,
                            onAction = {
                                focusManager.clearFocus()
                                if (isSignUp) viewModel.signUpWithEmail(name, email, password)
                                else viewModel.signInWithEmail(email, password)
                            },
                            onToggleMode = {
                                isSignUp = !isSignUp
                                viewModel.resetState()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "By continuing, you agree to our Terms & Privacy Policy.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── DEV BYPASS — remove before production ─────────────────────────
            OutlinedButton(
                onClick = { viewModel.devBypass() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    "⚡ Skip Login (Dev Mode)",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            // ── END DEV BYPASS ────────────────────────────────────────────────

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (loginState is LoginState.Loading) LoadingOverlay()
    }
}

// ── Phone Auth Section ────────────────────────────────────────────────────────

@Composable
private fun PhoneAuthSection(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    otp: String,
    onOtpChange: (String) -> Unit,
    otpSent: Boolean,
    otpFocusRequester: FocusRequester,
    loginState: LoginState,
    onSendOtp: () -> Unit,
    onVerifyOtp: () -> Unit,
    onResend: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneChange,
            label = { Text("Phone Number") },
            placeholder = { Text("+91 98765 43210") },
            leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !otpSent,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            shape = MaterialTheme.shapes.medium
        )

        AnimatedVisibility(
            visible = otpSent,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut()
        ) {
            OutlinedTextField(
                value = otp,
                onValueChange = onOtpChange,
                label = { Text("6-Digit OTP") },
                placeholder = { Text("••••••") },
                modifier = Modifier.fillMaxWidth().focusRequester(otpFocusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onVerifyOtp() }),
                shape = MaterialTheme.shapes.medium
            )
        }

        if (loginState is LoginState.Error) {
            Text(
                text = (loginState as LoginState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = { if (!otpSent) onSendOtp() else onVerifyOtp() },
            enabled = loginState !is LoginState.Loading &&
                    if (!otpSent) phoneNumber.isNotBlank() else otp.length == 6,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(if (!otpSent) "Send OTP" else "Verify & Continue",
                style = MaterialTheme.typography.labelLarge)
        }

        AnimatedVisibility(visible = otpSent) {
            TextButton(onClick = onResend, modifier = Modifier.fillMaxWidth()) {
                Text("Didn't receive OTP? Resend")
            }
        }
    }
}

// ── Email Auth Section ────────────────────────────────────────────────────────

@Composable
private fun EmailAuthSection(
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    isSignUp: Boolean,
    loginState: LoginState,
    onAction: () -> Unit,
    onToggleMode: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Name field — only for Sign Up
        AnimatedVisibility(
            visible = isSignUp,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
            exit = fadeOut()
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = MaterialTheme.shapes.medium
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            shape = MaterialTheme.shapes.medium
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisible) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff
                                      else Icons.Rounded.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None
                                   else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onAction() }),
            shape = MaterialTheme.shapes.medium
        )

        if (loginState is LoginState.Error) {
            Text(
                text = (loginState as LoginState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = onAction,
            enabled = loginState !is LoginState.Loading &&
                    email.isNotBlank() && password.length >= 6 &&
                    if (isSignUp) name.isNotBlank() else true,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = if (isSignUp) "Create Account" else "Sign In",
                style = MaterialTheme.typography.labelLarge
            )
        }

        TextButton(onClick = onToggleMode, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isSignUp) "Already have an account? Sign In"
                       else "New here? Create an account",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}
