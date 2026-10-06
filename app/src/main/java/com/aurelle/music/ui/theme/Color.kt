package com.aurelle.music.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.aurelle.music.settings.AppStyle

/** Cores que mudam com o estilo do app (Etapa 6). O resto da paleta é igual em todos os estilos. */
@Immutable
class AurellePalette(
    val background: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val navBar: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val gold: Color,
    val goldLight: Color,
    val goldDark: Color,
    val hint: Color,
    val purpleGlow: Color,
    val onBackground: Color = Color(0xFFF2EEF7),
    val onCard: Color = Color(0xFF1B1B1B),
)

object Palettes {
    // Clássico: a paleta original da marca (extraída do branding / mockups).
    private val Classic = AurellePalette(
        background = Color(0xFF262032), backgroundTop = Color(0xFF30263F), backgroundBottom = Color(0xFF181320),
        navBar = Color(0xFF1B1626), surface = Color(0xFF362D47), surfaceHigh = Color(0xFF453A59),
        gold = Color(0xFFD4A857), goldLight = Color(0xFFEBCB85), goldDark = Color(0xFFA77C34),
        hint = Color(0xFF9C93AB), purpleGlow = Color(0xFF4A2A55),
    )
    // Ametista: roxo mais vivo, com dourado rosado.
    private val Amethyst = AurellePalette(
        background = Color(0xFF2A1B4D), backgroundTop = Color(0xFF36245F), backgroundBottom = Color(0xFF170E2E),
        navBar = Color(0xFF1C1236), surface = Color(0xFF3B2A66), surfaceHigh = Color(0xFF4C397F),
        gold = Color(0xFFE0A17C), goldLight = Color(0xFFF2C6A8), goldDark = Color(0xFFB0724E),
        hint = Color(0xFFA596BF), purpleGlow = Color(0xFF5B2E86),
    )
    // Meia-noite: azul-noite com dourado champanhe.
    private val Midnight = AurellePalette(
        background = Color(0xFF18233A), backgroundTop = Color(0xFF1F2D4A), backgroundBottom = Color(0xFF0D1423),
        navBar = Color(0xFF111A2E), surface = Color(0xFF24344F), surfaceHigh = Color(0xFF2F4466),
        gold = Color(0xFFE3C07A), goldLight = Color(0xFFF3DDA8), goldDark = Color(0xFFB08F4A),
        hint = Color(0xFF8E9DB5), purpleGlow = Color(0xFF20406B),
    )
    // Grafite: cinzas neutros com o dourado da marca.
    private val Graphite = AurellePalette(
        background = Color(0xFF202124), backgroundTop = Color(0xFF2A2B2F), backgroundBottom = Color(0xFF121315),
        navBar = Color(0xFF17181A), surface = Color(0xFF2E3035), surfaceHigh = Color(0xFF3B3E44),
        gold = Color(0xFFD4A857), goldLight = Color(0xFFEBCB85), goldDark = Color(0xFFA77C34),
        hint = Color(0xFF9A9CA3), purpleGlow = Color(0xFF3A3A40),
    )

    fun of(style: AppStyle): AurellePalette = when (style) {
        AppStyle.CLASSIC -> Classic
        AppStyle.AMETHYST -> Amethyst
        AppStyle.MIDNIGHT -> Midnight
        AppStyle.GRAPHITE -> Graphite
    }
}

/**
 * Paleta em uso. É um estado do Compose: as propriedades `Aurelle*` abaixo leem dele, então trocar o estilo
 * recompõe quem usa as cores, sem mexer nas telas nem recriar a navegação.
 */
object ActivePalette {
    var current: AurellePalette by mutableStateOf(Palettes.of(AppStyle.CLASSIC))
}

