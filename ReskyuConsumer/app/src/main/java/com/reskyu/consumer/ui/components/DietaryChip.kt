package com.reskyu.consumer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reskyu.consumer.data.model.DietaryTag

@Composable
fun DietaryChip(tag: DietaryTag) {
    val (label, color) = when (tag) {
        DietaryTag.VEG     -> "VEG"     to Color(0xFF4CAF50)
        DietaryTag.NON_VEG -> "NON-VEG" to Color(0xFFF44336)
        DietaryTag.VEGAN   -> "VEGAN"   to Color(0xFF00897B)
        DietaryTag.JAIN    -> "JAIN"    to Color(0xFFFF8F00)
        DietaryTag.BAKERY  -> "BAKERY"  to Color(0xFF8D6E63)   // warm brown
        DietaryTag.SWEETS  -> "SWEETS"  to Color(0xFFE91E8C)   // vibrant pink
    }

    Text(
        text = label,
        color = color,
        fontSize = 10.sp,
        modifier = Modifier
            .border(width = 1.dp, color = color, shape = RoundedCornerShape(4.dp))
            .background(color = color.copy(alpha = 0.08f), shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
