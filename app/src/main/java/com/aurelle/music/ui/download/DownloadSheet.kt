package com.aurelle.music.ui.download

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.aurelle.music.R
import com.aurelle.music.download.DownloadChoice
import com.aurelle.music.download.DownloadQuality
import com.aurelle.music.download.DownloadRequest
import com.aurelle.music.download.DownloadStatus
import com.aurelle.music.download.DownloadTarget
import com.aurelle.music.download.SongDownloads
import com.aurelle.music.download.forSong
import com.aurelle.music.download.isActive
import com.aurelle.music.ui.theme.AurelleBackgroundBottom
import com.aurelle.music.ui.theme.AurelleBackgroundTop
import com.aurelle.music.ui.theme.AurelleGold
import com.aurelle.music.ui.theme.AurelleGoldLight
import com.aurelle.music.ui.theme.AurelleHint
import com.aurelle.music.ui.theme.AurelleOnBackground
import com.aurelle.music.ui.theme.AurelleSurface
import com.aurelle.music.ui.theme.AurelleSurfaceHigh

/**
 * Folha de escolha (destino + qualidade) para [request]. Fica em um lugar só (AurelleApp) e serve o player e
 * os cards. Cada destino é independente: dá para ter a mesma música no Dispositivo e no App, em qualidades
 * diferentes. Escolher um destino que já tem a música troca aquela cópia ("Baixar de novo").
 */
@Composable
fun DownloadSheetHost(
    request: DownloadRequest?,
    statuses: Map<String, DownloadStatus>,
    defaults: DownloadChoice,
    onDownload: (DownloadRequest, DownloadChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<Pair<DownloadRequest, DownloadChoice>?>(null) }

    // Só Android 8/9: salvar em Música/Aurelle exige permissão de armazenamento.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val job = pending
        pending = null
        if (job != null) {
            if (granted) {
                onDownload(job.first, job.second)
            } else {
                Toast.makeText(context, R.string.download_error_storage_permission, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun start(song: DownloadRequest, choice: DownloadChoice) {
        val needsLegacyPermission = choice.target == DownloadTarget.DEVICE &&
            Build.VERSION.SDK_INT < 29 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        if (needsLegacyPermission) {
            pending = song to choice
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            onDownload(song, choice)
        }
    }

    if (request != null) {
        DownloadSheet(
            current = statuses.forSong(request.id),
            initial = defaults,
            onConfirm = { choice ->
                onDismiss()
                start(request, choice)
            },
            onDismiss = onDismiss,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadSheet(
    current: SongDownloads,
    initial: DownloadChoice,
    onConfirm: (DownloadChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    // Já tem no destino preferido e ainda não no outro? Começa no outro (o caso de querer as duas cópias).
    val other = if (initial.target == DownloadTarget.DEVICE) DownloadTarget.APP else DownloadTarget.DEVICE
    var target by remember {
        mutableStateOf(
            if (current.of(initial.target) is DownloadStatus.Done && current.of(other) == null) other
            else initial.target
        )
    }
    var quality by remember { mutableStateOf(initial.quality) }
    val selected = current.of(target)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AurelleBackgroundTop,
        contentColor = AurelleOnBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AurelleHint.copy(alpha = 0.5f)) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.download_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                color = AurelleOnBackground,
            )
            val error = (selected as? DownloadStatus.Failed)?.error
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(error.messageRes()),
                    style = MaterialTheme.typography.labelLarge,
                    color = AurelleGoldLight,
                )
            }

            Spacer(Modifier.height(20.dp))
            SheetLabel(R.string.download_save_to)
            OptionRow(
                selected = target == DownloadTarget.DEVICE,
                title = stringResource(R.string.download_target_device),
                subtitle = stringResource(R.string.download_target_device_hint),
                note = statusNote(current.device),
                onClick = { target = DownloadTarget.DEVICE },
            )
            Spacer(Modifier.height(8.dp))
            OptionRow(
                selected = target == DownloadTarget.APP,
                title = stringResource(R.string.download_target_app),
                subtitle = stringResource(R.string.download_target_app_hint),
                note = statusNote(current.app),
                onClick = { target = DownloadTarget.APP },
            )

            Spacer(Modifier.height(20.dp))
            SheetLabel(R.string.download_quality)
            val qualities = listOf(
                DownloadQuality.LOW to R.string.download_quality_low,
                DownloadQuality.MID to R.string.download_quality_mid,
                DownloadQuality.HIGH to R.string.download_quality_high,
                DownloadQuality.MAX to R.string.download_quality_max,
            )
            qualities.forEachIndexed { index, (option, label) ->
                if (index > 0) Spacer(Modifier.height(8.dp))
                OptionRow(
                    selected = quality == option,
                    title = stringResource(label),
                    subtitle = null,
                    note = null,
                    onClick = { quality = option },
                )
            }

            val replacing = selected is DownloadStatus.Done
            if (replacing) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.download_replace_note),
                    style = MaterialTheme.typography.labelMedium,
                    color = AurelleHint,
                )
            }

            Spacer(Modifier.height(20.dp))
            val busy = selected?.isActive == true
            Button(
                onClick = { onConfirm(DownloadChoice(target, quality)) },
                enabled = !busy,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AurelleGold,
                    contentColor = AurelleBackgroundBottom,
                    disabledContainerColor = AurelleSurfaceHigh,
                    disabledContentColor = AurelleHint,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = stringResource(
                        when {
                            busy -> R.string.download_status_active
                            replacing -> R.string.download_confirm_again
                            else -> R.string.download_confirm
                        }
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

/** "Já baixada · M4A · 128 kbps" / "Baixando…" / nada. */
@Composable
private fun statusNote(status: DownloadStatus?): String? = when (status) {
    is DownloadStatus.Done -> {
        val track = status.track
        val details = buildList {
            add(track.format.uppercase())
            if (track.bitrateKbps > 0) add("${track.bitrateKbps} kbps")
        }.joinToString(" · ")
        stringResource(R.string.download_status_done, details)
    }
    is DownloadStatus.Queued, is DownloadStatus.Downloading -> stringResource(R.string.download_status_active)
    else -> null
}

@Composable
private fun SheetLabel(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelLarge,
        color = AurelleGoldLight,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun OptionRow(
    selected: Boolean,
    title: String,
    subtitle: String?,
    note: String?,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AurelleSurface)
            .border(1.dp, if (selected) AurelleGold else AurelleGold.copy(alpha = 0.12f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = AurelleGold,
                unselectedColor = AurelleHint,
            ),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = AurelleOnBackground)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.labelMedium, color = AurelleHint)
            }
            if (note != null) {
                Text(text = note, style = MaterialTheme.typography.labelMedium, color = AurelleGoldLight)
            }
        }
    }
}
