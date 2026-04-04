package com.reskyu.merchant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reskyu.merchant.ui.theme.RGreenDark
import com.reskyu.merchant.ui.theme.RGreenDeep
import com.reskyu.merchant.ui.theme.RGreenLight
import com.reskyu.merchant.ui.theme.RGreenMid

/**
 * Shared hero-style header used by every content screen.
 *
 * Matches the Dashboard header visual language:
 *  - 3-stop vertical gradient  (RGreenDark → RGreenDeep → RGreenMid)
 *  - Rounded bottom corners    (32 dp)
 *  - Generous internal padding (top 20 dp, bottom 24–28 dp)
 *  - Handles [statusBarsPadding] internally
 *
 * @param title        Large title text shown in white bold.
 * @param subtitle     Optional caption line below the title (light tint).
 * @param onBack       If non-null, shows a back-arrow icon button on the left.
 * @param trailing     Optional composable slot on the far right (icons, menus).
 * @param bottomContent  Optional composable rendered below the title row
 *                       (e.g. a tab row for Order Management).
 */
@Composable
fun ReskyuHeader(
    title         : String,
    subtitle      : String?                   = null,
    onBack        : (() -> Unit)?             = null,
    trailing      : (@Composable () -> Unit)? = null,
    bottomContent : (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(
                Brush.verticalGradient(listOf(RGreenDark, RGreenDeep, RGreenMid))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(
                    top    = 20.dp,
                    bottom = if (bottomContent != null) 12.dp else 28.dp
                )
        ) {
            // ── Title row ─────────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Optional back-arrow button
                if (onBack != null) {
                    IconButton(
                        onClick  = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                    ) {
                        Icon(
                            imageVector        = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint               = Color.White,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                }

                // Title + subtitle column
                Column(modifier = Modifier.weight(1f)) {
                    if (subtitle != null) {
                        Text(
                            text          = subtitle,
                            fontSize      = 12.sp,
                            color         = RGreenLight,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(
                        text       = title,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }

                // Optional trailing slot (icon buttons, menus, etc.)
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        trailing()
                    }
                }
            }

            // ── Optional bottom slot (tab bar, search, etc.) ──────────────────
            if (bottomContent != null) {
                Spacer(Modifier.height(14.dp))
                bottomContent()
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
