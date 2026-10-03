package com.example.downloader

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import com.example.WarXApp
import com.example.data.DownloadEntity
import com.example.data.DownloadRepository
import com.example.data.DownloadState
import com.example.hls.HlsParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class DownloadManager private constructor(private val context: Context) {

    private val repository: DownloadRepository = WarXApp.instance.downloadRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<Long, Job>()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        @Volatile
        private var INSTANCE: DownloadManager? = null

        fun getInstance(context: Context): DownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun sanitizeFileName(title: String, extension: String): String {
            val clean = title.replace(Regex("""[\\/:*?"<>|]"""), "_")
                .trim()
                .take(75)
            val ext = if (extension.startsWith(".")) extension else ".$extension"
            return if (clean.endsWith(ext, ignoreCase = true)) clean else "$clean$ext"
        }
    }

    /**
     * Starts download with protection against duplicate jobs.
     */
    fun startDownload(downloadId: Long) {
        val existingJob = activeJobs[downloadId]
        if (existingJob != null && existingJob.isActive) {
            // Already downloading, avoid duplicate execution
            return
        }

        val job = scope.launch {
            val item = repository.getDownload(downloadId) ?: return@launch
            repository.updateDownload(item.copy(state = DownloadState.DOWNLOADING, errorMessage = null))

            // Start foreground service safely
            DownloadForegroundService.startService(context)

            try {
                if (item.isHls) {
                    executeHlsDownload(item)
                } else {
                    executeDirectDownload(item)
                }
            } catch (e: CancellationException) {
                // Cancelled or paused normally
            } catch (e: Exception) {
                val current = repository.getDownload(downloadId)
                if (current != null && current.state == DownloadState.DOWNLOADING) {
                    repository.updateDownload(
                        current.copy(
                            state = DownloadState.FAILED,
                            speedBytesPerSec = 0L,
                            errorMessage = e.localizedMessage ?: "Terjadi kesalahan koneksi atau berkas rusak"
                        )
                    )
                }
            } finally {
                activeJobs.remove(downloadId)
            }
        }
        activeJobs[downloadId] = job
    }

    fun pauseDownload(downloadId: Long) {
        val job = activeJobs.remove(downloadId)
        job?.cancel()
        scope.launch {
            val item = repository.getDownload(downloadId)
            if (item != null && item.state == DownloadState.DOWNLOADING) {
                repository.updateDownload(item.copy(state = DownloadState.PAUSED, speedBytesPerSec = 0L))
            }
        }
    }

    fun resumeDownload(downloadId: Long) {
        startDownload(downloadId)
    }

    fun cancelDownload(downloadId: Long) {
        val job = activeJobs.remove(downloadId)
        job?.cancel()
        scope.launch {
            val item = repository.getDownload(downloadId)
            if (item != null) {
                try {
                    val partFile = File("${item.filePath}.part")
                    if (partFile.exists()) partFile.delete()
                    val metaFile = File("${item.filePath}.meta")
                    if (metaFile.exists()) metaFile.delete()
                } catch (_: Exception) {}

                repository.updateDownload(
                    item.copy(
                        state = DownloadState.CANCELLED,
                        downloadedBytes = 0L,
                        speedBytesPerSec = 0L
                    )
                )
            }
        }
    }

    fun retryDownload(downloadId: Long) {
        startDownload(downloadId)
    }

    private suspend fun executeDirectDownload(download: DownloadEntity) = withContext(Dispatchers.IO) {
        val targetFile = File(download.filePath)
        val partFile = File("${download.filePath}.part")
        targetFile.parentFile?.mkdirs()

        var existingBytes = if (partFile.exists()) partFile.length() else 0L

        // Check if server supports resume
        val headReq = Request.Builder()
            .url(download.mediaUrl)
            .head()
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
            .build()

        var totalBytes = download.totalBytes
        var canResume = false

        try {
            val headRes = client.newCall(headReq).execute()
            if (headRes.isSuccessful) {
                val acceptRanges = headRes.header("Accept-Ranges")
                canResume = acceptRanges != null && acceptRanges.contains("bytes", ignoreCase = true)
                val len = headRes.header("Content-Length")?.toLongOrNull() ?: 0L
                if (len > 0) totalBytes = len
            }
            headRes.close()
        } catch (_: Exception) {}

        if (!canResume && existingBytes > 0) {
            partFile.delete()
            existingBytes = 0L
        }

        val getReqBuilder = Request.Builder()
            .url(download.mediaUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")

        if (existingBytes > 0) {
            getReqBuilder.header("Range", "bytes=$existingBytes-")
        }

        val response = client.newCall(getReqBuilder.build()).execute()
        if (!response.isSuccessful && response.code != 206) {
            response.close()
            throw IllegalStateException("Server merespons kode HTTP ${response.code}")
        }

        val body = response.body ?: run {
            response.close()
            throw IllegalStateException("Respons body kosong dari server")
        }
        val stream: InputStream = body.byteStream()

        val isAppend = existingBytes > 0 && response.code == 206
        if (!isAppend && existingBytes > 0) {
            existingBytes = 0L
            partFile.delete()
        }

        val output = FileOutputStream(partFile, isAppend)
        val buffer = ByteArray(64 * 1024)
        var bytesRead: Int
        var downloaded = existingBytes

        var lastUpdateTime = System.currentTimeMillis()
        var lastBytes = downloaded
        var currentSpeed = 0L

        try {
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                if (!coroutineContext.isActive) {
                    output.flush()
                    output.close()
                    stream.close()
                    response.close()
                    return@withContext
                }

                output.write(buffer, 0, bytesRead)
                downloaded += bytesRead

                val now = System.currentTimeMillis()
                val deltaT = now - lastUpdateTime
                if (deltaT >= 800) {
                    val deltaB = downloaded - lastBytes
                    currentSpeed = if (deltaT > 0) ((deltaB * 1000L) / deltaT).coerceAtLeast(0L) else 0L
                    val eta = if (currentSpeed > 0 && totalBytes > downloaded) {
                        (totalBytes - downloaded) / currentSpeed
                    } else 0L

                    repository.updateDownload(
                        download.copy(
                            state = DownloadState.DOWNLOADING,
                            downloadedBytes = downloaded,
                            totalBytes = totalBytes,
                            speedBytesPerSec = currentSpeed,
                            etaSeconds = eta
                        )
                    )
                    lastUpdateTime = now
                    lastBytes = downloaded
                }
            }

            output.flush()
            output.close()
            stream.close()
            response.close()

            // Complete download: rename .part to final
            if (partFile.exists()) {
                if (targetFile.exists()) targetFile.delete()
                partFile.renameTo(targetFile)
            }

            // Scan media to appear in Gallery
            scanMediaFile(targetFile.absolutePath)

            repository.updateDownload(
                download.copy(
                    state = DownloadState.COMPLETED,
                    downloadedBytes = downloaded,
                    totalBytes = downloaded,
                    speedBytesPerSec = 0L,
                    etaSeconds = 0L,
                    completedAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            try {
                output.flush()
                output.close()
                stream.close()
                response.close()
            } catch (_: Exception) {}
            throw e
        }
    }

    private suspend fun executeHlsDownload(download: DownloadEntity) = withContext(Dispatchers.IO) {
        val targetFile = File(download.filePath)
        val partFile = File("${download.filePath}.part")
        val metaFile = File("${download.filePath}.meta")
        targetFile.parentFile?.mkdirs()

        // 1. Fetch playlist content
        val req = Request.Builder()
            .url(download.mediaUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
            .build()

        val res = client.newCall(req).execute()
        if (!res.isSuccessful) {
            res.close()
            throw IllegalStateException("Gagal memuat manifest HLS (HTTP ${res.code})")
        }
        val content = res.body?.string().orEmpty()
        val finalUrl = res.request.url.toString()
        res.close()

        val parsed = HlsParser.parse(content, finalUrl)
        val segments = if (parsed.isMaster) {
            // Select matching variant or first variant
            val selectedVariant = parsed.variants.firstOrNull { it.quality == download.quality }
                ?: parsed.variants.firstOrNull()
                ?: throw IllegalStateException("Tidak ada variant stream yang ditemukan pada master playlist")

            val subReq = Request.Builder()
                .url(selectedVariant.url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                .build()
            val subRes = client.newCall(subReq).execute()
            val subContent = subRes.body?.string().orEmpty()
            val subFinalUrl = subRes.request.url.toString()
            subRes.close()

            val subParsed = HlsParser.parse(subContent, subFinalUrl)
            subParsed.segments
        } else {
            parsed.segments
        }

        if (segments.isEmpty()) {
            throw IllegalStateException("Playlist HLS tidak memiliki segmen video yang dapat diunduh.")
        }

        val totalSegments = segments.size
        var startSegment = 0

        // Check resume status from meta file
        if (metaFile.exists() && partFile.exists()) {
            try {
                val savedMeta = metaFile.readText().trim().toIntOrNull()
                if (savedMeta != null && savedMeta in 1 until totalSegments) {
                    startSegment = savedMeta
                }
            } catch (_: Exception) {}
        }

        var downloadedBytes = if (partFile.exists() && startSegment > 0) partFile.length() else 0L
        if (startSegment == 0 && partFile.exists()) {
            partFile.delete()
            downloadedBytes = 0L
        }

        val output = FileOutputStream(partFile, startSegment > 0)
        val keyCache = mutableMapOf<String, ByteArray>()

        var lastUpdateTime = System.currentTimeMillis()
        var lastBytes = downloadedBytes
        var currentSpeed = 0L

        repository.updateDownload(
            download.copy(
                totalSegments = totalSegments,
                downloadedSegments = startSegment,
                state = DownloadState.DOWNLOADING
            )
        )

        for (i in startSegment until totalSegments) {
            if (!coroutineContext.isActive) {
                output.flush()
                output.close()
                metaFile.writeText(i.toString())
                return@withContext
            }

            val segment = segments[i]
            val segReq = Request.Builder()
                .url(segment.url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                .build()

            var segRes = client.newCall(segReq).execute()
            if (!segRes.isSuccessful) {
                delay(500)
                segRes.close()
                segRes = client.newCall(segReq).execute()
                if (!segRes.isSuccessful) {
                    segRes.close()
                    output.flush()
                    output.close()
                    metaFile.writeText(i.toString())
                    throw IllegalStateException("Gagal mengunduh segmen ${i + 1}/$totalSegments (HTTP ${segRes.code})")
                }
            }

            var segBytes = segRes.body?.bytes() ?: ByteArray(0)
            segRes.close()

            // Handle AES-128 decryption if necessary
            if (segment.keyMethod.equals("AES-128", ignoreCase = true) && segment.keyUrl != null) {
                var keyBytes = keyCache[segment.keyUrl]
                if (keyBytes == null) {
                    val keyReq = Request.Builder().url(segment.keyUrl).build()
                    val keyRes = client.newCall(keyReq).execute()
                    if (keyRes.isSuccessful) {
                        keyBytes = keyRes.body?.bytes()
                        if (keyBytes != null) keyCache[segment.keyUrl] = keyBytes
                    }
                    keyRes.close()
                }

                if (keyBytes != null) {
                    val iv = segment.keyIv ?: run {
                        val generatedIv = ByteArray(16)
                        var seq = i.toLong()
                        for (idx in 15 downTo 8) {
                            generatedIv[idx] = (seq and 0xFF).toByte()
                            seq = seq shr 8
                        }
                        generatedIv
                    }
                    try {
                        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
                        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(iv))
                        segBytes = cipher.doFinal(segBytes)
                    } catch (_: Exception) {}
                }
            }

            output.write(segBytes)
            downloadedBytes += segBytes.size

            val currentDownloadedSegments = i + 1
            metaFile.writeText(currentDownloadedSegments.toString())

            val now = System.currentTimeMillis()
            val deltaT = now - lastUpdateTime
            if (deltaT >= 800 || currentDownloadedSegments == totalSegments) {
                val deltaB = downloadedBytes - lastBytes
                currentSpeed = if (deltaT > 0) ((deltaB * 1000L) / deltaT).coerceAtLeast(0L) else 0L
                val remainingSegs = totalSegments - currentDownloadedSegments
                val avgBytesPerSeg = (downloadedBytes / currentDownloadedSegments).coerceAtLeast(1L)
                val eta = if (currentSpeed > 0) (remainingSegs * avgBytesPerSeg) / currentSpeed else 0L

                repository.updateDownload(
                    download.copy(
                        state = DownloadState.DOWNLOADING,
                        downloadedBytes = downloadedBytes,
                        downloadedSegments = currentDownloadedSegments,
                        totalSegments = totalSegments,
                        speedBytesPerSec = currentSpeed,
                        etaSeconds = eta
                    )
                )
                lastUpdateTime = now
                lastBytes = downloadedBytes
            }
        }

        output.flush()
        output.close()
        metaFile.delete()

        // Complete: rename .part to final target
        if (partFile.exists()) {
            if (targetFile.exists()) targetFile.delete()
            partFile.renameTo(targetFile)
        }

        scanMediaFile(targetFile.absolutePath)

        repository.updateDownload(
            download.copy(
                state = DownloadState.COMPLETED,
                downloadedBytes = downloadedBytes,
                downloadedSegments = totalSegments,
                totalSegments = totalSegments,
                speedBytesPerSec = 0L,
                etaSeconds = 0L,
                completedAt = System.currentTimeMillis()
            )
        )
    }

    private fun scanMediaFile(filePath: String) {
        try {
            val f = File(filePath)
            if (f.exists()) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(f.absolutePath),
                    arrayOf("video/*", "video/mp4", "video/mp2t")
                ) { _, _ -> }
            }
        } catch (_: Exception) {}
    }
}
