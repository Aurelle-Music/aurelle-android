package com.aurelle.music.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aurelle.music.R
import com.aurelle.music.download.DownloadQuality
import com.aurelle.music.download.DownloadTarget
import com.aurelle.music.settings.AppIcon
import com.aurelle.music.settings.AppSettings
import com.aurelle.music.settings.AppStyle
import com.aurelle.music.ui.components.AurelleLogo
import com.aurelle.music.ui.components.SectionHeader
import com.aurelle.music.ui.components.staggeredEntrance
import com.aurelle.music.ui.library.Chip
import com.aurelle.music.ui.library.LibrarySort
import com.aurelle.music.ui.library.label
import com.aurelle.music.ui.theme.AurelleGold
import com.aurelle.music.ui.theme.AurelleGoldLight
import com.aurelle.music.ui.theme.AurelleHint
import com.aurelle.music.ui.theme.AurelleOnBackground
import com.aurelle.music.ui.theme.AurelleSurface
import com.aurelle.music.ui.theme.Palettes

/**
 * Aba Conta: configurações (downloads, aparência, Biblioteca) e Sobre. Cada toque grava na hora
 * ([onChange]); o ícone passa por [onIconChange] porque troca um componente do sistema.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onIconChange: (AppIcon) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
    ) {
        item { Column {
            AurelleLogo(
                Modifier
                    .padding(horizontal = 76.dp, vertical = 14.dp)
                    .staggeredEntrance(0)
            )
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = AurelleOnBackground,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
        } }

        // --- Downloads ---------------------------------------------------------------------------
        item { SectionHeader(stringResource(R.string.settings_downloads)) }
        item { Column {
            Group(R.string.settings_download_target) {
                Chip(
                    stringResource(R.string.download_target_device),
                    settings.downloadTarget == DownloadTarget.DEVICE,
                ) { onChange { it.copy(downloadTarget = DownloadTarget.DEVICE) } }
                Chip(
                    stringResource(R.string.download_target_app),
                    settings.downloadTarget == DownloadTarget.APP,
                ) { onChange { it.copy(downloadTarget = DownloadTarget.APP) } }
            }
            Group(R.string.settings_download_quality) {
                DownloadQuality.entries.forEach { quality ->
                    Chip(stringResource(quality.label()), settings.downloadQuality == quality) {
                        onChange { it.copy(downloadQuality = quality) }
                    }
                }
            }
            Note(R.string.settings_download_note)
        } }

        // --- Aparência ---------------------------------------------------------------------------
        item { SectionHeader(stringResource(R.string.settings_appearance), Modifier.padding(top = 12.dp)) }
        item { Column {
            Label(R.string.settings_style)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                AppStyle.entries.forEach { style ->
                    StyleTile(style, selected = settings.style == style) {
                        onChange { it.copy(style = style) }
                    }
                }
            }
            Group(R.string.settings_icon) {
                AppIcon.entries.forEach { icon ->
                    Chip(stringResource(icon.label()), settings.icon == icon) { onIconChange(icon) }
                }
            }
            Note(R.string.settings_icon_note)
        } }

        // --- Biblioteca --------------------------------------------------------------------------
        item { SectionHeader(stringResource(R.string.settings_library), Modifier.padding(top = 12.dp)) }
        item { Column {
            Group(R.string.settings_library_source) {
                Chip(
                    stringResource(R.string.download_target_device),
                    settings.librarySource == DownloadTarget.DEVICE,
                ) { onChange { it.copy(librarySource = DownloadTarget.DEVICE) } }
                Chip(
                    stringResource(R.string.download_target_app),
                    settings.librarySource == DownloadTarget.APP,
                ) { onChange { it.copy(librarySource = DownloadTarget.APP) } }
            }
            Group(R.string.settings_library_sort) {
                LibrarySort.entries.forEach { sort ->
                    Chip(stringResource(sort.label()), settings.librarySort == sort) {
                        onChange { it.copy(librarySort = sort) }
                    }
                }
            }
        } }

        // --- Sobre -------------------------------------------------------------------------------
        item { SectionHeader(stringResource(R.string.settings_about), Modifier.padding(top = 12.dp)) }
        item { Column {
            val context = LocalContext.current
            val version = remember {
                runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
                    .getOrNull().orEmpty()
            }
            Text(
                text = "${stringResource(R.string.app_name)} · ${stringResource(R.string.settings_version, version)}",
                style = MaterialTheme.typography.titleMedium,
                color = AurelleOnBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.settings_license),
                style = MaterialTheme.typography.bodyMedium,
                color = AurelleHint,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_legal),
                style = MaterialTheme.typography.labelMedium,
                color = AurelleHint,
            )
        } }
    }
}

@Composable
private fun Label(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelLarge,
        color = AurelleHint,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun Note(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelSmall,
        color = AurelleHint,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
    )
}

/** Rótulo + chips que quebram de linha quando não cabem. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Group(@StringRes label: Int, chips: @Composable () -> Unit) {
    Label(label)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 16.dp),
    ) { chips() }
}

/** Amostra do estilo: degradê do fundo, uma "superfície" e o dourado, com o nome embaixo. */
@Composable
private fun StyleTile(style: AppStyle, selected: Boolean, onClick: () -> Unit) {
    val palette = Palettes.of(style)
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(palette.backgroundTop, palette.backgroundBottom)))
                .border(if (selected) 2.dp else 1.dp, if (selected) AurelleGold else AurelleGold.copy(alpha = 0.15f), shape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.surface),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(palette.gold))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(style.label()),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) AurelleGoldLight else AurelleHint,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@StringRes
private fun AppStyle.label(): Int = when (this) {
    AppStyle.CLASSIC -> R.string.style_classic
    AppStyle.AMETHYST -> R.string.style_amethyst
    AppStyle.MIDNIGHT -> R.string.style_midnight
    AppStyle.GRAPHITE -> R.string.style_graphite
}

/** Os ícones levam o nome do estilo de mesma cor. */
@StringRes
private fun AppIcon.label(): Int = when (this) {
    AppIcon.CLASSIC -> R.string.style_classic
    AppIcon.AMETHYST -> R.string.style_amethyst
    AppIcon.MIDNIGHT -> R.string.style_midnight
    AppIcon.GRAPHITE -> R.string.style_graphite
}

@StringRes
private fun DownloadQuality.label(): Int = when (this) {
    DownloadQuality.LOW -> R.string.download_quality_low
    DownloadQuality.MID -> R.string.download_quality_mid
    DownloadQuality.HIGH -> R.string.download_quality_high
    DownloadQuality.MAX -> R.string.download_quality_max
}
