package com.example.model

data class MediaSource(
    val id: String = java.util.UUID.randomUUID().toString(),
    val url: String,
    val format: String, // "HLS (m3u8)", "MP4", "WEBM", "MKV", etc.
    val quality: String, // "1080p", "720p", "480p", "360p", "Original"
    val resolution: String? = null, // e.g. "1920x1080"
    val width: Int = 0,
    val height: Int = 0,
    val bitrate: Long = 0L, // bps
    val fps: Double = 0.0,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val sizeBytes: Long = 0L,
    val isMasterPlaylist: Boolean = false,
    val isHls: Boolean = false,
    val totalSegments: Int = 0,
    val headers: Map<String, String> = emptyMap()
)

data class SubtitleTrack(
    val name: String,
    val language: String,
    val url: String
)

data class AudioTrack(
    val name: String,
    val language: String? = null,
    val channels: String? = null,
    val url: String
)

data class VideoMetadata(
    val pageUrl: String,
    val title: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val author: String? = null,
    val domain: String,
    val durationSeconds: Double? = null,
    val durationFormatted: String? = null,
    val uploadDate: String? = null,
    val sources: List<MediaSource> = emptyList(),
    val subtitles: List<SubtitleTrack> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
    val extractedAt: Long = System.currentTimeMillis()
)

enum class ExtractionStage(val message: String) {
    INITIALIZING("Memulai extraction engine..."),
    FETCHING_PAGE("Mengambil konten halaman web..."),
    ANALYZING_DOM("Memeriksa tag video, OpenGraph & JSON-LD..."),
    SCANNING_SCRIPTS("Menganalisis player config & embedded JS..."),
    DISCOVERING_STREAMS("Menemukan media stream URL..."),
    PARSING_HLS("Parsing HLS manifest & variant playlist..."),
    PROBING_METADATA("Memeriksa codec, resolusi & file size..."),
    COMPLETED("Ekstraksi berhasil!"),
    FAILED("Ekstraksi gagal")
}
