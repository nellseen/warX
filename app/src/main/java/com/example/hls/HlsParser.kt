package com.example.hls

import com.example.model.AudioTrack
import com.example.model.MediaSource
import com.example.model.SubtitleTrack
import java.net.URI

data class HlsSegment(
    val index: Int,
    val durationSeconds: Double,
    val url: String,
    val keyMethod: String? = null,
    val keyUrl: String? = null,
    val keyIv: ByteArray? = null
)

data class HlsPlaylistResult(
    val isMaster: Boolean,
    val variants: List<MediaSource> = emptyList(),
    val segments: List<HlsSegment> = emptyList(),
    val targetDuration: Double = 0.0,
    val totalDurationSeconds: Double = 0.0,
    val isLive: Boolean = false,
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitles: List<SubtitleTrack> = emptyList()
)

object HlsParser {

    fun parse(content: String, baseUrl: String): HlsPlaylistResult {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty() || !lines.first().startsWith("#EXTM3U")) {
            return HlsPlaylistResult(isMaster = false)
        }

        val isMaster = lines.any { it.startsWith("#EXT-X-STREAM-INF") }

        return if (isMaster) {
            parseMasterPlaylist(lines, baseUrl)
        } else {
            parseMediaPlaylist(lines, baseUrl)
        }
    }

    private fun parseMasterPlaylist(lines: List<String>, baseUrl: String): HlsPlaylistResult {
        val variants = mutableListOf<MediaSource>()
        val audioTracks = mutableListOf<AudioTrack>()
        val subtitles = mutableListOf<SubtitleTrack>()

        var currentStreamInf: Map<String, String>? = null

        for (line in lines) {
            when {
                line.startsWith("#EXT-X-STREAM-INF:") -> {
                    val attrsString = line.removePrefix("#EXT-X-STREAM-INF:")
                    currentStreamInf = parseAttributes(attrsString)
                }
                line.startsWith("#EXT-X-MEDIA:") -> {
                    val attrs = parseAttributes(line.removePrefix("#EXT-X-MEDIA:"))
                    val type = attrs["TYPE"]?.uppercase()
                    val name = attrs["NAME"] ?: "Track"
                    val uriAttr = attrs["URI"]
                    val lang = attrs["LANGUAGE"] ?: attrs["LANG"]
                    if (!uriAttr.isNullOrEmpty()) {
                        val resolvedUri = resolveUrl(baseUrl, uriAttr)
                        when (type) {
                            "AUDIO" -> {
                                audioTracks.add(
                                    AudioTrack(
                                        name = name,
                                        language = lang,
                                        channels = attrs["CHANNELS"],
                                        url = resolvedUri
                                    )
                                )
                            }
                            "SUBTITLES" -> {
                                subtitles.add(
                                    SubtitleTrack(
                                        name = name,
                                        language = lang ?: "und",
                                        url = resolvedUri
                                    )
                                )
                            }
                        }
                    }
                }
                !line.startsWith("#") && currentStreamInf != null -> {
                    // This is the URI for the preceding #EXT-X-STREAM-INF
                    val variantUrl = resolveUrl(baseUrl, line)
                    val bandwidth = currentStreamInf["BANDWIDTH"]?.toLongOrNull()
                        ?: currentStreamInf["AVERAGE-BANDWIDTH"]?.toLongOrNull() ?: 0L
                    val resolution = currentStreamInf["RESOLUTION"] // e.g. "1920x1080"
                    val codecs = currentStreamInf["CODECS"]?.replace("\"", "")
                    val frameRate = currentStreamInf["FRAME-RATE"]?.toDoubleOrNull() ?: 0.0

                    var width = 0
                    var height = 0
                    if (resolution != null && resolution.contains("x")) {
                        val parts = resolution.split("x")
                        width = parts.getOrNull(0)?.toIntOrNull() ?: 0
                        height = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    }

                    val qualityLabel = when {
                        height >= 2160 -> "4K 2160p"
                        height >= 1440 -> "2K 1440p"
                        height >= 1080 -> "1080p FHD"
                        height >= 720 -> "720p HD"
                        height >= 480 -> "480p SD"
                        height >= 360 -> "360p"
                        height >= 240 -> "240p"
                        bandwidth > 0 -> "${bandwidth / 1000}k"
                        else -> "Default"
                    }

                    // Separate video and audio codecs if possible
                    var videoCodec: String? = null
                    var audioCodec: String? = null
                    if (codecs != null) {
                        val codecList = codecs.split(",").map { it.trim() }
                        for (c in codecList) {
                            when {
                                c.startsWith("avc") || c.startsWith("h264") || c.startsWith("h265") ||
                                c.startsWith("hev") || c.startsWith("vp9") || c.startsWith("av01") -> {
                                    videoCodec = c
                                }
                                c.startsWith("mp4a") || c.startsWith("aac") || c.startsWith("ac-3") ||
                                c.startsWith("ec-3") || c.startsWith("opus") -> {
                                    audioCodec = c
                                }
                            }
                        }
                    }

                    variants.add(
                        MediaSource(
                            url = variantUrl,
                            format = "HLS (m3u8)",
                            quality = qualityLabel,
                            resolution = resolution,
                            width = width,
                            height = height,
                            bitrate = bandwidth,
                            fps = frameRate,
                            videoCodec = videoCodec,
                            audioCodec = audioCodec,
                            isMasterPlaylist = false,
                            isHls = true
                        )
                    )
                    currentStreamInf = null
                }
            }
        }

        // Sort variants by bandwidth/height descending
        variants.sortByDescending { it.height * 10000000L + it.bitrate }

        return HlsPlaylistResult(
            isMaster = true,
            variants = variants,
            audioTracks = audioTracks,
            subtitles = subtitles
        )
    }

    private fun parseMediaPlaylist(lines: List<String>, baseUrl: String): HlsPlaylistResult {
        val segments = mutableListOf<HlsSegment>()
        var targetDuration = 0.0
        var totalDuration = 0.0
        var isLive = true
        var currentDuration = 0.0
        var currentKeyMethod: String? = null
        var currentKeyUrl: String? = null
        var currentKeyIv: ByteArray? = null
        var segmentIndex = 0

        for (line in lines) {
            when {
                line.startsWith("#EXT-X-TARGETDURATION:") -> {
                    targetDuration = line.removePrefix("#EXT-X-TARGETDURATION:").toDoubleOrNull() ?: 0.0
                }
                line.startsWith("#EXT-X-ENDLIST") -> {
                    isLive = false
                }
                line.startsWith("#EXT-X-KEY:") -> {
                    val attrs = parseAttributes(line.removePrefix("#EXT-X-KEY:"))
                    currentKeyMethod = attrs["METHOD"]
                    val keyUri = attrs["URI"]?.replace("\"", "")
                    currentKeyUrl = if (!keyUri.isNullOrEmpty()) resolveUrl(baseUrl, keyUri) else null
                    val ivStr = attrs["IV"]
                    if (ivStr != null && ivStr.startsWith("0x", ignoreCase = true)) {
                        currentKeyIv = hexStringToByteArray(ivStr.substring(2))
                    }
                }
                line.startsWith("#EXTINF:") -> {
                    val rawDuration = line.removePrefix("#EXTINF:").split(",")[0].trim()
                    currentDuration = rawDuration.toDoubleOrNull() ?: 0.0
                }
                !line.startsWith("#") -> {
                    if (currentDuration > 0 || line.isNotEmpty()) {
                        val segmentUrl = resolveUrl(baseUrl, line)
                        segments.add(
                            HlsSegment(
                                index = segmentIndex++,
                                durationSeconds = currentDuration,
                                url = segmentUrl,
                                keyMethod = currentKeyMethod,
                                keyUrl = currentKeyUrl,
                                keyIv = currentKeyIv
                            )
                        )
                        totalDuration += currentDuration
                        currentDuration = 0.0
                    }
                }
            }
        }

        return HlsPlaylistResult(
            isMaster = false,
            segments = segments,
            targetDuration = targetDuration,
            totalDurationSeconds = totalDuration,
            isLive = isLive
        )
    }

    fun parseAttributes(attrString: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val regex = Regex("""([A-Z0-9\-]+)=("([^"]*)"|([^",]+))""")
        val matches = regex.findAll(attrString)
        for (match in matches) {
            val key = match.groupValues[1]
            val value = match.groupValues[3].ifEmpty { match.groupValues[4] }
            result[key] = value
        }
        return result
    }

    fun resolveUrl(baseUrl: String, relativeOrAbsoluteUrl: String): String {
        return try {
            val trimmed = relativeOrAbsoluteUrl.trim()
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                val base = URI(baseUrl)
                base.resolve(trimmed).toString()
            }
        } catch (_: Exception) {
            relativeOrAbsoluteUrl
        }
    }

    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) +
                    Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
