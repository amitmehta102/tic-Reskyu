package com.reskyu.merchant.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.merchant.data.model.LoginState
import com.reskyu.merchant.ui.components.LoadingOverlay
import com.reskyu.merchant.ui.navigation.Screen
import kotlinx.coroutines.launch

import com.reskyu.merchant.ui.theme.RGreenAccent
import com.reskyu.merchant.ui.theme.RGreenDark
import com.reskyu.merchant.ui.theme.RGreenDeep
import com.reskyu.merchant.ui.theme.RGreenMid

// ── Brand palette ─────────────────────────────────────────────────────────────
private val GreenDark   = RGreenDark
private val GreenDeep   = RGreenDeep
private val GreenMid    = RGreenMid
private val GreenAccent = RGreenAccent
private val CardBg      = Color(0xFFF4FBF6)

@Composable
fun MerchantLoginScreen(
    navController: NavController,
    viewModel: MerchantLoginViewModel = viewModel()
) {
    val loginState       by viewModel.loginState.collectAsState()
    val passwordResetSent by viewModel.passwordResetSent.collectAsState()

    // Tab: 0 = Sign In, 1 = Create Account
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Sign-In form
    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Register form
    var regEmail           by remember { mutableStateOf("") }
    var regPassword        by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var showRegPassword    by remember { mutableStateOf(false) }

    // Forgot-password dialog
    var showForgotDialog by remember { mutableStateOf(false) }
    var resetEmail       by remember { mutableStateOf("") }

    // Reset-sent banner
    var showResetBanner by remember { mutableStateOf(false) }
    LaunchedEffect(passwordResetSent) {
        if (passwordResetSent) { showResetBanner = true; viewModel.resetPasswordResetSent() }
    }

    // Card entrance animation
    val cardAlpha = remember { Animatable(0f) }
    val cardSlide = remember { Animatable(56f) }
    LaunchedEffect(Unit) {
        launch { cardAlpha.animateTo(1f, tween(500)) }
        launch { cardSlide.animateTo(0f, tween(500)) }
    }

    // Navigate on success
    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is LoginState.Success -> {
                val dest = if (state.merchant != null) Screen.DASHBOARD else Screen.ONBOARDING
                navController.navigate(dest) { popUpTo(Screen.LOGIN) { inclusive = true } }
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // Forgot-password dialog
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            containerColor   = CardBg,
            title = {
                Text(
                    "Reset Password",
                    fontWeight = FontWeight.Bold,
                    color      = GreenDeep
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enter your email address and we'll send you a reset link.",
                        color    = Color.DarkGray,
                        fontSize = 14.sp
                    )
                    AuthTextField(
                        value         = resetEmail,
                        onValueChange = { resetEmail = it },
                        label         = "Email address"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick  = { viewModel.sendPasswordReset(resetEmail); showForgotDialog = false },
                    colors   = ButtonDefaults.buttonColors(containerColor = GreenAccent)
                ) { Text("Send Reset Link") }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GreenDark, GreenDeep, GreenMid)))
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Hero ──────────────────────────────────────────────────────────
            Spacer(Modifier.height(64.dp))
            Text("🌱", fontSize = 56.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                "Reskyu",
                fontSize   = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                letterSpacing = (-1).sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "MERCHANT  PORTAL",
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color      = GreenAccent.copy(alpha = 0.85f),
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(40.dp))

            // ── Card ──────────────────────────────────────────────────────────
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .alpha(cardAlpha.value)
                    .offset(y = cardSlide.value.dp),
                shape     = RoundedCornerShape(24.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(20.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {

                    // ── Tab row ───────────────────────────────────────────────
                    AuthTabRow(
                        selectedTab = selectedTab,
                        onTabChange = { selectedTab = it; viewModel.resetState(); showResetBanner = false }
                    )
                    Spacer(Modifier.height(24.dp))

                    // ── Tab content ───────────────────────────────────────────
                    val errorMsg = (loginState as? LoginState.Error)?.message
                    val isLoading = loginState is LoginState.Loading

                    if (selectedTab == 0) {
                        SignInForm(
                            email              = email,
                            onEmailChange      = { email = it },
                            password           = password,
                            onPasswordChange   = { password = it },
                            showPassword       = showPassword,
                            onTogglePassword   = { showPassword = !showPassword },
                            onSignIn           = { viewModel.signIn(email, password) },
                            onForgotPassword   = { resetEmail = email; showForgotDialog = true },
                            isLoading          = isLoading,
                            error              = errorMsg
                        )
                    } else {
                        RegisterForm(
                            email                    = regEmail,
                            onEmailChange            = { regEmail = it },
                            password                 = regPassword,
                            onPasswordChange         = { regPassword = it },
                            confirmPassword          = regConfirmPassword,
                            onConfirmPasswordChange  = { regConfirmPassword = it },
                            showPassword             = showRegPassword,
                            onTogglePassword         = { showRegPassword = !showRegPassword },
                            onRegister               = { viewModel.register(regEmail, regPassword, regConfirmPassword) },
                            isLoading                = isLoading,
                            error                    = errorMsg
                        )
                    }

                    // ── Reset-sent confirmation ────────────────────────────────
                    if (showResetBanner) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "✅  Password reset email sent!",
                            color      = Color(0xFF2D6A4F),
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Dev Mode ──────────────────────────────────────────────────────
            HorizontalDivider(
                color    = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(Modifier.height(24.dp))
        }


        LoadingOverlay(isVisible = loginState is LoginState.Loading)
    }
}

// ── Tab row ───────────────────────────────────────────────────────────────────

@Composable
private fun AuthTabRow(selectedTab: Int, onTabChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GreenDeep.copy(alpha = 0.08f))
            .padding(3.dp)
    ) {
        listOf("Sign In", "Create Account").forEachIndexed { index, label ->
            val selected = selectedTab == index
            Box(
                modifier         = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) GreenAccent else Color.Transparent)
                    .clickable { onTabChange(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = label,
                    fontSize   = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (selected) Color.White else Color(0xFF6B7280)
                )
            }
        }
    }
}

