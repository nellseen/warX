package com.example

import com.example.downloader.DownloadManager
import com.example.hls.HlsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testHlsMasterPlaylistParsing() {
        val masterContent = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-STREAM-INF:BANDWIDTH=1280000,AVERAGE-BANDWIDTH=1000000,RESOLUTION=1280x720,FRAME-RATE=29.970,CODECS="avc1.64001f,mp4a.40.2"
            gear4/prog_index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=2560000,RESOLUTION=1920x1080,FRAME-RATE=29.970,CODECS="avc1.640028,mp4a.40.2"
            gear5/prog_index.m3u8
        """.trimIndent()

        val result = HlsParser.parse(masterContent, "https://example.com/video/master.m3u8")
        assertTrue(result.isMaster)
        assertEquals(2, result.variants.size)

        // Sorted by quality/height descending: gear5 (1080p) should be first
        val firstVariant = result.variants[0]
        assertEquals("1080p FHD", firstVariant.quality)
        assertEquals("1920x1080", firstVariant.resolution)
        assertEquals("https://example.com/video/gear5/prog_index.m3u8", firstVariant.url)

        val secondVariant = result.variants[1]
        assertEquals("720p HD", secondVariant.quality)
        assertEquals("https://example.com/video/gear4/prog_index.m3u8", secondVariant.url)
    }

    @Test
    fun testHlsMediaPlaylistParsing() {
        val mediaContent = """
            #EXTM3U
            #EXT-X-TARGETDURATION:10
            #EXT-X-MEDIA-SEQUENCE:0
            #EXTINF:9.009,
            segment0.ts
            #EXTINF:9.009,
            segment1.ts
            #EXTINF:3.003,
            segment2.ts
            #EXT-X-ENDLIST
        """.trimIndent()

        val result = HlsParser.parse(mediaContent, "https://example.com/stream/index.m3u8")
        assertFalse(result.isMaster)
        assertFalse(result.isLive)
        assertEquals(3, result.segments.size)
        assertEquals("https://example.com/stream/segment0.ts", result.segments[0].url)
        assertEquals(21.021, result.totalDurationSeconds, 0.01)
    }

    @Test
    fun testSanitizeFileName() {
        val raw = "My Video: With / Invalid * Characters? <Title>"
        val clean = DownloadManager.sanitizeFileName(raw, ".mp4")
        assertEquals("My Video_ With _ Invalid _ Characters_ _Title_.mp4", clean)
    }
}
