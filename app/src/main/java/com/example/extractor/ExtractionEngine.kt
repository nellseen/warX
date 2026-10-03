package com.example.extractor

import android.net.Uri
import com.example.hls.HlsParser
import com.example.model.AudioTrack
import com.example.model.ExtractionStage
import com.example.model.MediaSource
import com.example.model.SubtitleTrack
import com.example.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class ExtractionEngine(
    private val userAgent: String = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro Build/UQ1A.240205.004) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.119 Mobile Safari/537.36"
) {

    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val list = cookieStore.getOrPut(url.host) { mutableListOf() }
            list.removeAll { existing -> cookies.any { it.name == existing.name } }
            list.addAll(cookies)
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun extract(
        inputUrl: String,
        onStage: (ExtractionStage, String) -> Unit = { _, _ -> }
    ): Result<VideoMetadata> = withContext(Dispatchers.IO) {
        try {
            onStage(ExtractionStage.INITIALIZING, "Memvalidasi URL target...")
            val cleanUrl = inputUrl.trim()
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                return@withContext Result.failure(IllegalArgumentException("URL tidak valid. Harap gunakan URL yang diawali http:// atau https://"))
            }

            val domain = try {
                val targetUri = URI(cleanUrl)
                targetUri.host ?: cleanUrl
            } catch (_: Exception) {
                cleanUrl.substringAfter("://").substringBefore("/")
            }

            // 1. Check if the URL itself is already a direct media stream or HLS
            if (isDirectMediaUrl(cleanUrl)) {
                onStage(ExtractionStage.DISCOVERING_STREAMS, "Mendeteksi direct stream URL...")
                return@withContext extractDirectStream(cleanUrl, domain, onStage)
            }

            // 2. Fetch page HTML
            onStage(ExtractionStage.FETCHING_PAGE, "Mengunduh konten HTML dari $domain...")
            val request = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9,id;q=0.8")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-User", "?1")
                .build()

            val response = client.newCall(request).execute()
            val finalUrl = response.request.url.toString()
            val contentType = response.header("Content-Type", "")?.lowercase() ?: ""

            // Check if redirect ended at a media file
            if (contentType.contains("application/vnd.apple.mpegurl") ||
                contentType.contains("application/x-mpegurl") ||
                contentType.contains("video/") ||
                isDirectMediaUrl(finalUrl)
            ) {
                response.close()
                return@withContext extractDirectStream(finalUrl, domain, onStage)
            }

            val htmlBody = response.body?.string().orEmpty()
            response.close()

            if (htmlBody.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Halaman kosong atau tidak dapat diakses."))
            }

            // 3. Parse HTML DOM
            onStage(ExtractionStage.ANALYZING_DOM, "Membedah struktur DOM & OpenGraph metadata...")
            val doc = Jsoup.parse(htmlBody, finalUrl)

            // Extract basic metadata
            val pageTitle = extractTitle(doc, finalUrl)
            val description = extractDescription(doc)
            val thumbnailUrl = extractThumbnail(doc, finalUrl)
            val author = extractAuthor(doc)
            val uploadDate = extractUploadDate(doc)

            val candidateUrls = mutableSetOf<String>()
            val subtitles = mutableListOf<SubtitleTrack>()
            val audioTracks = mutableListOf<AudioTrack>()

            // A. Look for video tags & source tags
            for (video in doc.select("video")) {
                video.attr("src").takeIf { it.isNotBlank() }?.let { candidateUrls.add(resolve(finalUrl, it)) }
                for (source in video.select("source")) {
                    source.attr("src").takeIf { it.isNotBlank() }?.let { candidateUrls.add(resolve(finalUrl, it)) }
                }
                for (track in video.select("track[kind=subtitles], track[kind=captions]")) {
                    val trackSrc = track.attr("src")
                    if (trackSrc.isNotBlank()) {
                        subtitles.add(
                            SubtitleTrack(
                                name = track.attr("label").ifBlank { track.attr("srclang").ifBlank { "Subtitles" } },
                                language = track.attr("srclang").ifBlank { "und" },
                                url = resolve(finalUrl, trackSrc)
                            )
                        )
                    }
                }
            }

            // B. OpenGraph & Twitter video tags
            doc.select("meta[property=og:video], meta[property=og:video:url], meta[property=og:video:secure_url], meta[name=twitter:player:stream]")
                .forEach { meta ->
                    val content = meta.attr("content").trim()
                    if (content.isNotBlank()) {
                        candidateUrls.add(resolve(finalUrl, content))
                    }
                }

            // C. JSON-LD structured data
            onStage(ExtractionStage.SCANNING_SCRIPTS, "Menganalisis JSON-LD & Player Configs...")
            extractFromJsonLd(doc, finalUrl, candidateUrls)

            // D. Embedded scripts & player configs
            extractFromScripts(doc, finalUrl, candidateUrls)

            // E. Look for iframes (e.g. embedded video players)
            val iframes = doc.select("iframe[src]")
            for (iframe in iframes) {
                val iframeSrc = iframe.attr("src").trim()
                if (iframeSrc.isNotBlank() && isPotentialPlayerIframe(iframeSrc)) {
                    val resolvedIframeUrl = resolve(finalUrl, iframeSrc)
                    try {
                        onStage(ExtractionStage.SCANNING_SCRIPTS, "Menelusuri iframe player: ${Uri.parse(resolvedIframeUrl).host}...")
                        extractFromIframe(resolvedIframeUrl, finalUrl, candidateUrls)
                    } catch (_: Exception) {
                        // Continue even if an iframe fails
                    }
                }
            }

            // F. Deep regex scanning on raw HTML for m3u8 and video files
            val regexPatterns = listOf(
                Pattern.compile("""["'](https?://[^"'\s\\]+?\.(?:m3u8|mp4|webm|mkv)(?:\?[^"'\s\\]*)?)["']""", Pattern.CASE_INSENSITIVE),
                Pattern.compile("""["'](/[a-zA-Z0-9_\-\./]+?\.(?:m3u8|mp4|webm|mkv)(?:\?[^"'\s\\]*)?)["']""", Pattern.CASE_INSENSITIVE),
                Pattern.compile("""(?:file|source|url|stream|hls|src)\s*:\s*["']([^"'\s\\]+?\.(?:m3u8|mp4|webm|mkv)[^"'\s\\]*)["']""", Pattern.CASE_INSENSITIVE),
                Pattern.compile("""(?:source|src|file)\s*=\s*["']([^"'\s\\]+?\.(?:m3u8|mp4|webm|mkv)[^"'\s\\]*)["']""", Pattern.CASE_INSENSITIVE)
            )

            for (pattern in regexPatterns) {
                val matcher = pattern.matcher(htmlBody)
                while (matcher.find()) {
                    val matchedUrl = cleanExtractedString(matcher.group(1).orEmpty())
                    if (matchedUrl.isNotBlank()) {
                        candidateUrls.add(resolve(finalUrl, matchedUrl))
                    }
                }
            }

            // Unpack Dean Edwards packed scripts if present
            extractFromPackedScripts(htmlBody, finalUrl, candidateUrls)

            if (candidateUrls.isEmpty()) {
                return@withContext Result.failure(
                    NoSuchElementException("Tidak ditemukan media stream video pada halaman ini. Pastikan halaman memiliki video yang dapat diputar secara publik.")
                )
            }

            // 4. Probing discovered media URLs & HLS parsing
            onStage(ExtractionStage.DISCOVERING_STREAMS, "Ditemukan ${candidateUrls.size} kandidat stream, menganalisis kualitas...")
            val finalSources = mutableListOf<MediaSource>()
            var foundDuration: Double? = null

            for (mediaUrl in candidateUrls) {
                try {
                    val probeResult = probeMediaUrl(mediaUrl, finalUrl, onStage)
                    if (probeResult != null) {
                        finalSources.addAll(probeResult.sources)
                        if (foundDuration == null && probeResult.durationSeconds > 0) {
                            foundDuration = probeResult.durationSeconds
                        }
                        audioTracks.addAll(probeResult.audioTracks)
                        subtitles.addAll(probeResult.subtitles)
                    }
                } catch (_: Exception) {
                    // probe failure ignored for bad candidate
                }
            }

            if (finalSources.isEmpty()) {
                return@withContext Result.failure(
                    NoSuchElementException("Kandidat media stream ditemukan namun tidak dapat diakses atau diblokir.")
                )
            }

            // Deduplicate and order sources (highest resolution/bitrate first)
            val distinctSources = finalSources.distinctBy { it.url }.sortedWith(
                compareByDescending<MediaSource> { it.height }
                    .thenByDescending { it.bitrate }
                    .thenByDescending { it.sizeBytes }
            )

            onStage(ExtractionStage.COMPLETED, "Ekstraksi selesai! Ditemukan ${distinctSources.size} pilihan stream.")

            val metadata = VideoMetadata(
                pageUrl = cleanUrl,
                title = pageTitle,
                description = description,
                thumbnailUrl = thumbnailUrl,
                author = author,
                domain = domain,
                durationSeconds = foundDuration,
                durationFormatted = foundDuration?.let { formatDuration(it) },
                uploadDate = uploadDate,
                sources = distinctSources,
                subtitles = subtitles.distinctBy { it.url },
                audioTracks = audioTracks.distinctBy { it.url }
            )

            Result.success(metadata)
        } catch (e: Exception) {
            onStage(ExtractionStage.FAILED, e.message ?: "Terjadi kesalahan saat ekstraksi")
            Result.failure(e)
        }
    }

    private suspend fun extractDirectStream(
        streamUrl: String,
        domain: String,
        onStage: (ExtractionStage, String) -> Unit
    ): Result<VideoMetadata> {
        val probe = probeMediaUrl(streamUrl, streamUrl, onStage)
            ?: return Result.failure(IllegalStateException("Gagal memproses direct stream URL."))

        val filename = try {
            Uri.parse(streamUrl).lastPathSegment?.substringBefore("?") ?: "Direct Video"
        } catch (_: Exception) {
            "Direct Video"
        }

        val metadata = VideoMetadata(
            pageUrl = streamUrl,
            title = filename,
            domain = domain,
            durationSeconds = if (probe.durationSeconds > 0) probe.durationSeconds else null,
            durationFormatted = if (probe.durationSeconds > 0) formatDuration(probe.durationSeconds) else null,
            sources = probe.sources,
            subtitles = probe.subtitles,
            audioTracks = probe.audioTracks
        )
        onStage(ExtractionStage.COMPLETED, "Berhasil memproses direct stream!")
        return Result.success(metadata)
    }

    private data class ProbeResult(
        val sources: List<MediaSource>,
        val durationSeconds: Double = 0.0,
        val audioTracks: List<AudioTrack> = emptyList(),
        val subtitles: List<SubtitleTrack> = emptyList()
    )

    private fun probeMediaUrl(
        url: String,
        referer: String,
        onStage: (ExtractionStage, String) -> Unit
    ): ProbeResult? {
        val isLikelyHls = url.contains(".m3u8", ignoreCase = true)

        if (isLikelyHls) {
            onStage(ExtractionStage.PARSING_HLS, "Parsing HLS playlist...")
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Referer", referer)
                .build()

            val res = client.newCall(req).execute()
            if (!res.isSuccessful) {
                res.close()
                return null
            }
            val content = res.body?.string().orEmpty()
            val finalRedirectedUrl = res.request.url.toString()
            res.close()

            val parsed = HlsParser.parse(content, finalRedirectedUrl)
            if (parsed.isMaster && parsed.variants.isNotEmpty()) {
                // Master playlist found with variants!
                val variantsWithHeaders = parsed.variants.map { variant ->
                    variant.copy(
                        headers = mapOf("Referer" to referer, "User-Agent" to userAgent)
                    )
                }

                // Probe the highest variant to get duration and segment info
                val highestVariant = variantsWithHeaders.firstOrNull()
                var masterDuration = 0.0
                if (highestVariant != null) {
                    try {
                        val subReq = Request.Builder()
                            .url(highestVariant.url)
                            .header("User-Agent", userAgent)
                            .header("Referer", finalRedirectedUrl)
                            .build()
                        val subRes = client.newCall(subReq).execute()
                        if (subRes.isSuccessful) {
                            val subContent = subRes.body?.string().orEmpty()
                            val subParsed = HlsParser.parse(subContent, subRes.request.url.toString())
                            if (!subParsed.isMaster) {
                                masterDuration = subParsed.totalDurationSeconds
                            }
                        }
                        subRes.close()
                    } catch (_: Exception) {}
                }

                // Add master playlist as fallback option as well
                val masterSource = MediaSource(
                    url = finalRedirectedUrl,
                    format = "HLS Master (Auto)",
                    quality = "Auto (Adaptive)",
                    isMasterPlaylist = true,
                    isHls = true,
                    headers = mapOf("Referer" to referer, "User-Agent" to userAgent)
                )

                val allSources = listOf(masterSource) + variantsWithHeaders

                return ProbeResult(
                    sources = allSources,
                    durationSeconds = masterDuration,
                    audioTracks = parsed.audioTracks,
                    subtitles = parsed.subtitles
                )
            } else if (!parsed.isMaster && parsed.segments.isNotEmpty()) {
                // Single variant / media playlist with segments directly
                val source = MediaSource(
                    url = finalRedirectedUrl,
                    format = "HLS (m3u8)",
                    quality = "Default Stream",
                    isMasterPlaylist = false,
                    isHls = true,
                    totalSegments = parsed.segments.size,
                    headers = mapOf("Referer" to referer, "User-Agent" to userAgent)
                )
                return ProbeResult(
                    sources = listOf(source),
                    durationSeconds = parsed.totalDurationSeconds
                )
            }
        }

        // Direct Video format probe (HEAD request)
        try {
            val headReq = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", userAgent)
                .header("Referer", referer)
                .build()

            val headRes = client.newCall(headReq).execute()
            val contentLength = headRes.header("Content-Length")?.toLongOrNull() ?: 0L
            val contentType = headRes.header("Content-Type", "video/mp4")?.lowercase() ?: "video/mp4"
            val format = when {
                contentType.contains("webm") || url.contains(".webm", true) -> "WEBM"
                contentType.contains("mkv") || url.contains(".mkv", true) -> "MKV"
                contentType.contains("quicktime") || url.contains(".mov", true) -> "MOV"
                else -> "MP4"
            }
            headRes.close()

            val source = MediaSource(
                url = url,
                format = format,
                quality = "Direct Video",
                sizeBytes = contentLength,
                isMasterPlaylist = false,
                isHls = false,
                headers = mapOf("Referer" to referer, "User-Agent" to userAgent)
            )
            return ProbeResult(sources = listOf(source))
        } catch (_: Exception) {
            // Fallback: Return as basic direct source
            val source = MediaSource(
                url = url,
                format = "Direct Video",
                quality = "Direct",
                headers = mapOf("Referer" to referer, "User-Agent" to userAgent)
            )
            return ProbeResult(sources = listOf(source))
        }
    }

    private fun extractFromJsonLd(doc: Document, baseUrl: String, candidateUrls: MutableSet<String>) {
        val scripts = doc.select("script[type=application/ld+json]")
        for (script in scripts) {
            try {
                val data = script.data().trim()
                if (data.startsWith("{")) {
                    parseJsonLdObject(JSONObject(data), baseUrl, candidateUrls)
                } else if (data.startsWith("[")) {
                    val arr = JSONArray(data)
                    for (i in 0 until arr.length()) {
                        val item = arr.optJSONObject(i)
                        if (item != null) parseJsonLdObject(item, baseUrl, candidateUrls)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun parseJsonLdObject(obj: JSONObject, baseUrl: String, candidateUrls: MutableSet<String>) {
        val type = obj.optString("@type", "")
        if (type.equals("VideoObject", ignoreCase = true) || obj.has("contentUrl") || obj.has("embedUrl")) {
            val contentUrl = obj.optString("contentUrl")
            if (contentUrl.isNotBlank()) candidateUrls.add(resolve(baseUrl, contentUrl))
            val embedUrl = obj.optString("embedUrl")
            if (embedUrl.isNotBlank()) candidateUrls.add(resolve(baseUrl, embedUrl))
        }
        if (obj.has("@graph")) {
            val graph = obj.optJSONArray("@graph")
            if (graph != null) {
                for (i in 0 until graph.length()) {
                    val child = graph.optJSONObject(i)
                    if (child != null) parseJsonLdObject(child, baseUrl, candidateUrls)
                }
            }
        }
    }

    private fun extractFromScripts(doc: Document, baseUrl: String, candidateUrls: MutableSet<String>) {
        val scripts = doc.select("script:not([src])")
        for (script in scripts) {
            val text = script.data()
            if (text.isBlank()) continue

            // 1. Look for JWPlayer configs
            val jwRegex = Pattern.compile("""(?:file|sources|playlist)\s*:\s*(?:\[|\{)?\s*["']([^"']+\.(?:m3u8|mp4|webm)[^"']*)["']""", Pattern.CASE_INSENSITIVE)
            val jwMatcher = jwRegex.matcher(text)
            while (jwMatcher.find()) {
                val found = cleanExtractedString(jwMatcher.group(1).orEmpty())
                if (found.isNotBlank()) candidateUrls.add(resolve(baseUrl, found))
            }

            // 2. Look for VideoJS / Hls.js setups
            val hlsRegex = Pattern.compile("""(?:loadSource|src)\s*\(\s*["']([^"']+\.(?:m3u8|mp4|webm)[^"']*)["']""", Pattern.CASE_INSENSITIVE)
            val hlsMatcher = hlsRegex.matcher(text)
            while (hlsMatcher.find()) {
                val found = cleanExtractedString(hlsMatcher.group(1).orEmpty())
                if (found.isNotBlank()) candidateUrls.add(resolve(baseUrl, found))
            }

            // 3. Look for JSON state (window.__INITIAL_STATE__, window.__NEXT_DATA__, playerConfig)
            if (text.contains("__INITIAL_STATE__") || text.contains("__NEXT_DATA__") || text.contains("playerConfig") || text.contains("ytInitialPlayerResponse")) {
                scanDeepForMediaUrls(text, baseUrl, candidateUrls)
            }
        }
    }

    private fun extractFromIframe(iframeUrl: String, parentReferer: String, candidateUrls: MutableSet<String>) {
        val req = Request.Builder()
            .url(iframeUrl)
            .header("User-Agent", userAgent)
            .header("Referer", parentReferer)
            .build()

        val res = client.newCall(req).execute()
        if (!res.isSuccessful) {
            res.close()
            return
        }
        val iframeBody = res.body?.string().orEmpty()
        val finalIframeUrl = res.request.url.toString()
        res.close()

        val iframeDoc = Jsoup.parse(iframeBody, finalIframeUrl)
        for (video in iframeDoc.select("video, video source")) {
            val src = video.attr("src")
            if (src.isNotBlank()) candidateUrls.add(resolve(finalIframeUrl, src))
        }

        extractFromScripts(iframeDoc, finalIframeUrl, candidateUrls)
        scanDeepForMediaUrls(iframeBody, finalIframeUrl, candidateUrls)
        extractFromPackedScripts(iframeBody, finalIframeUrl, candidateUrls)
    }

    private fun extractFromPackedScripts(html: String, baseUrl: String, candidateUrls: MutableSet<String>) {
        val packedPattern = Pattern.compile("""eval\s*\(\s*function\s*\(\s*p\s*,\s*a\s*,\s*c\s*,\s*k\s*,\s*e\s*,\s*[dr]\s*\)[\s\S]*?\.split\s*\(\s*['"]\|['"]\s*\)\s*\)\s*\)""")
        val matcher = packedPattern.matcher(html)
        while (matcher.find()) {
            val packedBlock = matcher.group(0).orEmpty()
            val unpacked = unpackDeanEdwards(packedBlock)
            if (unpacked != null) {
                scanDeepForMediaUrls(unpacked, baseUrl, candidateUrls)
            }
        }
    }

    private fun unpackDeanEdwards(script: String): String? {
        return try {
            val pattern = Pattern.compile("""\}\s*\(\s*'(.*?)'\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*'(.*?)'\.split\('\|'\)""")
            val matcher = pattern.matcher(script)
            if (matcher.find()) {
                val payload = matcher.group(1).orEmpty()
                val radix = matcher.group(2)?.toIntOrNull() ?: 10
                val count = matcher.group(3)?.toIntOrNull() ?: 0
                val symtab = matcher.group(4)?.split("|") ?: emptyList()

                val wordRegex = Regex("""\b\w+\b""")
                val unpacked = wordRegex.replace(payload) { mr ->
                    val word = mr.value
                    try {
                        val index = word.toInt(radix)
                        if (index < symtab.size && symtab[index].isNotEmpty()) {
                            symtab[index]
                        } else {
                            word
                        }
                    } catch (_: Exception) {
                        word
                    }
                }
                unpacked
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun scanDeepForMediaUrls(text: String, baseUrl: String, candidateUrls: MutableSet<String>) {
        val pattern = Pattern.compile("""(?:"|')((?:https?:\\?/\\?/|/)[^"'\s\\]+?\.(?:m3u8|mp4|webm|mkv)(?:\\?[^"'\s\\]*)?)(?:"|')""", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val raw = cleanExtractedString(matcher.group(1).orEmpty())
            if (raw.isNotBlank()) {
                candidateUrls.add(resolve(baseUrl, raw))
            }
        }
    }

    private fun isPotentialPlayerIframe(src: String): Boolean {
        val lower = src.lowercase()
        return lower.contains("embed") || lower.contains("player") ||
                lower.contains("video") || lower.contains("vimeo") ||
                lower.contains("stream")
    }

    private fun isDirectMediaUrl(url: String): Boolean {
        val clean = url.substringBefore("?").lowercase()
        return clean.endsWith(".m3u8") || clean.endsWith(".mp4") ||
                clean.endsWith(".webm") || clean.endsWith(".mkv") ||
                clean.endsWith(".mov")
    }

    private fun extractTitle(doc: Document, defaultUrl: String): String {
        val ogTitle = doc.select("meta[property=og:title]").attr("content").trim()
        if (ogTitle.isNotBlank()) return ogTitle

        val twitterTitle = doc.select("meta[name=twitter:title]").attr("content").trim()
        if (twitterTitle.isNotBlank()) return twitterTitle

        val docTitle = doc.title().trim()
        if (docTitle.isNotBlank()) return docTitle

        val h1 = doc.select("h1").firstOrNull()?.text()?.trim().orEmpty()
        if (h1.isNotBlank()) return h1

        return try {
            Uri.parse(defaultUrl).lastPathSegment ?: defaultUrl
        } catch (_: Exception) {
            "Video Stream"
        }
    }

    private fun extractDescription(doc: Document): String? {
        val ogDesc = doc.select("meta[property=og:description]").attr("content").trim()
        if (ogDesc.isNotBlank()) return ogDesc

        val metaDesc = doc.select("meta[name=description]").attr("content").trim()
        if (metaDesc.isNotBlank()) return metaDesc

        val twitterDesc = doc.select("meta[name=twitter:description]").attr("content").trim()
        if (twitterDesc.isNotBlank()) return twitterDesc

        return null
    }

    private fun extractThumbnail(doc: Document, baseUrl: String): String? {
        val ogImage = doc.select("meta[property=og:image], meta[property=og:image:secure_url]").attr("content").trim()
        if (ogImage.isNotBlank()) return resolve(baseUrl, ogImage)

        val twitterImage = doc.select("meta[name=twitter:image]").attr("content").trim()
        if (twitterImage.isNotBlank()) return resolve(baseUrl, twitterImage)

        val poster = doc.select("video[poster]").attr("poster").trim()
        if (poster.isNotBlank()) return resolve(baseUrl, poster)

        val linkThumb = doc.select("link[rel=image_src]").attr("href").trim()
        if (linkThumb.isNotBlank()) return resolve(baseUrl, linkThumb)

        return null
    }

    private fun extractAuthor(doc: Document): String? {
        val author = doc.select("meta[name=author], meta[property=article:author], meta[name=channel]").attr("content").trim()
        if (author.isNotBlank()) return author
        return null
    }

    private fun extractUploadDate(doc: Document): String? {
        val date = doc.select("meta[property=article:published_time], meta[itemprop=uploadDate], meta[name=uploadDate]").attr("content").trim()
        if (date.isNotBlank()) return date
        return null
    }

    private fun resolve(baseUrl: String, url: String): String {
        return try {
            val unescaped = cleanExtractedString(url)
            if (unescaped.startsWith("http://") || unescaped.startsWith("https://")) {
                unescaped
            } else if (unescaped.startsWith("//")) {
                "https:$unescaped"
            } else {
                URI(baseUrl).resolve(unescaped).toString()
            }
        } catch (_: Exception) {
            url
        }
    }

    private fun cleanExtractedString(str: String): String {
        var res = str.replace("\\/", "/")
            .replace("\\u002F", "/")
            .replace("\\u0026", "&")
            .replace("&amp;", "&")
            .replace("\\\"", "\"")
            .replace("\\'", "'")
            .trim()
        if (res.startsWith("http%3A%2F%2F", ignoreCase = true) || res.startsWith("https%3A%2F%2F", ignoreCase = true)) {
            try {
                res = java.net.URLDecoder.decode(res, "UTF-8")
            } catch (_: Exception) {}
        }
        return res
    }

    private fun formatDuration(seconds: Double): String {
        val totalSecs = seconds.toLong()
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }
}