// ── Sign-in form ──────────────────────────────────────────────────────────────

@Composable
private fun SignInForm(
    email: String,           onEmailChange: (String) -> Unit,
    password: String,        onPasswordChange: (String) -> Unit,
    showPassword: Boolean,   onTogglePassword: () -> Unit,
    onSignIn: () -> Unit,    onForgotPassword: () -> Unit,
    isLoading: Boolean,      error: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AuthTextField(value = email, onValueChange = onEmailChange, label = "Email")
        PasswordTextField(
            value       = password,
            onValueChange = onPasswordChange,
            label       = "Password",
            showPassword  = showPassword,
            onToggle      = onTogglePassword
        )

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick         = onForgotPassword,
                contentPadding  = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("Forgot Password?", fontSize = 12.sp, color = GreenAccent)
            }
        }

        if (error != null) {
            Text(error, color = Color(0xFFDC2626), fontSize = 12.sp)
        }

        Button(
            onClick  = onSignIn,
            enabled  = !isLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = GreenAccent,
                contentColor           = Color.White,
                disabledContainerColor = GreenAccent.copy(alpha = 0.40f),
                disabledContentColor   = Color.White.copy(alpha = 0.6f)
            )
        ) {
            Text("Sign In", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

// ── Register form ─────────────────────────────────────────────────────────────

@Composable
private fun RegisterForm(
    email: String,              onEmailChange: (String) -> Unit,
    password: String,           onPasswordChange: (String) -> Unit,
    confirmPassword: String,    onConfirmPasswordChange: (String) -> Unit,
    showPassword: Boolean,      onTogglePassword: () -> Unit,
    onRegister: () -> Unit,
    isLoading: Boolean,         error: String?
) {
    val mismatch = confirmPassword.isNotEmpty() && password != confirmPassword
    val canSubmit = !isLoading
            && email.isNotBlank()
            && password.length >= 6
            && password == confirmPassword

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AuthTextField(value = email, onValueChange = onEmailChange, label = "Email")
        PasswordTextField(
            value         = password,
            onValueChange = onPasswordChange,
            label         = "Password (min. 6 chars)",
            showPassword  = showPassword,
            onToggle      = onTogglePassword
        )
        PasswordTextField(
            value         = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label         = "Confirm Password",
            showPassword  = showPassword,
            onToggle      = onTogglePassword
        )

        when {
            mismatch -> Text("Passwords don't match", color = Color(0xFFDC2626), fontSize = 12.sp)
            error != null -> Text(error, color = Color(0xFFDC2626), fontSize = 12.sp)
        }

        Button(
            onClick  = onRegister,
            enabled  = canSubmit,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = GreenAccent,
                contentColor           = Color.White,
                disabledContainerColor = GreenAccent.copy(alpha = 0.40f),
                disabledContentColor   = Color.White.copy(alpha = 0.6f)
            )
        ) {
            Text("Create Account", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }

        Text(
            "By creating an account you agree to Reskyu's Terms of Service.",
            fontSize  = 11.sp,
            color     = Color.Gray,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )
    }
}

// ── Shared field components ───────────────────────────────────────────────────

@Composable
private fun AuthTextField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        modifier      = Modifier.fillMaxWidth(),
        singleLine    = true,
        shape         = RoundedCornerShape(12.dp),
        colors        = authFieldColors()
    )
}

@Composable
private fun PasswordTextField(
    value: String, onValueChange: (String) -> Unit,
    label: String, showPassword: Boolean, onToggle: () -> Unit
) {
    OutlinedTextField(
        value               = value,
        onValueChange       = onValueChange,
        label               = { Text(label) },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon        = {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector        = if (showPassword) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = if (showPassword) "Hide password" else "Show password",
                    tint               = GreenAccent,
                    modifier           = Modifier.size(20.dp)
                )
            }
        },
        modifier   = Modifier.fillMaxWidth(),
        singleLine = true,
        shape      = RoundedCornerShape(12.dp),
        colors     = authFieldColors()
    )
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = GreenAccent,
    unfocusedBorderColor    = Color(0xFFCDD9D2),
    focusedLabelColor       = GreenAccent,
    unfocusedLabelColor     = Color.Gray,
    focusedTextColor        = GreenDeep,
    unfocusedTextColor      = GreenDeep,
    cursorColor             = GreenAccent
)
