package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusOnline

@Composable
fun AvatarInitials(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showOnlineBadge: Boolean = false,
    isOnline: Boolean = false
) {
    val initials = name.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .ifEmpty { "?" }

    val gradientColors = getAvatarColors(name)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = (size.value * 0.38f).sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (showOnlineBadge) {
            val badgeSize = (size.value * 0.28f).coerceAtLeast(10f).dp
            Box(
                modifier = Modifier
                    .size(badgeSize)
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) StatusOnline else StatusOffline)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

private fun getAvatarColors(name: String): List<Color> {
    val hash = name.hashCode()
    val palettes = listOf(
        listOf(Color(0xFF6366F1), Color(0xFF4F46E5)),
        listOf(Color(0xFF0EA5E9), Color(0xFF0284C7)),
        listOf(Color(0xFF10B981), Color(0xFF059669)),
        listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED)),
        listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
        listOf(Color(0xFFEC4899), Color(0xFFDB2777))
    )
    val index = kotlin.math.abs(hash) % palettes.size
    return palettes[index]
}
