package com.aurelle.music.ui.download

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aurelle.music.R
import com.aurelle.music.download.DownloadError
import com.aurelle.music.download.DownloadStatus
import com.aurelle.music.download.SongDownloads
import com.aurelle.music.ui.theme.AurelleGold
import com.aurelle.music.ui.theme.AurelleGoldLight
import com.aurelle.music.ui.theme.AurelleSurface
import com.aurelle.music.ui.theme.AurelleSurfaceHigh

@StringRes
fun DownloadError.messageRes(): Int = when (this) {
    DownloadError.NO_NETWORK -> R.string.download_error_no_network
    DownloadError.NO_SPACE -> R.string.download_error_no_space
    DownloadError.AGE_RESTRICTED -> R.string.download_error_age_restricted
    DownloadError.UNAVAILABLE -> R.string.download_error_unavailable
    DownloadError.STORAGE_PERMISSION -> R.string.download_error_storage_permission
    DownloadError.GENERIC -> R.string.download_error_generic
}

/**
 * Botão redondo de baixar (player e cards). Mostra o estado da música nos dois destinos:
 * baixar → progresso com "cancelar" → baixada / erro. Tocar abre a folha de escolha ([onClick]);
 * tocar durante um download cancela ([onCancel]).
 */
@Composable
fun DownloadButton(
    downloads: SongDownloads,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    container: Color = AurelleSurface,
) {
    val status = downloads.summary
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .border(1.dp, AurelleGold.copy(alpha = 0.25f), CircleShape)
            .clickable { if (downloads.isActive) onCancel() else onClick() },
        contentAlignment = Alignment.Center,
    ) {
        val iconSize = size * 0.54f
        when (status) {
            null -> StatusIcon(Icons.Filled.Download, R.string.download_button, AurelleGoldLight, iconSize)
            is DownloadStatus.Failed ->
                StatusIcon(Icons.Filled.ErrorOutline, R.string.download_failed_description, AurelleGoldLight, iconSize)
            is DownloadStatus.Done ->
                StatusIcon(Icons.Filled.DownloadDone, R.string.download_done_description, AurelleGold, iconSize)
            is DownloadStatus.Queued, is DownloadStatus.Downloading -> {
                val percent = (status as? DownloadStatus.Downloading)?.percent ?: -1
                if (percent >= 0) {
                    CircularProgressIndicator(
                        progress = { percent / 100f },
                        color = AurelleGold,
                        trackColor = AurelleSurfaceHigh,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(size * 0.66f),
                    )
                } else {
                    CircularProgressIndicator(
                        color = AurelleGold,
                        trackColor = AurelleSurfaceHigh,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(size * 0.66f),
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.download_cancel_description),
                    tint = AurelleGoldLight,
                    modifier = Modifier.size(size * 0.3f),
                )
            }
        }
    }
}

@Composable
private fun StatusIcon(
    icon: ImageVector,
    @StringRes description: Int,
    tint: Color,
    size: Dp,
) {
    Icon(
        imageVector = icon,
        contentDescription = stringResource(description),
        tint = tint,
        modifier = Modifier.size(size),
    )
}
