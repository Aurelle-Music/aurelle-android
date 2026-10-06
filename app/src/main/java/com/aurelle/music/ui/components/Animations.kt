package com.aurelle.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// ============================================================================
// SPRING SPECS - Molas customizadas para diferentes efeitos
// ============================================================================

val SpringBouncy = Spring<Float>(dampingRatio = 0.5f, stiffness = 200f)
val SpringStiff = Spring<Float>(dampingRatio = 0.8f, stiffness = 500f)
val SpringSmooth = Spring<Float>(dampingRatio = 0.9f, stiffness = 300f)
val SpringSoft = Spring<Float>(dampingRatio = 0.7f, stiffness = 200f)

// Spring specs para Animatable
val SpringSpecBouncy = FloatSpringSpec(dampingRatio = 0.5f, stiffness = 200f)
val SpringSpecStiff = FloatSpringSpec(dampingRatio = 0.8f, stiffness = 500f)
val SpringSpecSmooth = FloatSpringSpec(dampingRatio = 0.9f, stiffness = 300f)

// ============================================================================
// TWEEN SPECS - Animações lineares customizadas
// ============================================================================

fun tweenFast(durationMillis: Int = 200) = tween<Float>(durationMillis, easing = FastOutLinearInEasing)
fun tweenSmooth(durationMillis: Int = 300) = tween<Float>(durationMillis, easing = FastOutSlowInEasing)
fun tweenBounce(durationMillis: Int = 400) = tween<Float>(durationMillis, easing = FastOutSlowInEasing)

// ============================================================================
// PULSE ANIMATION - Efeito de pulso contínuo
// ============================================================================

@Composable
fun Modifier.pulse(
    isActive: Boolean = true,
    durationMillis: Int = 1000,
    minScale: Float = 0.95f,
    maxScale: Float = 1.05f,
): Modifier = composed(inspectorInfo = debugInspectorInfo {
    name = "pulse"
    properties["isActive"] = isActive
    properties["durationMillis"] = durationMillis
}) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = if (isActive) minScale else 1f,
        targetValue = if (isActive) maxScale else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis / 2, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

// ============================================================================
// GLLOW ANIMATION - Efeito de brilho pulsante
// ============================================================================

@Composable
fun Modifier.glowPulse(
    isActive: Boolean = true,
    color: Color = Color.White,
    durationMillis: Int = 2000,
    minAlpha: Float = 0.2f,
    maxAlpha: Float = 0.6f,
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "glowPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = if (isActive) minAlpha else 0f,
        targetValue = if (isActive) maxAlpha else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    this.graphicsLayer {
        // Aplicar shadow com alpha animado
        shadowElevation = if (isActive) 8.dp.toPx() else 0f
        // Para sombra colorida, precisaria de implementação customizada
    }
}

// ============================================================================
// MORPH ANIMATION - Transição suave entre formas
// ============================================================================

@Composable
fun <T> animateValueAsStateMorph(
    targetValue: T,
    animationSpec: AnimationSpec<T> = tweenSmooth(),
    converter: TwoWayConverter<T, AnimationVector1D> = VectorConverter,
): T {
    val animatedValue by animateValueAsState(
        targetValue = targetValue,
        animationSpec = animationSpec,
        typeConverter = converter,
        label = "morphAnimation"
    )
    return animatedValue
}

// ============================================================================
// BOUNCE ANIMATION - Efeito de quique
// ============================================================================

@Composable
fun Modifier.bounce(
    trigger: Boolean,
    durationMillis: Int = 500,
    bounceCount: Int = 2,
    onComplete: () -> Unit = {},
): Modifier = composed {
    val animatable = remember { Animatable(1f) }
    
    LaunchedEffect(trigger) {
        if (trigger) {
            animatable.snapTo(1f)
            for (i in 1..bounceCount) {
                animatable.animateTo(
                    targetValue = 0.8f,
                    animationSpec = tween(durationMillis / (bounceCount * 2), easing = FastOutSlowInEasing)
                )
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis / (bounceCount * 2), easing = FastOutSlowInEasing)
                )
            }
            onComplete()
        }
    }
    
    this.graphicsLayer {
        scaleX = animatable.value
        scaleY = animatable.value
    }
}

// ============================================================================
// SHAKE ANIMATION - Efeito de tremida
// ============================================================================

@Composable
fun Modifier.shake(
    trigger: Boolean,
    durationMillis: Int = 400,
    shakes: Int = 5,
    shakeAmount: Float = 5f,
    onComplete: () -> Unit = {},
): Modifier = composed {
    val animatable = remember { Animatable(0f) }
    
    LaunchedEffect(trigger) {
        if (trigger) {
            val segmentDuration = durationMillis / (shakes * 2)
            for (i in 1..shakes) {
                animatable.animateTo(
                    targetValue = shakeAmount,
                    animationSpec = tween(segmentDuration)
                )
                animatable.animateTo(
                    targetValue = -shakeAmount,
                    animationSpec = tween(segmentDuration)
                )
            }
            animatable.snapTo(0f)
            onComplete()
        }
    }
    
    this.graphicsLayer {
        translationX = animatable.value
    }
}

// ============================================================================
// FLIP ANIMATION - Efeito de virar
// ============================================================================

@Composable
fun Modifier.flip(
    isFlipped: Boolean,
    durationMillis: Int = 300,
): Modifier = composed {
    val rotation by animateValueAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "flipRotation"
    )
    
    this.graphicsLayer {
        rotationY = rotation
        cameraDistance = 8.dp.toPx()
    }
}

