package com.reskyu.consumer.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

// ── Theme colors ───────────────────────────────────────────────────────────────
private val GreenDark    = Color(0xFF0A2E1A)
private val GreenMid     = Color(0xFF0D3D22)
private val GreenAccent  = Color(0xFF2DC653)
private val GreenSurface = Color(0xFFE8F5ED)
private val GreenOnCard  = Color(0xFF1B4332)

// Consumer types
enum class ConsumerType(val label: String, val emoji: String, val description: String) {
    INDIVIDUAL("Individual", "🙋", "Personal use — rescue food for yourself"),
    NGO("NGO / Organisation", "🏢", "Bulk rescue for a charity or community")
}

/**
 * LoginScreen — email-only auth + consumer type selection on sign-up
 */
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = viewModel()
) {
    val loginState   by viewModel.loginState.collectAsState()
    val focusManager = LocalFocusManager.current

    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var name            by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSignUp        by remember { mutableStateOf(false) }
    var consumerType    by remember { mutableStateOf(ConsumerType.INDIVIDUAL) }

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(GreenDark, GreenMid, Color(0xFF0F4828)))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))

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

            Spacer(Modifier.height(40.dp))

            // ── Auth Card ─────────────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors    = CardDefaults.cardColors(containerColor = GreenSurface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    // Title
                    Text(
                        if (isSignUp) "Create Account" else "Welcome back",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GreenOnCard
                    )
                    Text(
                        if (isSignUp) "Join Reskyu and start rescuing food"
                        else "Sign in to rescue food near you",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4B7A5A),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // ── Consumer type picker (sign-up only) ───────────────────
                    AnimatedVisibility(
                        visible = isSignUp,
                        enter   = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                        exit    = fadeOut()
                    ) {
                        Column {
                            Text(
                                "I am registering as",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GreenOnCard,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ConsumerType.entries.forEach { type ->
                                    ConsumerTypeCard(
                                        type     = type,
                                        selected = consumerType == type,
                                        onClick  = { consumerType = type },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                        }
                    }

                    // ── Email form ────────────────────────────────────────────
                    EmailAuthSection(
                        name                   = name,
                        onNameChange           = { name = it },
                        email                  = email,
                        onEmailChange          = { email = it },
                        password               = password,
                        onPasswordChange       = { password = it },
                        passwordVisible        = passwordVisible,
                        onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                        isSignUp               = isSignUp,
                        loginState             = loginState,
                        accentColor            = GreenAccent,
                        onAction = {
                            focusManager.clearFocus()
                            if (isSignUp) viewModel.signUpWithEmail(name, email, password, consumerType.name)
                            else viewModel.signInWithEmail(email, password)
                        },
                        onToggleMode = {
                            isSignUp = !isSignUp
                            consumerType = ConsumerType.INDIVIDUAL
                            viewModel.resetState()
                        }
                    )
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

            // ── Dev bypass ────────────────────────────────────────────────────
            TextButton(
                onClick  = { viewModel.devBypass() },
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

// ── Consumer type card ─────────────────────────────────────────────────────────

@Composable
private fun ConsumerTypeCard(
    type:     ConsumerType,
    selected: Boolean,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) GreenAccent else Color(0xFFB2DFBB)
    val bgColor     = if (selected) Color(0xFFD6F5E0) else Color(0xFFF0FAF1)
    val textColor   = if (selected) GreenOnCard else Color(0xFF4B7A5A)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(type.emoji, fontSize = 26.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                type.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Text(
                type.description,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

// ── Email Auth Section ────────────────────────────────────────────────────────

@Composable
private fun EmailAuthSection(
    name:                   String,
    onNameChange:           (String) -> Unit,
    email:                  String,
    onEmailChange:          (String) -> Unit,
    password:               String,
    onPasswordChange:       (String) -> Unit,
    passwordVisible:        Boolean,
    onTogglePasswordVisible: () -> Unit,
    isSignUp:               Boolean,
    loginState:             LoginState,
    accentColor:            Color,
    onAction:               () -> Unit,
    onToggleMode:           () -> Unit
) {
    val fieldColors = greenFieldColors()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Full name — sign-up only
        AnimatedVisibility(
            visible = isSignUp,
            enter   = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
            exit    = fadeOut()
        ) {
            OutlinedTextField(
                value           = name,
                onValueChange   = onNameChange,
                label           = { Text("Full Name") },
                leadingIcon     = { Icon(Icons.Rounded.Person, null) },
                modifier        = Modifier.fillMaxWidth(),
                singleLine      = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape           = RoundedCornerShape(14.dp),
                colors          = fieldColors
            )
        }

        OutlinedTextField(
            value           = email,
            onValueChange   = onEmailChange,
            label           = { Text("Email") },
            leadingIcon     = { Icon(Icons.Rounded.Email, null) },
            modifier        = Modifier.fillMaxWidth(),
            singleLine      = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            shape           = RoundedCornerShape(14.dp),
            colors          = fieldColors
        )

        OutlinedTextField(
            value           = password,
            onValueChange   = onPasswordChange,
            label           = { Text("Password") },
            leadingIcon     = { Icon(Icons.Rounded.Lock, null) },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisible) {
                    Icon(
                        if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (passwordVisible) "Hide" else "Show"
                    )
                }
            },
            modifier              = Modifier.fillMaxWidth(),
            singleLine            = true,
            visualTransformation  = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions       = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions       = KeyboardActions(onDone = { onAction() }),
            shape                 = RoundedCornerShape(14.dp),
            colors                = fieldColors
        )

        if (loginState is LoginState.Error) {
            Text(
                loginState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick  = onAction,
            enabled  = loginState !is LoginState.Loading &&
                    email.isNotBlank() && password.length >= 6 &&
                    if (isSignUp) name.isNotBlank() else true,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = accentColor,
                contentColor           = Color.White,
                disabledContainerColor = GreenAccent.copy(alpha = 0.35f),
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
                else "New here? Create an account",
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
    focusedBorderColor        = GreenAccent,
    unfocusedBorderColor      = Color(0xFFB2DFBB),
    focusedLabelColor         = GreenAccent,
    unfocusedLabelColor       = Color(0xFF4B7A5A),
    focusedLeadingIconColor   = GreenAccent,
    unfocusedLeadingIconColor = Color(0xFF4B7A5A),
    cursorColor               = GreenAccent,
    focusedTextColor          = GreenOnCard,
    unfocusedTextColor        = GreenOnCard,
    unfocusedContainerColor   = Color(0xFFF0FAF1),
    focusedContainerColor     = Color.White
)
