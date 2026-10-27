package com.tgplayer.android.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.PowerManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.tgplayer.android.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.TimeUnit

/**
 * Foreground WorkManager worker for one Telegram file. Mirrors NextPlayer's
 * DownloadWorker: keeps running when the UI process is gone, shows progress in
 * the notification, and hands the copied path back through outputData.
 */
class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val fileId = inputData.getInt(DownloadContract.KEY_FILE_ID, 0)
        val title = inputData.getString(DownloadContract.KEY_TITLE) ?: "视频"
        if (fileId == 0) return Result.failure()

        // Must be foreground before the (possibly long) TDLib cold start,
        // otherwise the OS may kill this process while it is still connecting.
        runCatching { setForeground(foregroundInfo(title, 0L, 0L)) }

        if (!Td.awaitReady(120_000L)) return Result.retry()

        return Downloads.workerPermits.withPermit {
            download(fileId, title)
        }
    }

    private suspend fun download(fileId: Int, title: String): Result {
        val power = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val lock = power?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TGPlayer:download")?.apply {
            setReferenceCounted(false)
            acquire(TimeUnit.HOURS.toMillis(6))
        }
        return try {
            downloadInternal(fileId, title)
        } finally {
            if (lock?.isHeld == true) lock.release()
        }
    }

    private suspend fun downloadInternal(fileId: Int, title: String): Result {
        Td.download(fileId, priority = 32)

        val deadline = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(6)
        var lastNotification = 0L
        var lastGot = 0L
        var lastTime = System.currentTimeMillis()
        var lastGrowthAt = System.currentTimeMillis()
        var missingSince = 0L
        while (true) {
            delay(1_000)
            val now = System.currentTimeMillis()
            val file = Td.file(fileId)
            if (file == null) {
                if (missingSince == 0L) missingSince = now
                if (now - missingSince > 30_000L) return Result.retry()
                continue
            }
            missingSince = 0L
            val total = file.totalSize
            val got = if (file.completed) total else file.downloadedPrefix
            val speed = if (now > lastTime) ((got - lastGot) * 1000L) / (now - lastTime) else 0L
            if (got > lastGot) lastGrowthAt = now
            lastGot = got
            lastTime = now
            setProgress(
                workDataOf(
                    DownloadContract.KEY_DOWNLOADED to got,
                    DownloadContract.KEY_TOTAL to total,
                    DownloadContract.KEY_SPEED to speed,
                ),
            )

            if (now - lastNotification > 3_000) {
                lastNotification = now
                runCatching { setForeground(foregroundInfo(title, got, total)) }
            }

            // TDLib can pause a transfer (network switch, playback competing for
            // the same file, or a re-created client). Re-arm it instead of
            // sitting at 0 B/s until the 6h deadline.
            if (!file.completed && now - lastGrowthAt > 20_000L) {
                Td.download(fileId, priority = 32)
                lastGrowthAt = now
            }
            if (file.completed || (total > 0 && got >= total)) {
                val path = Downloads.copyToDownloads(applicationContext, fileId, title, file.localPath)
                if (path == null) {
                    return if (runAttemptCount < 3) Result.retry() else Result.failure()
                }
                return Result.success(
                    workDataOf(
                        DownloadContract.KEY_PATH to path,
                        DownloadContract.KEY_DOWNLOADED to total,
                        DownloadContract.KEY_TOTAL to total,
                    ),
                )
            }
            if (now > deadline) return Result.failure()
        }
    }

    private fun foregroundInfo(title: String, downloaded: Long, total: Long): ForegroundInfo {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "下载", NotificationManager.IMPORTANCE_LOW),
        )
        val progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle("正在下载")
            .setContentText(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, total <= 0)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private companion object {
        const val CHANNEL_ID = "tm_downloads"
        const val NOTIFICATION_ID = 0x7D1
    }
}