val AurelleBackground: Color get() = ActivePalette.current.background          // roxo base da marca
val AurelleBackgroundTop: Color get() = ActivePalette.current.backgroundTop    // topo do degradê de fundo
val AurelleBackgroundBottom: Color get() = ActivePalette.current.backgroundBottom // base do degradê de fundo
val AurelleNavBar: Color get() = ActivePalette.current.navBar                  // dock de navegação
val AurelleSurface: Color get() = ActivePalette.current.surface                // superfícies elevadas (busca, cards)
val AurelleSurfaceHigh: Color get() = ActivePalette.current.surfaceHigh        // superfícies mais claras
val AurelleGold: Color get() = ActivePalette.current.gold                      // dourado da marca
val AurelleGoldLight: Color get() = ActivePalette.current.goldLight            // dourado claro (destaques)
val AurelleGoldDark: Color get() = ActivePalette.current.goldDark              // dourado escuro (gradientes)
val AurelleHint: Color get() = ActivePalette.current.hint                      // placeholders
val AurellePurpleGlow: Color get() = ActivePalette.current.purpleGlow          // brilho do fundo

// Iguais em todos os estilos.
val AurelleCard = Color(0xFFD9D9D9)             // cinza claro da paleta original
val AurelleOnBackground: Color get() = ActivePalette.current.onBackground
val AurelleOnCard: Color get() = ActivePalette.current.onCard

// ============================================================================
// ANIMATED COLOR TRANSITIONS - Transições suaves entre estilos
// ============================================================================

/**
 * Anima a transição entre paletas quando o estilo muda.
 * Usa uma animação suave para todas as cores.
 */
object AnimatedPalette {
    private val animationSpec = tween<Color>(durationMillis = 400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    
    @Composable
    fun background(): Color {
        return animateValueAsState(
            targetValue = ActivePalette.current.background,
            animationSpec = animationSpec,
            typeConverter = Color.VectorConverter,
            label = "animatedBackground"
        ).value
    }
    
    @Composable
    fun backgroundTop(): Color {
        return animateValueAsState(
            targetValue = ActivePalette.current.backgroundTop,
            animationSpec = animationSpec,
            typeConverter = Color.VectorConverter,
            label = "animatedBackgroundTop"
        ).value
    }
    
    @Composable
    fun backgroundBottom(): Color {
        return animateValueAsState(
            targetValue = ActivePalette.current.backgroundBottom,
            animationSpec = animationSpec,
            typeConverter = Color.VectorConverter,
            label = "animatedBackgroundBottom"
        ).value
    }
    
    @Composable
    fun navBar(): Color {
        return animateValueAsState(
            targetValue = ActivePalette.current.navBar,
            animationSpec = animationSpec,
            typeConverter = Color.VectorConverter,
            label = "animatedNavBar"
        ).value
    }
    
    @Composable
    fun surface(): Color {
        return animateValueAsState(
            targetValue = ActivePalette.current.surface,
            animationSpec = animationSpec,
            typeConverter = Color.VectorConverter,
            label = "animatedSurface"
        ).value
    }
    
    @Composable
    fun surfaceHigh(): Color {
        return animateValueAsState(
            targetValue = ActivePalette.current.surfaceHigh,
            animationSpec = animationSpec,
            typeConverter = Color.VectorConverter,
            label = "animatedSurfaceHigh"
        ).value
    }
    
    @Composable
    fun gold(): Color {
        return animateValueAsState(
            targetValue = ActivePalette.current.gold,
            animationSpec = animationSpec,
            typeConverter = Color.VectorConverter,
            label = "animatedGold"
        ).value
    }
    
    @Composable
    fun goldLight(): Color {
        return animateValueAsState(
            targetValue = ActivePalette.current.goldLight,
            animationSpec = animationSpec,
            typeConverter = Color.VectorConverter,
            label = "animatedGoldLight"
        ).value
    }
    
    @Composable
    fun goldDark(): Color {
        return animateValueAsState(
            targetValue = ActivePalette.current.goldDark,
            animationSpec = animationSpec,
            typeConverter = Color.VectorConverter,
            label = "animatedGoldDark"
        ).value
    }
    
    @Composable
    fun hint(): Color {
        return animateValueAsState(
            targetValue = ActivePalette.current.hint,
            animationSpec = animationSpec,
            typeConverter = Color.VectorConverter,
            label = "animatedHint"
        ).value
    }
    
    @Composable
    fun purpleGlow(): Color {
        return animateValueAsState(
            targetValue = ActivePalette.current.purpleGlow,
            animationSpec = animationSpec,
            typeConverter = Color.VectorConverter,
            label = "animatedPurpleGlow"
        ).value
    }
}
