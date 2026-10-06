package com.aurelle.music.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// ============================================================================
// CUSTOM SHAPES - Formas customizadas para o design do app
// ============================================================================

/**
 * Formas customizadas para diferentes níveis de arredondamento.
 * Usado para manter consistência visual em todo o app.
 */

// Arredondamento padrão para cards
val AurelleCardShape = RoundedCornerShape(16.dp)

// Arredondamento para cards menores (como badges)
val AurelleSmallCardShape = RoundedCornerShape(12.dp)

// Arredondamento para botões
val AurelleButtonShape = RoundedCornerShape(24.dp)

// Arredondamento para a barra de navegação
val AurelleNavBarShape = RoundedCornerShape(34.dp)

// Arredondamento para itens de navegação (pílulas)
val AurelleNavPillShape = RoundedCornerShape(26.dp)

// Arredondamento para a barra de busca
val AurelleSearchBarShape = RoundedCornerShape(27.dp)

// Arredondamento para capas de álbuns
val AurelleAlbumCoverShape = RoundedCornerShape(14.dp)

// Arredondamento para capas de artistas (circular)
val AurelleArtistCoverShape = androidx.compose.foundation.shape.CircleShape

// ============================================================================
// CUSTOM CORNER SIZES - Tamanhos de cantos customizados
// ============================================================================

val TopStartCornerSize = CornerSize(16.dp)
val TopEndCornerSize = CornerSize(16.dp)
val BottomStartCornerSize = CornerSize(16.dp)
val BottomEndCornerSize = CornerSize(16.dp)

// Forma com cantos superiores mais arredondados
val AurelleTopHeavyShape = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomStart = 12.dp,
    bottomEnd = 12.dp
)

// Forma com cantos inferiores mais arredondados
val AurelleBottomHeavyShape = RoundedCornerShape(
    topStart = 12.dp,
    topEnd = 12.dp,
    bottomStart = 20.dp,
    bottomEnd = 20.dp
)

// ============================================================================
// SHADOWS - Sombras customizadas
// ============================================================================

import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Color

/**
 * Configurações de sombra para diferentes profundidades.
 * Usado para criar hierarquia visual.
 */

// Sombra leve para elementos elevados
val AurelleShadowSmall = Shadow(
    color = Color.Black.copy(alpha = 0.15f),
    blurRadius = 4f,
    offsetX = 0f,
    offsetY = 2f
)

// Sombra média para cards
val AurelleShadowMedium = Shadow(
    color = Color.Black.copy(alpha = 0.2f),
    blurRadius = 8f,
    offsetX = 0f,
    offsetY = 4f
)

// Sombra forte para elementos flutuantes
val AurelleShadowLarge = Shadow(
    color = Color.Black.copy(alpha = 0.25f),
    blurRadius = 16f,
    offsetX = 0f,
    offsetY = 8f
)

// Sombra colorida para elementos especiais (usando dourado)
fun aurelleGoldShadow(alpha: Float = 0.2f): Shadow = Shadow(
    color = AurelleGold.copy(alpha = alpha),
    blurRadius = 8f,
    offsetX = 0f,
    offsetY = 4f
)

// ============================================================================
// GRADIENTS - Gradientes customizados
// ============================================================================

import androidx.compose.ui.graphics.Brush

/**
 * Gradientes pré-definidos para uso no app.
 */

// Gradiente principal do fundo
fun aurelleBackgroundGradient() = Brush.verticalGradient(
    listOf(AurelleBackgroundTop, AurelleBackgroundBottom)
)

// Gradiente para cards
fun aurelleCardGradient() = Brush.linearGradient(
    listOf(AurelleSurfaceHigh, AurelleSurface)
)

// Gradiente dourado para botões
fun aurelleGoldGradient() = Brush.linearGradient(
    listOf(AurelleGoldDark, AurelleGold, AurelleGoldLight)
)

// Gradiente roxo para destaque
fun aurellePurpleGradient() = Brush.linearGradient(
    listOf(AurellePurpleGlow, AurelleGold)
)

// Gradiente radial para glow
fun aurelleGlowGradient(center: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset(0.5f, 0.5f)) = Brush.radialGradient(
    colors = listOf(AurellePurpleGlow.copy(alpha = 0.4f), Color.Transparent),
    center = center,
    radius = 0.5f
)

// ============================================================================
// BORDER STYLES - Estilos de borda customizados
// ============================================================================

import androidx.compose.ui.unit.Dp

/**
 * Estilos de borda para diferentes estados.
 */

// Borda padrão para cards
fun aurelleCardBorder(width: Dp = 1.dp, alpha: Float = 0.12f) = DpBorder(width, AurelleGold.copy(alpha = alpha))

// Borda para cards selecionados
fun aurelleSelectedBorder(width: Dp = 2.dp, alpha: Float = 1f) = DpBorder(width, AurelleGold.copy(alpha = alpha))

// Borda dourada forte
fun aurelleGoldBorder(width: Dp = 1.5.dp) = DpBorder(width, AurelleGold)

// Classe auxiliar para armazenar borda
@JvmInline
value class DpBorder(val width: Dp, val color: Color)
