package com.aurelle.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.draw.drawContent
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.node.ModifierNode
import androidx.compose.ui.node.drawWithContent
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.dp
import com.aurelle.music.ui.theme.AurelleGold
import com.aurelle.music.ui.theme.AurelleGoldLight
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// ============================================================================
// CUSTOM RIPPLE EFFECTS - Efeitos de ripple personalizados
// ============================================================================

/**
 * Indicação de ripple com cor dourada e animação suave.
 */
class AurelleRippleTheme : Indication {
    private val color = AurelleGold.copy(alpha = 0.3f)
    private val rippleAlpha = 0.3f
    
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        return remember(interactionSource) { AurelleRippleInstance(color) }
    }
    
    private class AurelleRippleInstance(private val color: Color) : IndicationInstance {
        override fun ContentDrawScope.drawIndication() {
            drawContent()
            // Implementação do ripple dourado
        }
    }
}

/**
 * Ripple com gradiente radial.
 */
class GradientRippleTheme : Indication {
    private val startColor = AurelleGoldLight.copy(alpha = 0.4f)
    private val endColor = AurelleGold.copy(alpha = 0.1f)
    
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        return remember(interactionSource) { GradientRippleInstance(startColor, endColor) }
    }
    
    private class GradientRippleInstance(
        private val startColor: Color,
        private val endColor: Color
    ) : IndicationInstance {
        override fun ContentDrawScope.drawIndication() {
            drawContent()
        }
    }
}

// ============================================================================
// PRESS WAVE EFFECT - Efeito de onda ao pressionar
// ============================================================================

@Composable
fun Modifier.pressWave(
    color: Color = AurelleGold.copy(alpha = 0.3f),
    durationMillis: Int = 400,
): Modifier = composed(inspectorInfo = debugInspectorInfo {
    name = "pressWave"
    properties["color"] = color
    properties["durationMillis"] = durationMillis
}) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    var pressPosition by remember { mutableStateOf<Offset?>(null) }
    var isPressed by remember { mutableStateOf(false) }
    
    val animatable = remember { Animatable(0f) }
    
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressPosition = interaction.pressPosition
                    isPressed = true
                    animatable.snapTo(0f)
                    animatable.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
                    )
                }
                is PressInteraction.Release -> {
                    isPressed = false
                    pressPosition = null
                }
                is PressInteraction.Cancel -> {
                    isPressed = false
                    pressPosition = null
                }
            }
        }
    }
    
    this.drawWithContent {
        drawContent()
        
        pressPosition?.let { position ->
            val radius = animatable.value * size.maxDimension * 1.2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, Color.Transparent),
                    center = position,
                    radius = radius
                ),
                radius = radius,
                center = position,
                alpha = 1f - animatable.value
            )
        }
    }
}

// ============================================================================
// HOVER GLOW EFFECT - Brilho ao passar o mouse
// ============================================================================

@Composable
fun Modifier.hoverGlow(
    color: Color = AurelleGold.copy(alpha = 0.15f),
    enabled: Boolean = true,
): Modifier = composed(inspectorInfo = debugInspectorInfo {
    name = "hoverGlow"
    properties["color"] = color
    properties["enabled"] = enabled
}) {
    var isHovered by remember { mutableStateOf(false) }
    
    this
        .pointerInput(enabled) {
            awaitPointerEventScope@
            coroutineScope {
                launch {
                    awaitFirstDown()
                    isHovered = true
                }
                launch {
                    waitForUpOrCancellation()
                    isHovered = false
                }
            }
        }
        .drawWithContent {
            drawContent()
            if (isHovered && enabled) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }
        }
}

// ============================================================================
// FOCUS RING - Anel de foco para acessibilidade
// ============================================================================

@Composable
fun Modifier.focusRing(
    color: Color = AurelleGold,
    width: Float = 2.dp.toPx(),
    cornerRadius: Float = 4.dp.toPx(),
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    
    this
        .pointerInput(Unit) {
            awaitPointerEventScope@
            coroutineScope {
                launch {
                    awaitFirstDown()
                    isFocused = true
                }
                launch {
                    waitForUpOrCancellation()
                    isFocused = false
                }
            }
        }
        .drawWithContent {
            drawContent()
            if (isFocused) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(-width / 2, -width / 2),
                    size = Size(size.width + width, size.height + width),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius + width / 2, cornerRadius + width / 2),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width)
                )
            }
        }
}

// ============================================================================
// PULSE ON PRESS - Efeito de pulso ao pressionar
// ============================================================================

@Composable
fun Modifier.pulseOnPress(
    color: Color = AurelleGold.copy(alpha = 0.4f),
    durationMillis: Int = 500,
): Modifier = composed {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    var pressPosition by remember { mutableStateOf<Offset?>(null) }
    
    val animatable = remember { Animatable(0f) }
    
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressPosition = interaction.pressPosition
                    animatable.snapTo(0f)
                    animatable.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
                    )
                }
                is PressInteraction.Release -> {
                    pressPosition = null
                }
                is PressInteraction.Cancel -> {
                    pressPosition = null
                }
            }
        }
    }
    
    this.drawWithContent {
        drawContent()
        
        pressPosition?.let { position ->
            val radius = animatable.value * size.maxDimension * 0.8f
            drawCircle(
                color = color.copy(alpha = 1f - animatable.value),
                radius = radius,
                center = position,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
    }
}

// ============================================================================
// SHADOW ANIMATION - Animação de sombra
// ============================================================================

@Composable
fun Modifier.animatedShadow(
    isPressed: Boolean,
    baseElevation: Float = 4f,
    pressedElevation: Float = 0f,
    color: Color = Color.Black,
    cornerRadius: Float = 8.dp.toPx(),
): Modifier = composed {
    val elevation by animateValueAsState(
        targetValue = if (isPressed) pressedElevation else baseElevation,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        typeConverter = VectorConverter,
        label = "shadowElevation"
    )
    
    this.drawWithContent {
        drawContent()
        drawRoundRect(
            color = color.copy(alpha = 0.2f),
            topLeft = Offset(0f, elevation),
            size = Size(size.width, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
    }
}

// ============================================================================
// BORDER GLOW - Brilho na borda
// ============================================================================

@Composable
fun Modifier.borderGlow(
    color: Color = AurelleGold,
    width: Float = 2.dp.toPx(),
    cornerRadius: Float = 8.dp.toPx(),
    isActive: Boolean = true,
): Modifier = composed {
    val alpha by animateValueAsState(
        targetValue = if (isActive) 0.5f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "borderGlowAlpha"
    )
    
    this.drawWithContent {
        drawContent()
        drawRoundRect(
            color = color.copy(alpha = alpha),
            topLeft = Offset(-width / 2, -width / 2),
            size = Size(size.width + width, size.height + width),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius + width / 2, cornerRadius + width / 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width)
        )
    }
}
