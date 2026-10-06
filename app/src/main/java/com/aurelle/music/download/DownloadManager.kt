package com.aurelle.music.download

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Fachada dos downloads: enfileira/cancela no WorkManager e junta (trabalhos em andamento + registro do
 * que já foi baixado) em um único `StateFlow<Map<[downloadKey], DownloadStatus>>` para a UI.
 * Cada música pode ter um download por destino (Dispositivo e App); use [forSong] para ler os dois.
 */
class DownloadManager(
    context: Context,
    val repository: DownloadRepository,
    val preferences: DownloadPreferences,
) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val statuses: StateFlow<Map<String, DownloadStatus>> =
        combine(repository.items, workManager.getWorkInfosByTagFlow(TAG)) { items, infos ->
            buildStatuses(items, infos)
        }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    init {
        scope.launch(Dispatchers.IO) { cleanStalePartials() }
    }

    fun enqueue(job: DownloadJob) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(job.toData())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 20, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(TAG)
            .addTag(SONG_TAG + job.id)
            .addTag(TARGET_TAG + job.target.name)
            .build()
        // KEEP: se já existe um download desta música em andamento, não duplica.
        workManager.enqueueUniqueWork(uniqueName(job.id, job.target), ExistingWorkPolicy.KEEP, request)
    }

    /** Cancela os downloads em andamento da música (nos dois destinos). */
    fun cancel(songId: String) {
        DownloadTarget.entries.forEach { workManager.cancelUniqueWork(uniqueName(songId, it)) }
    }

    /** Apaga o arquivo baixado e tira do registro (a Biblioteca e o botão do player se atualizam sozinhos). */
    fun remove(track: DownloadedTrack) {
        scope.launch(Dispatchers.IO) {
            DownloadFiles.deleteStored(appContext, track)
            repository.remove(track.id, track.target)
        }
    }

    /** Tira da Biblioteca o que o usuário apagou por fora. */
    suspend fun removeMissingFiles() = repository.removeMissingFiles()

    private fun uniqueName(songId: String, target: DownloadTarget) =
        "aurelle-dl-" + DownloadFiles.key(downloadKey(songId, target))

    private fun buildStatuses(
        items: Map<String, DownloadedTrack>,
        infos: List<WorkInfo>,
    ): Map<String, DownloadStatus> {
        val result = HashMap<String, DownloadStatus>()
        items.forEach { (key, track) -> result[key] = DownloadStatus.Done(track) }

        for (info in infos) {
            val songId = info.tags.firstOrNull { it.startsWith(SONG_TAG) }?.removePrefix(SONG_TAG) ?: continue
            val target = info.tags.firstOrNull { it.startsWith(TARGET_TAG) }
                ?.removePrefix(TARGET_TAG)
                ?.let { name -> DownloadTarget.entries.firstOrNull { it.name == name } }
                ?: continue
            val id = downloadKey(songId, target)
            val status: DownloadStatus = when (info.state) {
                WorkInfo.State.RUNNING ->
                    DownloadStatus.Downloading(info.progress.getInt(DownloadJob.KEY_PROGRESS, -1))
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED ->
                    DownloadStatus.Queued(retrying = info.runAttemptCount > 0)
                // Falha ao rebaixar uma música que já estava salva: ela continua baixada.
                WorkInfo.State.FAILED -> if (id in items) continue else {
                    val error = runCatching {
                        DownloadError.valueOf(info.outputData.getString(DownloadJob.KEY_ERROR).orEmpty())
                    }.getOrDefault(DownloadError.GENERIC)
                    DownloadStatus.Failed(error)
                }
                WorkInfo.State.SUCCEEDED, WorkInfo.State.CANCELLED -> continue
            }
            val current = result[id]
            if (current == null || priority(status) > priority(current)) result[id] = status
        }
        return result
    }

    /** Em andamento vence "falhou" e "baixada" (re-download); "falhou" vence "baixada" não ocorre (ver acima). */
    private fun priority(status: DownloadStatus) = when (status) {
        is DownloadStatus.Downloading, is DownloadStatus.Queued -> 2
        is DownloadStatus.Failed -> 1
        is DownloadStatus.Done -> 0
    }

    /** Parciais guardados para retomar; os de mais de 3 dias sem mexer são apagados. */
    private fun cleanStalePartials() {
        val limit = System.currentTimeMillis() - STALE_PARTIAL_MS
        DownloadFiles.partialRoot(appContext).listFiles()?.forEach { dir ->
            val newest = (dir.listFiles()?.maxOfOrNull { it.lastModified() } ?: dir.lastModified())
            if (newest < limit) dir.deleteRecursively()
        }
    }

    private companion object {
        const val TAG = "aurelle-download"
        const val SONG_TAG = "song:"
        const val TARGET_TAG = "target:"
        const val STALE_PARTIAL_MS = 3L * 24 * 60 * 60 * 1000
    }
}
