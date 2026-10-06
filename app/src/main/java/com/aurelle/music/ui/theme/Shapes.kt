package com.aurelle.music.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================================
// CUSTOM SHAPES
// ============================================================================

val AurelleCardShape = RoundedCornerShape(16.dp)
val AurelleSmallCardShape = RoundedCornerShape(12.dp)
val AurelleButtonShape = RoundedCornerShape(24.dp)
val AurelleNavBarShape = RoundedCornerShape(34.dp)
val AurelleNavPillShape = RoundedCornerShape(26.dp)
val AurelleSearchBarShape = RoundedCornerShape(27.dp)
val AurelleAlbumCoverShape = RoundedCornerShape(14.dp)
val AurelleArtistCoverShape: CornerBasedShape = CircleShape

val TopStartCornerSize = CornerSize(16.dp)
val TopEndCornerSize = CornerSize(16.dp)
val BottomStartCornerSize = CornerSize(16.dp)
val BottomEndCornerSize = CornerSize(16.dp)

val AurelleTopHeavyShape = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomStart = 12.dp,
    bottomEnd = 12.dp
)

val AurelleBottomHeavyShape = RoundedCornerShape(
    topStart = 12.dp,
    topEnd = 12.dp,
    bottomStart = 20.dp,
    bottomEnd = 20.dp
)

// ============================================================================
// SHADOWS
// ============================================================================

val AurelleShadowSmall = Shadow(
    color = Color.Black.copy(alpha = 0.15f),
    blurRadius = 4f,
    offsetX = 0f,
    offsetY = 2f
)

val AurelleShadowMedium = Shadow(
    color = Color.Black.copy(alpha = 0.2f),
    blurRadius = 8f,
    offsetX = 0f,
    offsetY = 4f
)

val AurelleShadowLarge = Shadow(
    color = Color.Black.copy(alpha = 0.25f),
    blurRadius = 16f,
    offsetX = 0f,
    offsetY = 8f
)

fun aurelleGoldShadow(alpha: Float = 0.2f): Shadow = Shadow(
    color = AurelleGold.copy(alpha = alpha),
    blurRadius = 8f,
    offsetX = 0f,
    offsetY = 4f
)

// ============================================================================
// GRADIENTS
// ============================================================================

fun aurelleBackgroundGradient(): Brush = Brush.verticalGradient(
    listOf(AurelleBackgroundTop, AurelleBackgroundBottom)
)

fun aurelleCardGradient(): Brush = Brush.linearGradient(
    listOf(AurelleSurfaceHigh, AurelleSurface)
)

fun aurelleGoldGradient(): Brush = Brush.linearGradient(
    listOf(AurelleGoldDark, AurelleGold, AurelleGoldLight)
)

fun aurellePurpleGradient(): Brush = Brush.linearGradient(
    listOf(AurellePurpleGlow, AurelleGold)
)

fun aurelleGlowGradient(center: Offset = Offset(0.5f, 0.5f)): Brush = Brush.radialGradient(
    colors = listOf(AurellePurpleGlow.copy(alpha = 0.4f), Color.Transparent),
    center = center,
    radius = 0.5f
)

// ============================================================================
// BORDER STYLES
// ============================================================================

fun aurelleCardBorder(width: Dp = 1.dp, alpha: Float = 0.12f): DpBorder = DpBorder(width, AurelleGold.copy(alpha = alpha))
fun aurelleSelectedBorder(width: Dp = 2.dp, alpha: Float = 1f): DpBorder = DpBorder(width, AurelleGold.copy(alpha = alpha))
fun aurelleGoldBorder(width: Dp = 1.5.dp): DpBorder = DpBorder(width, AurelleGold)

@JvmInline
value class DpBorder(val width: Dp, val color: Color)
