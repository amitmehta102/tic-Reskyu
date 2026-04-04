package com.reskyu.consumer.ui.update

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val UGreenDark   = Color(0xFF0C1E13)
private val UGreenAccent = Color(0xFF52B788)
private val UGreenMid    = Color(0xFF1F5235)
private val UGreenLight  = Color(0xFFE8F5E9)

@Composable
fun ForceUpdateDialog() {
    val context = LocalContext.current

    // Intercept back press — user cannot dismiss this
    BackHandler(enabled = true) { /* swallow */ }

    Dialog(
        onDismissRequest = { /* non-dismissible */ },
        properties = DialogProperties(
            dismissOnBackPress    = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(UGreenDark, Color(0xFF163823), UGreenMid)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Icon badge
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(UGreenAccent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(UGreenAccent.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            tint               = UGreenAccent,
                            modifier           = Modifier.size(32.dp)
                        )
                    }
                }

                // Heading
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Update Required",
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White
                    )
                    Text(
                        "A critical update is available for Reskyu. Please update to the latest version to continue.",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }

                // Info card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        UpdateInfoRow("🛡️", "Security & stability improvements")
                        UpdateInfoRow("🐛", "Critical bug fixes included")
                        UpdateInfoRow("⚡", "Better performance & new features")
                    }
                }

                // Update button
                Button(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=com.reskyu.consumer")
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape  = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UGreenAccent,
                        contentColor   = Color.White
                    )
                ) {
                    Icon(
                        Icons.Rounded.SystemUpdate, null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Update Now",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    "You must update to continue using Reskyu.",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = Color.White.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun UpdateInfoRow(emoji: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 16.sp)
        Text(
            text,
            style  = MaterialTheme.typography.bodySmall,
            color  = Color.White.copy(alpha = 0.80f),
            fontWeight = FontWeight.Medium
        )
    }
}
