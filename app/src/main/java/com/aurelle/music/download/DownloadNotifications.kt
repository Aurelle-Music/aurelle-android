package com.aurelle.music.download

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import com.aurelle.music.MainActivity
import com.aurelle.music.R
import java.util.UUID

/** Notificações de download: progresso (em primeiro plano, com "Cancelar") e conclusão. */
object DownloadNotifications {

    const val CHANNEL_ID = "aurelle_downloads"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.download_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun notificationId(job: DownloadJob) = 4000 + (downloadKey(job.id, job.target).hashCode() and 0xFFFF)

    private fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /** [percent] = -1 → barra indeterminada ("Preparando…"). */
    fun foregroundInfo(context: Context, job: DownloadJob, percent: Int, workId: UUID): ForegroundInfo {
        ensureChannel(context)
        val cancel = WorkManager.getInstance(context).createCancelPendingIntent(workId)
        val text = if (percent >= 0) {
            context.getString(R.string.download_progress, percent)
        } else {
            context.getString(R.string.download_preparing)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(job.title)
            .setContentText(text)
            .setProgress(100, percent.coerceAtLeast(0), percent < 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent(context))
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.download_cancel),
                cancel,
            )
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        val id = notificationId(job)
        return if (Build.VERSION.SDK_INT >= 29) {
            ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notification)
        }
    }

    @SuppressLint("MissingPermission") // checamos areNotificationsEnabled() logo abaixo
    fun notifyDone(context: Context, job: DownloadJob) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(job.title)
            .setContentText(context.getString(R.string.download_done_notification))
            .setAutoCancel(true)
            .setSilent(true)
            .setContentIntent(openAppIntent(context))
            .build()
        manager.notify(notificationId(job), notification)
    }
}
