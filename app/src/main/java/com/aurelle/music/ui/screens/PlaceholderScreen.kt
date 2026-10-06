package com.aurelle.music.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aurelle.music.R
import com.aurelle.music.ui.components.AurelleLogo
import com.aurelle.music.ui.components.staggeredEntrance
import com.aurelle.music.ui.theme.AurelleGold
import com.aurelle.music.ui.theme.AurelleGoldLight
import com.aurelle.music.ui.theme.AurelleHint
import com.aurelle.music.ui.theme.AurelleOnBackground
import com.aurelle.music.ui.theme.AurellePurpleGlow
import com.aurelle.music.ui.theme.AurelleSurface

/** Tela provisória das abas Biblioteca e Conta (implementadas em etapas futuras). */
@Composable
fun PlaceholderScreen(
    icon: ImageVector,
    @StringRes title: Int,
    @StringRes message: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        AurelleLogo(
            Modifier
                .padding(horizontal = 48.dp, vertical = 20.dp)
                .staggeredEntrance(0)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .staggeredEntrance(1),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(170.dp)
                            .background(
                                Brush.radialGradient(listOf(AurellePurpleGlow, Color.Transparent)),
                                CircleShape,
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(AurelleSurface)
                            .border(1.5.dp, AurelleGold.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = AurelleGoldLight,
                            modifier = Modifier.size(42.dp),
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = AurelleOnBackground,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AurelleHint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp),
                )
            }
        }
    }
}
