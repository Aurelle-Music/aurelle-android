package com.aurelle.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.aurelle.music.ui.theme.AurelleBackgroundBottom
import com.aurelle.music.ui.theme.AurelleBackgroundTop
import com.aurelle.music.ui.theme.AurellePurpleGlow

/** Fundo do app: degradê vertical + brilho roxo no canto superior (como no ícone). */
@Composable
fun AurelleBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AurelleBackgroundTop, AurelleBackgroundBottom)))
            .drawBehind {
                val center = Offset(size.width * 0.9f, 0f)
                val radius = size.width * 0.95f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AurellePurpleGlow.copy(alpha = 0.40f), Color.Transparent),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            },
        content = content,
    )
}
