package com.aurelle.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun AurelleTheme(content: @Composable () -> Unit) {
    // O app é escuro por design; o estilo (Conta ▸ Aparência) só troca a paleta. Montado a cada composição
    // porque as cores vêm de [ActivePalette] (estado do Compose).
    val colorScheme = darkColorScheme(
        primary = AurelleGold,
        onPrimary = AurelleBackground,
        background = AurelleBackground,
        onBackground = AurelleOnBackground,
        surface = AurelleBackground,
        onSurface = AurelleOnBackground,
        surfaceVariant = AurelleSurface,
        onSurfaceVariant = AurelleOnBackground,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AurelleTypography,
        content = content,
    )
}
