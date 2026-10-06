package com.aurelle.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aurelle.music.R
import com.aurelle.music.settings.AppStyle
import com.aurelle.music.ui.theme.AurelleGold
import com.aurelle.music.ui.theme.AurelleGoldLight
import com.aurelle.music.ui.theme.AurelleHint
import com.aurelle.music.ui.theme.AurelleOnBackground
import com.aurelle.music.ui.theme.AurelleSurface
import com.aurelle.music.ui.theme.AurelleSurfaceHigh
import com.aurelle.music.ui.theme.Palettes
import com.aurelle.music.ui.components.pulse
import com.aurelle.music.ui.components.floating

// ============================================================================
// STYLE PREVIEW CARD - Card de visualização de estilo com animações
// ============================================================================

@Composable
fun StylePreviewCard(
    style: AppStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Palettes.of(style)
    val shape = RoundedCornerShape(16.dp)
    var isHovered by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered || isSelected) 1.02f else 1f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "stylePreviewScale"
    )
    
    val shadowElevation by animateFloatAsState(
        targetValue = if (isHovered || isSelected) 8f else 4f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "stylePreviewShadow"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) AurelleGold else palette.gold.copy(alpha = 0.15f),
        animationSpec = tween(200),
        label = "stylePreviewBorder"
    )
    
    Column(
        modifier = modifier
            .width(120.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = shadowElevation.dp.toPx()
            }
            .background(
                Brush.verticalGradient(listOf(palette.backgroundTop, palette.backgroundBottom))
            )
            .border(if (isSelected) 2.5.dp else 1.5.dp, borderColor, shape)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header with style name
        Text(
            text = stringResource(style.label()),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) AurelleGoldLight else palette.hint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        
        Spacer(Modifier.height(8.dp))
        
        // Preview content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(palette.surfaceHigh, palette.surface)))
                .border(1.dp, palette.gold.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            // Artist circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(palette.surface)
                    .border(1.dp, palette.gold.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = palette.gold,
                    modifier = Modifier.size(20.dp),
                )
            }
            
            // Glow effect
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(palette.purpleGlow.copy(alpha = 0.4f), Color.Transparent),
                            center = Alignment.Center,
                            radius = 0.5f
                        )
                    )
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Color palette preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.gold)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.goldLight)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.surface)
            )
        }
        
        // Selection indicator
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
        ) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = AurelleGold,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ============================================================================
// STYLE PREVIEW WITH ANIMATION - Versão animada
// ============================================================================

@Composable
fun AnimatedStylePreviewCard(
    style: AppStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Palettes.of(style)
    val shape = RoundedCornerShape(16.dp)
    
    val infiniteTransition = rememberInfiniteTransition(label = "stylePreviewAnimation")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "styleGlow"
    )
    
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isSelected) 4f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "styleFloating"
    )
    
    Box(
        modifier = modifier
            .width(120.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .graphicsLayer {
                translationY = floatingOffset
            }
            .shadow(if (isSelected) 12.dp else 4.dp, shape)
            .background(
                Brush.verticalGradient(listOf(palette.backgroundTop, palette.backgroundBottom))
            )
            .border(if (isSelected) 2.5.dp else 1.5.dp, if (isSelected) AurelleGold else palette.gold.copy(alpha = 0.15f), shape)
            .padding(12.dp),
    ) {
        // Glow background when selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(palette.purpleGlow.copy(alpha = glowAlpha), Color.Transparent),
                            center = Alignment.Center,
                            radius = 0.8f
                        )
                    )
            )
        }
        
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(style.label()),
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) AurelleGoldLight else palette.hint,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            
            Spacer(Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(palette.surfaceHigh, palette.surface)))
                    .border(1.dp, palette.gold.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                // Pulsing icon
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (isSelected) 1.1f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "iconPulse"
                )
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(palette.surface)
                        .border(1.dp, palette.gold.copy(alpha = 0.2f), CircleShape)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = palette.gold,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.gold)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.goldLight)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.surface)
                )
            }
        }
        
        // Selection badge
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(AurelleGold)
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

// ============================================================================
// STYLE SHOWCASE - Exibição completa do estilo
// ============================================================================

@Composable
fun StyleShowcase(
    style: AppStyle,
    modifier: Modifier = Modifier,
) {
    val palette = Palettes.of(style)
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(listOf(palette.backgroundTop, palette.backgroundBottom))
            )
            .border(1.5.dp, palette.gold.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        // Header
        Text(
            text = stringResource(style.label()),
            style = MaterialTheme.typography.titleMedium,
            color = palette.gold,
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Sample card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(palette.surfaceHigh, palette.surface)))
                    .border(1.dp, palette.gold.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = palette.gold,
                    modifier = Modifier.size(32.dp),
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Song Title",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AurelleOnBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Artist Name",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.hint,
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Color palette
        Text(
            text = "Color Palette",
            style = MaterialTheme.typography.labelLarge,
            color = palette.hint,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ColorSwatch(color = palette.background, label = "Background")
            ColorSwatch(color = palette.surface, label = "Surface")
            ColorSwatch(color = palette.gold, label = "Gold")
            ColorSwatch(color = palette.purpleGlow, label = "Glow")
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AurelleHint,
        )
    }
}

// ============================================================================
// EXTENSION FUNCTIONS FOR STYLE PREVIEW
// ============================================================================

@Composable
fun AppStyle.label(): Int = when (this) {
    AppStyle.CLASSIC -> R.string.style_classic
    AppStyle.AMETHYST -> R.string.style_amethyst
    AppStyle.MIDNIGHT -> R.string.style_midnight
    AppStyle.GRAPHITE -> R.string.style_graphite
}

@Composable
fun AppStyle.description(): Int = when (this) {
    AppStyle.CLASSIC -> R.string.style_classic_desc
    AppStyle.AMETHYST -> R.string.style_amethyst_desc
    AppStyle.MIDNIGHT -> R.string.style_midnight_desc
    AppStyle.GRAPHITE -> R.string.style_graphite_desc
}

// ============================================================================
// STYLE COMPARISON - Comparação lado a lado
// ============================================================================

@Composable
fun StyleComparison(
    style1: AppStyle,
    style2: AppStyle,
    modifier: Modifier = Modifier,
) {
    val palette1 = Palettes.of(style1)
    val palette2 = Palettes.of(style2)
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AurelleSurface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StyleComparisonCard(style1, palette1, modifier = Modifier.weight(1f))
        StyleComparisonCard(style2, palette2, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StyleComparisonCard(
    style: AppStyle,
    palette: com.aurelle.music.ui.theme.AurellePalette,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(listOf(palette.backgroundTop, palette.backgroundBottom))
            )
            .padding(12.dp),
    ) {
        Text(
            text = stringResource(style.label()),
            style = MaterialTheme.typography.labelMedium,
            color = palette.gold,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(palette.surface)
                .border(1.dp, palette.gold.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = palette.gold,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