// ============================================================================
// SLIDE + FADE ANIMATIONS - Combinações prontas
// ============================================================================

@Composable
fun Modifier.slideInFromTop(
    isVisible: Boolean,
    durationMillis: Int = 300,
    offset: Float = 100f,
): Modifier = composed {
    val offsetAnim by animateValueAsState(
        targetValue = if (isVisible) 0f else -offset,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "slideInFromTop"
    )
    
    this.graphicsLayer {
        translationY = offsetAnim
    }
}

@Composable
fun Modifier.slideInFromBottom(
    isVisible: Boolean,
    durationMillis: Int = 300,
    offset: Float = 100f,
): Modifier = composed {
    val offsetAnim by animateValueAsState(
        targetValue = if (isVisible) 0f else offset,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "slideInFromBottom"
    )
    
    this.graphicsLayer {
        translationY = offsetAnim
    }
}

@Composable
fun Modifier.slideInFromLeft(
    isVisible: Boolean,
    durationMillis: Int = 300,
    offset: Float = 100f,
): Modifier = composed {
    val offsetAnim by animateValueAsState(
        targetValue = if (isVisible) 0f else -offset,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "slideInFromLeft"
    )
    
    this.graphicsLayer {
        translationX = offsetAnim
    }
}

@Composable
fun Modifier.slideInFromRight(
    isVisible: Boolean,
    durationMillis: Int = 300,
    offset: Float = 100f,
): Modifier = composed {
    val offsetAnim by animateValueAsState(
        targetValue = if (isVisible) 0f else offset,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "slideInFromRight"
    )
    
    this.graphicsLayer {
        translationX = offsetAnim
    }
}

// ============================================================================
// HOVER & PRESS EFFECTS - Micro-interações
// ============================================================================

@Composable
fun Modifier.hoverScale(
    enabled: Boolean = true,
    scaleOnHover: Float = 1.05f,
    durationMillis: Int = 150,
): Modifier = composed {
    var isHovered by remember { mutableStateOf(false) }
    
    val scale by animateValueAsState(
        targetValue = if (isHovered && enabled) scaleOnHover else 1f,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "hoverScale"
    )
    
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
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
}

@Composable
fun Modifier.pressScale(
    enabled: Boolean = true,
    scaleOnPress: Float = 0.95f,
    durationMillis: Int = 100,
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateValueAsState(
        targetValue = if (isPressed && enabled) scaleOnPress else 1f,
        animationSpec = spring(SpringSpecStiff),
        label = "pressScale"
    )
    
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            awaitPointerEventScope@
            coroutineScope {
                launch {
                    awaitFirstDown()
                    isPressed = true
                }
                launch {
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
}

// ============================================================================
// PARALLAX EFFECT - Efeito de profundidade
// ============================================================================

@Composable
fun Modifier.parallax(
    scrollOffset: Float,
    multiplier: Float = 0.5f,
): Modifier = composed {
    this.graphicsLayer {
        translationY = scrollOffset * multiplier
    }
}

// ============================================================================
// COLOR ANIMATION - Transição suave entre cores
// ============================================================================

@Composable
fun animateColorBetween(
    targetColor: Color,
    durationMillis: Int = 300,
): Color {
    val animatedColor by animateValueAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        typeConverter = Color.VectorConverter,
        label = "colorAnimation"
    )
    return animatedColor
}

// ============================================================================
// FLOATING ANIMATION - Efeito de flutuação suave
// ============================================================================

@Composable
fun Modifier.floating(
    isActive: Boolean = true,
    durationMillis: Int = 3000,
    floatAmount: Float = 4f,
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val offset by infiniteTransition.animateFloat(
        initialValue = if (isActive) 0f else 0f,
        targetValue = if (isActive) floatAmount else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatingOffset"
    )
    
    this.graphicsLayer {
        translationY = offset
    }
}

// ============================================================================
// ROTATION ANIMATION - Rotação contínua ou controlada
// ============================================================================

@Composable
fun Modifier.rotateContinuous(
    isActive: Boolean = true,
    durationMillis: Int = 2000,
    degrees: Float = 360f,
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "rotateContinuous")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isActive) degrees else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    this.graphicsLayer {
        rotationZ = rotation
    }
}

@Composable
fun Modifier.rotateTo(
    targetDegrees: Float,
    durationMillis: Int = 300,
): Modifier = composed {
    val rotation by animateValueAsState(
        targetValue = targetDegrees,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "rotateTo"
    )
    
    this.graphicsLayer {
        rotationZ = rotation
    }
}

// ============================================================================
// CUSTOM ENTER/EXIT ANIMATIONS
// ============================================================================

@Composable
fun Modifier.customEnter(
    isVisible: Boolean,
    durationMillis: Int = 300,
): Modifier = composed {
    val alpha by animateValueAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "enterAlpha"
    )
    val scale by animateValueAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "enterScale"
    )
    
    this.graphicsLayer {
        alpha = alpha
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun Modifier.customExit(
    isVisible: Boolean,
    durationMillis: Int = 200,
): Modifier = composed {
    val alpha by animateValueAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "exitAlpha"
    )
    val scale by animateValueAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "exitScale"
    )
    
    this.graphicsLayer {
        alpha = alpha
        scaleX = scale
        scaleY = scale
    }
}
