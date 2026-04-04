package com.reskyu.consumer.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.consumer.data.model.LoginState
import com.reskyu.consumer.ui.components.LoadingOverlay
import com.reskyu.consumer.ui.navigation.Screen

// ── Theme colors matching the Reskyu Merchant app ─────────────────────────────

private val GreenDark    = Color(0xFF0A2E1A)   // deep forest green background
private val GreenMid     = Color(0xFF0D3D22)   // slightly lighter band
private val GreenAccent  = Color(0xFF2DC653)   // vibrant button green
private val GreenSurface = Color(0xFFE8F5ED)   // light mint card background
private val GreenOnCard  = Color(0xFF1B4332)   // text on card
private val GreenSubtle  = Color(0xFF4CAF50)   // tab indicator / muted accent

/**
 * LoginScreen — Reskyu Consumer
 *
 * Full Reskyu brand theme:
 *  ── Deep forest green gradient background (top-to-bottom)
 *  ── 🌱 Sprout icon + "Reskyu" white bold heading
 *  ── "CONSUMER PORTAL" spacious teal subtitle
 *  ── Floating mint-green card with auth form
 *  ── Bright green primary button
 *  ── Dev bypass link at bottom
 */
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = viewModel()
) {
    val loginState by viewModel.loginState.collectAsState()
    val otpSent    by viewModel.otpSent.collectAsState()
    val focusManager       = LocalFocusManager.current
    val otpFocusRequester  = remember { FocusRequester() }

    var selectedTab      by remember { mutableIntStateOf(0) }
    val tabs              = listOf("📱  Phone", "✉️  Email")

    var email            by remember { mutableStateOf("") }
    var password         by remember { mutableStateOf("") }
    var name             by remember { mutableStateOf("") }
    var passwordVisible  by remember { mutableStateOf(false) }
    var isSignUp         by remember { mutableStateOf(false) }
    var phoneNumber      by remember { mutableStateOf("") }
    var otp              by remember { mutableStateOf("") }

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }
    LaunchedEffect(otpSent) {
        if (otpSent) try { otpFocusRequester.requestFocus() } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GreenDark, GreenMid, Color(0xFF0F4828))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            // ── Branding ──────────────────────────────────────────────────────
            Text("🌱", fontSize = 56.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Reskyu",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "CONSUMER  PORTAL",
                style = MaterialTheme.typography.labelMedium,
                color = GreenAccent,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(44.dp))

            // ── Auth Card ─────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = GreenSurface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    Text(
                        if (selectedTab == 1 && isSignUp) "Create Account"
                        else "Welcome back",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GreenOnCard
                    )
                    Text(
                        if (selectedTab == 1 && isSignUp)
                            "Join Reskyu and start rescuing food"
                        else
                            "Sign in to rescue food near you",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4B7A5A),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // Tab selector
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFFD4EAD9),
                        contentColor = GreenAccent
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
                                        title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTab == index) GreenOnCard
                                                else Color(0xFF4B7A5A)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Phone Tab ──────────────────────────────────────────────
                    if (selectedTab == 0) {
                        PhoneAuthSection(
                            phoneNumber = phoneNumber,
                            onPhoneChange = { if (it.length <= 13) phoneNumber = it },
                            otp = otp,
                            onOtpChange = { if (it.length <= 6) otp = it },
                            otpSent = otpSent,
                            otpFocusRequester = otpFocusRequester,
                            loginState = loginState,
                            accentColor = GreenAccent,
                            onSendOtp = { focusManager.clearFocus(); viewModel.sendOtp(phoneNumber) },
                            onVerifyOtp = { focusManager.clearFocus(); viewModel.verifyOtp(otp) },
                            onResend = { otp = ""; viewModel.resetOtp(); viewModel.sendOtp(phoneNumber) }
                        )
                    }

                    // ── Email Tab ──────────────────────────────────────────────
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
                            accentColor = GreenAccent,
                            onAction = {
                                focusManager.clearFocus()
                                if (isSignUp) viewModel.signUpWithEmail(name, email, password)
                                else viewModel.signInWithEmail(email, password)
                            },
                            onToggleMode = { isSignUp = !isSignUp; viewModel.resetState() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "By continuing, you agree to our Terms & Privacy Policy.",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF7DBF96),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // ── Dev bypass ─────────────────────────────────────────────────────
            TextButton(
                onClick = { viewModel.devBypass() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "⚡ Dev Mode — Skip Login",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF7DBF96)
                )
            }

            Spacer(Modifier.height(32.dp))
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
    accentColor: Color,
    onSendOtp: () -> Unit,
    onVerifyOtp: () -> Unit,
    onResend: () -> Unit
) {
    val fieldColors = greenFieldColors()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneChange,
            label = { Text("Phone Number") },
            placeholder = { Text("+91 98765 43210") },
            leadingIcon = { Icon(Icons.Rounded.Phone, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !otpSent,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors
        )

        AnimatedVisibility(
            visible = otpSent,
            enter  = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit   = fadeOut()
        ) {
            OutlinedTextField(
                value = otp,
                onValueChange = onOtpChange,
                label = { Text("6-Digit OTP") },
                placeholder = { Text("••••••") },
                modifier = Modifier.fillMaxWidth().focusRequester(otpFocusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onVerifyOtp() }),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors
            )
        }

        if (loginState is LoginState.Error) {
            Text(
                loginState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = { if (!otpSent) onSendOtp() else onVerifyOtp() },
            enabled = loginState !is LoginState.Loading &&
                    if (!otpSent) phoneNumber.isNotBlank() else otp.length == 6,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor         = accentColor,
                contentColor           = Color.White,
                disabledContainerColor = Color(0xFF2DC653).copy(alpha = 0.35f),
                disabledContentColor   = Color.White.copy(alpha = 0.6f)
            )
        ) {
            Text(
                if (!otpSent) "Send OTP" else "Verify & Continue",
                style = MaterialTheme.typography.labelLarge
            )
        }

        AnimatedVisibility(visible = otpSent) {
            TextButton(onClick = onResend, modifier = Modifier.fillMaxWidth()) {
                Text("Didn't receive OTP? Resend", color = accentColor)
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
    accentColor: Color,
    onAction: () -> Unit,
    onToggleMode: () -> Unit
) {
    val fieldColors = greenFieldColors()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        AnimatedVisibility(
            visible = isSignUp,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
            exit  = fadeOut()
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Rounded.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Rounded.Email, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Rounded.Lock, null) },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisible) {
                    Icon(
                        if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (passwordVisible) "Hide" else "Show"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAction() }),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors
        )

        if (loginState is LoginState.Error) {
            Text(
                loginState.message,
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
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor         = accentColor,
                contentColor           = Color.White,
                disabledContainerColor = Color(0xFF2DC653).copy(alpha = 0.35f),
                disabledContentColor   = Color.White.copy(alpha = 0.6f)
            )
        ) {
            Text(
                if (isSignUp) "Create Account" else "Sign In",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        TextButton(onClick = onToggleMode, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (isSignUp) "Already have an account? Sign In"
                else          "New here? Create an account",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = Color(0xFF2E7D32)
            )
        }
    }
}

// ── Shared field colors ────────────────────────────────────────────────────────

@Composable
private fun greenFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor    = GreenAccent,
    unfocusedBorderColor  = Color(0xFFB2DFBB),
    focusedLabelColor     = GreenAccent,
    unfocusedLabelColor   = Color(0xFF4B7A5A),
    focusedLeadingIconColor   = GreenAccent,
    unfocusedLeadingIconColor = Color(0xFF4B7A5A),
    cursorColor           = GreenAccent,
    focusedTextColor      = GreenOnCard,
    unfocusedTextColor    = GreenOnCard,
    unfocusedContainerColor = Color(0xFFF0FAF1),
    focusedContainerColor   = Color.White
)


