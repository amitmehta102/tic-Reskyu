package com.reskyu.merchant.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reskyu.merchant.data.model.LoginState
import com.reskyu.merchant.ui.components.LoadingOverlay
import com.reskyu.merchant.ui.navigation.Screen
import kotlinx.coroutines.launch

// ── Brand palette (login-local until theme is updated app-wide) ───────────────
private val GreenDark   = Color(0xFF0C1E13)
private val GreenDeep   = Color(0xFF163823)
private val GreenMid    = Color(0xFF1F5235)
private val GreenAccent = Color(0xFF52B788)
private val CardBg      = Color(0xFFF4FBF6)

/**
 * Login screen with a dark forest-green gradient background, an animated
 * card form, and a subtle Dev Mode bypass for development.
 *
 * ⚡ DEV MODE: "Dev Mode — Skip Login" button bypasses Firebase auth entirely
 *    and navigates straight to DASHBOARD.
 */
@Composable
fun MerchantLoginScreen(
    navController: NavController,
    viewModel: MerchantLoginViewModel = viewModel()
) {
    val loginState by viewModel.loginState.collectAsState()

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Card entrance animation: fade-in + slide-up
    val cardAlpha = remember { Animatable(0f) }
    val cardSlide = remember { Animatable(56f) }

    LaunchedEffect(Unit) {
        launch { cardAlpha.animateTo(1f, tween(durationMillis = 500)) }
        launch { cardSlide.animateTo(0f, tween(durationMillis = 500)) }
    }

    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is LoginState.Success -> {
                val dest = if (state.merchant != null) Screen.DASHBOARD else Screen.ONBOARDING
                navController.navigate(dest) {
                    popUpTo(Screen.LOGIN) { inclusive = true }
                }
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(GreenDark, GreenDeep, GreenMid))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ─── Hero / Branding ─────────────────────────────────────────────
            Spacer(modifier = Modifier.weight(1.3f))

            Text(text = "🌱", fontSize = 56.sp)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Reskyu",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "MERCHANT  PORTAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = GreenAccent.copy(alpha = 0.85f),
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.weight(0.8f))

            // ─── Login Card ──────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(cardAlpha.value)
                    .offset(y = cardSlide.value.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    Text(
                        text = "Welcome back",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GreenDeep
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sign in to manage your listings",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenAccent,
                            unfocusedBorderColor = Color(0xFFCDD9D2),
                            focusedLabelColor = GreenAccent,
                            focusedTextColor = GreenDeep,
                            unfocusedTextColor = GreenDeep,
                            cursorColor = GreenAccent,
                            unfocusedLabelColor = Color.Gray
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenAccent,
                            unfocusedBorderColor = Color(0xFFCDD9D2),
                            focusedLabelColor = GreenAccent,
                            focusedTextColor = GreenDeep,
                            unfocusedTextColor = GreenDeep,
                            cursorColor = GreenAccent,
                            unfocusedLabelColor = Color.Gray
                        )
                    )

                    if (loginState is LoginState.Error) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = (loginState as LoginState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.signIn(email, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = loginState !is LoginState.Loading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenAccent,
                            contentColor = Color.White,
                            disabledContainerColor = GreenAccent.copy(alpha = 0.45f),
                            disabledContentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Sign In",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ─── Dev Mode Bypass ─────────────────────────────────────────────
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    navController.navigate(Screen.DASHBOARD) {
                        popUpTo(Screen.LOGIN) { inclusive = true }
                    }
                }
            ) {
                Text(
                    text = "⚡  Dev Mode — Skip Login",
                    color = Color.White.copy(alpha = 0.32f),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.2.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        LoadingOverlay(isVisible = loginState is LoginState.Loading)
    }
}
