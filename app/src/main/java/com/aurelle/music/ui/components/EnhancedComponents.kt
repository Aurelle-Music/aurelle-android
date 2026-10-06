package com.aurelle.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aurelle.music.R
import com.aurelle.music.ui.theme.AurelleGold
import com.aurelle.music.ui.theme.AurelleGoldDark
import com.aurelle.music.ui.theme.AurelleGoldLight
import com.aurelle.music.ui.theme.AurelleHint
import com.aurelle.music.ui.theme.AurelleOnBackground
import com.aurelle.music.ui.theme.AurelleSurface
import com.aurelle.music.ui.theme.AurelleSurfaceHigh

// ============================================================================
// ENHANCED MEDIA CARD - Card com animações avançadas
// ============================================================================

@Composable
fun EnhancedMediaCard(
    imageUrl: String?,
    title: String,
    subtitle: String?,
    isPlaying: Boolean = false,
    isFavorite: Boolean = false,
    onClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    isArtist: Boolean = false,
    showControls: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "enhancedCardScale"
    )
    
    val shadowElevation by animateFloatAsState(
        targetValue = if (pressed) 0f else 8f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "enhancedCardShadow"
    )
    
    val shape = if (isArtist) CircleShape else RoundedCornerShape(16.dp)
    
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = shadowElevation.dp.toPx()
                shape = shape
                clip = true
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        horizontalAlignment = if (isArtist) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        // Card Cover with gradient border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(
                    Brush.linearGradient(listOf(AurelleSurfaceHigh, AurelleSurface))
                )
                .border(
                    width = 1.5.dp,
                    color = if (isPlaying) AurelleGold else AurelleGold.copy(alpha = 0.15f),
                    shape = shape
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Glow effect when playing
            if (isPlaying) {
                val infiniteTransition = rememberInfiniteTransition(label = "playingGlow")
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.6f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glowAlpha"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    AurelleGold.copy(alpha = glowAlpha),
                                    Color.Transparent
                                ),
                                center = Alignment.Center,
                                radius = 0.5f
                            )
                        )
                )
            }
            
            // Image or Icon
            if (imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = if (isArtist) Icons.Filled.Person else Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = AurelleGold.copy(alpha = 0.40f),
                    modifier = Modifier.fillMaxSize(0.4f),
                )
            }
            
            // Play overlay
            if (showControls && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Pause,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = AurelleOnBackground,
            textAlign = if (isArtist) TextAlign.Center else TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        
        // Subtitle
        if (subtitle != null && subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = AurelleHint,
                textAlign = if (isArtist) TextAlign.Center else TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        
        // Controls row
        if (showControls) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isArtist) Arrangement.Center else Arrangement.Start,
            ) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) AurelleGold else AurelleHint,
                        modifier = Modifier.size(18.dp),
                    )
                }
                if (!isArtist) {
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onMoreClick,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null,
                            tint = AurelleHint,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// GRADIENT BUTTON - Botão com gradiente animado
// ============================================================================

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: List<Color> = listOf(AurelleGoldDark, AurelleGold, AurelleGoldLight),
    cornerRadius: Dp = 24.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.95f else 1f,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "gradientButtonScale"
    )
    
    val shadowElevation by animateFloatAsState(
        targetValue = if (pressed && enabled) 0f else 4f,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "gradientButtonShadow"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "gradientShift")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientOffset"
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = shadowElevation.dp.toPx()
            }
            .background(
                Brush.linearGradient(
                    colors = colors,
                    startX = offset,
                    endX = offset + 1f,
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
    }
}

// ============================================================================
// PULSING ICON BUTTON - Botão com ícone pulsante
// ============================================================================

@Composable
fun PulsingIconButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pulseColor: Color = AurelleGold,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    isActive: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.9f else 1f,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "pulsingButtonScale"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Background with pulse effect
        Box(
            modifier = Modifier
                .size(size * 0.8f)
                .clip(CircleShape)
                .background(pulseColor.copy(alpha = 0.15f))
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
        )
        
        // Icon
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = pulseColor,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        )
    }
}

// ============================================================================
// LOADING INDICATOR - Indicador de carregamento customizado
// ============================================================================

@Composable
fun AurelleLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color = AurelleGold,
    strokeWidth: Dp = 4.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val dots = listOf(0f, 90f, 180f, 270f)
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        dots.forEachIndexed { index, angle ->
            val offsetAngle = angle + rotation
            val dotScale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1000,
                        delayMillis = index * 250,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dotScale$index"
            )
            
            Box(
                modifier = Modifier
                    .size(strokeWidth)
                    .clip(CircleShape)
                    .background(color)
                    .graphicsLayer {
                        rotationZ = offsetAngle
                        translationX = (size.toPx() / 2 - strokeWidth.toPx() / 2) * 0.6f
                        scaleX = dotScale
                        scaleY = dotScale
                    }
            )
        }
    }
}

// ============================================================================
// TOAST MESSAGE - Mensagem toast customizada
// ============================================================================

@Composable
fun AurelleToast(
    message: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    durationMillis: Int = 3000,
) {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "toastAlpha"
    )
    
    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 100f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "toastOffset"
    )
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            kotlinx.coroutines.delay(durationMillis.toLong())
            onDismiss()
        }
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300)) + androidx.compose.animation.slideInVertically(tween(300)) { -it },
        exit = fadeOut(tween(200)) + androidx.compose.animation.slideOutVertically(tween(200)) { -it },
    ) {
        Box(
            modifier = modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AurelleSurfaceHigh)
                .border(1.dp, AurelleGold.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = AurelleGold,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AurelleOnBackground,
                )
            }
        }
    }
}
