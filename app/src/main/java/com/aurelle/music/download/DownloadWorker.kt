package com.aurelle.music.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import android.os.StatFs
import com.aurelle.music.AurelleApplication
import com.aurelle.music.player.StreamHttpException
import com.aurelle.music.player.StreamResolver
import com.aurelle.music.player.StreamUnavailableException
import com.aurelle.music.player.UnavailableReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * Baixa UMA música em segundo plano (WorkManager, em primeiro plano com notificação de progresso).
 *
 * - Erros de rede no meio do caminho → `Result.retry()` (com espera crescente) e retoma do arquivo parcial.
 * - Erros definitivos (restrição de idade, indisponível, sem espaço, sem permissão) → `Result.failure` com o motivo.
 */
class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val job: DownloadJob? = DownloadJob.from(inputData)

    override suspend fun getForegroundInfo(): ForegroundInfo =
        DownloadNotifications.foregroundInfo(
            applicationContext,
            job ?: DownloadJob("", "", "", null, null, DownloadTarget.APP, DownloadQuality.MAX),
            -1,
            id,
        )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val job = this@DownloadWorker.job ?: return@withContext failure(DownloadError.GENERIC)
        val repository = AurelleApplication.downloadRepository
        // Cópia que já existe NESTE destino: só chegamos aqui se o usuário pediu "baixar de novo"; ela é trocada ao final.
        val previous = repository.get(job.id, job.target)

        val partialDir = DownloadFiles.partialDir(applicationContext, job.id, job.target)
        try {
            publish(job, -1)
            val maxKbps = job.quality.maxKbps
            val choice = StreamResolver.resolveForDownload(job.id, maxKbps)

            partialDir.mkdirs()
            val partial = File(partialDir, "${choice.suffix}_${choice.bitrateKbps}.part")
            partialDir.listFiles()?.filter { it != partial }?.forEach { it.delete() } // sobras de outro stream
            ensureSpace(job, choice.contentLength, partial)

            coroutineScope {
                val progress = MutableStateFlow(-1)
                val reporter = launch {
                    progress.collect { percent ->
                        publish(job, percent)
                        delay(PROGRESS_THROTTLE_MS)
                    }
                }
                try {
                    AudioDownloader(httpClient).download(
                        initial = choice,
                        partial = partial,
                        refresh = { StreamResolver.resolveForDownload(job.id, maxKbps) },
                    ) { done, total ->
                        progress.value = if (total != null && total > 0) (done * 100 / total).toInt() else -1
                    }
                } finally {
                    reporter.cancel()
                }
            }

            val track = store(job, choice, partial)
            repository.put(track)
            if (previous != null) DownloadFiles.deleteStored(applicationContext, previous, keepUri = track.uri)
            partialDir.deleteRecursively()
            DownloadNotifications.notifyDone(applicationContext, job)
            Result.success()
        } catch (e: CancellationException) {
            throw e // cancelado pelo usuário (ou parado pelo sistema): o parcial fica para retomar
        } catch (e: DownloadException) {
            cleanAndFail(partialDir, e.error)
        } catch (e: StreamUnavailableException) {
            cleanAndFail(
                partialDir,
                when (e.reason) {
                    UnavailableReason.AGE_RESTRICTED -> DownloadError.AGE_RESTRICTED
                    UnavailableReason.UNAVAILABLE -> DownloadError.UNAVAILABLE
                    UnavailableReason.GENERIC -> DownloadError.GENERIC
                },
            )
        } catch (e: StreamHttpException) {
            cleanAndFail(partialDir, DownloadError.UNAVAILABLE)
        } catch (e: SecurityException) {
            cleanAndFail(partialDir, DownloadError.STORAGE_PERMISSION)
        } catch (e: IOException) {
            when {
                isNoSpace(e) -> cleanAndFail(partialDir, DownloadError.NO_SPACE)
                runAttemptCount < MAX_ATTEMPTS -> Result.retry()
                // Esgotou as tentativas: o parcial fica, então tentar de novo depois retoma.
                else -> failure(if (isNetwork(e)) DownloadError.NO_NETWORK else DownloadError.GENERIC)
            }
        } catch (e: Exception) {
            cleanAndFail(partialDir, DownloadError.GENERIC)
        }
    }

    // --- Espaço, gravação ----------------------------------------------------------------------

    private fun ensureSpace(job: DownloadJob, total: Long?, partial: File) {
        if (total == null) return
        val have = if (partial.exists()) partial.length() else 0L
        var needed = (total - have).coerceAtLeast(0L) + SPACE_MARGIN_BYTES
        if (job.target == DownloadTarget.DEVICE) needed += total // o parcial é copiado para a pasta Música
        val free = StatFs(applicationContext.filesDir.path).availableBytes
        if (free < needed) throw DownloadException(DownloadError.NO_SPACE)
    }

    private fun store(job: DownloadJob, choice: StreamResolver.AudioChoice, partial: File): DownloadedTrack {
        val size = partial.length()
        val uri = when (job.target) {
            DownloadTarget.APP -> {
                val dest = DownloadFiles.appFile(applicationContext, job.id, choice.suffix)
                dest.parentFile?.mkdirs()
                if (!partial.renameTo(dest)) {
                    partial.copyTo(dest, overwrite = true)
                    partial.delete()
                }
                dest.absolutePath
            }
            DownloadTarget.DEVICE -> DownloadFiles.saveToDevice(
                context = applicationContext,
                source = partial,
                displayName = DownloadFiles.displayName(job.artist, job.title, choice.suffix),
                mimeType = choice.mimeType,
                title = job.title,
                artist = job.artist,
            )
        }
        return DownloadedTrack(
            id = job.id,
            title = job.title,
            artist = job.artist,
            imageUrl = job.imageUrl,
            durationSeconds = job.durationSeconds,
            target = job.target,
            uri = uri,
            format = choice.suffix,
            bitrateKbps = choice.bitrateKbps,
            sizeBytes = size,
            downloadedAt = System.currentTimeMillis(),
            quality = job.quality,
            album = job.album,
            trackNumber = job.trackNumber,
        )
    }

    // --- Progresso, resultado ------------------------------------------------------------------

    private suspend fun publish(job: DownloadJob, percent: Int) {
        setProgress(workDataOf(DownloadJob.KEY_PROGRESS to percent))
        try {
            setForeground(DownloadNotifications.foregroundInfo(applicationContext, job, percent, id))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Android 12+ pode negar iniciar serviço em primeiro plano em segundo plano: baixamos mesmo assim.
        }
    }

    private fun failure(error: DownloadError): Result =
        Result.failure(workDataOf(DownloadJob.KEY_ERROR to error.name))

    private fun cleanAndFail(partialDir: File, error: DownloadError): Result {
        partialDir.deleteRecursively()
        return failure(error)
    }

    private fun isNoSpace(e: IOException): Boolean =
        generateSequence<Throwable>(e) { it.cause }.any { it.message?.contains("ENOSPC") == true }

    private fun isNetwork(e: IOException): Boolean =
        generateSequence<Throwable>(e) { it.cause }
            .any { it is UnknownHostException || it is SocketTimeoutException || it is ConnectException }

    private companion object {
        const val MAX_ATTEMPTS = 5
        const val PROGRESS_THROTTLE_MS = 400L
        const val SPACE_MARGIN_BYTES = 2L * 1024 * 1024

        val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }
}
