package com.example.downloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.WarXApp
import com.example.data.DownloadEntity
import com.example.data.DownloadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DownloadForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_ID = "warx_downloader_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_CANCEL = "ACTION_CANCEL"
        const val EXTRA_DOWNLOAD_ID = "EXTRA_DOWNLOAD_ID"

        fun startService(context: Context, downloadId: Long) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        val notification = createNotification("WarX Downloader", "Siap mengunduh video...", 0, false)
        startForeground(NOTIFICATION_ID, notification)

        // Observe active downloads to update notification
        serviceScope.launch {
            val repo = WarXApp.instance.downloadRepository
            repo.activeDownloads.collectLatest { activeList ->
                val downloading = activeList.firstOrNull { it.state == DownloadState.DOWNLOADING }
                if (downloading != null) {
                    val progress = downloading.progressPercent.toInt().coerceIn(0, 100)
                    val speedStr = formatSpeed(downloading.speedBytesPerSec)
                    val etaStr = if (downloading.etaSeconds > 0) formatEta(downloading.etaSeconds) else ""
                    val content = "$speedStr • $progress% ${if (etaStr.isNotEmpty()) "• $etaStr" else ""}"
                    val notif = createNotification(downloading.title, content, progress, true, downloading.id)
                    notificationManager.notify(NOTIFICATION_ID, notif)
                } else if (activeList.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val downloadId = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1L) ?: -1L

        when (action) {
            ACTION_START -> {
                if (downloadId > 0) {
                    DownloadManager.getInstance(applicationContext).startDownload(downloadId)
                }
            }
            ACTION_PAUSE -> {
                if (downloadId > 0) {
                    DownloadManager.getInstance(applicationContext).pauseDownload(downloadId)
                }
            }
            ACTION_RESUME -> {
                if (downloadId > 0) {
                    DownloadManager.getInstance(applicationContext).startDownload(downloadId)
                }
            }
            ACTION_CANCEL -> {
                if (downloadId > 0) {
                    DownloadManager.getInstance(applicationContext).cancelDownload(downloadId)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Unduhan Aktif WarX",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menampilkan progress pengunduhan video"
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(
        title: String,
        content: String,
        progress: Int,
        isDownloading: Boolean,
        downloadId: Long = -1L
    ): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(isDownloading)
            .setOnlyAlertOnce(true)

        if (isDownloading) {
            builder.setProgress(100, progress, false)

            if (downloadId > 0) {
                // Pause Action
                val pauseIntent = Intent(this, DownloadForegroundService::class.java).apply {
                    action = ACTION_PAUSE
                    putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                }
                val pausePendingIntent = PendingIntent.getService(
                    this, 1, pauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(android.R.drawable.ic_media_pause, "Jeda", pausePendingIntent)

                // Cancel Action
                val cancelIntent = Intent(this, DownloadForegroundService::class.java).apply {
                    action = ACTION_CANCEL
                    putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                }
                val cancelPendingIntent = PendingIntent.getService(
                    this, 2, cancelIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Batal", cancelPendingIntent)
            }
        }

        return builder.build()
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
            bytesPerSec >= 1024 -> String.format("%.0f KB/s", bytesPerSec / 1024.0)
            else -> "$bytesPerSec B/s"
        }
    }

    private fun formatEta(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }
}
