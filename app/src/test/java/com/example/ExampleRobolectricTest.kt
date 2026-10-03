package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppThemeMode
import com.example.data.DownloadEntity
import com.example.data.DownloadState
import com.example.data.ThemePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WarX Downloader", appName)
    }

    @Test
    fun `test theme preferences persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val themePrefs = ThemePreferences.getInstance(context)

        themePrefs.setThemeMode(AppThemeMode.DARK)
        assertEquals(AppThemeMode.DARK, themePrefs.themeMode.value)

        themePrefs.setThemeMode(AppThemeMode.LIGHT)
        assertEquals(AppThemeMode.LIGHT, themePrefs.themeMode.value)
    }

    @Test
    fun `test room database download repository`() = runBlocking {
        val repo = WarXApp.instance.downloadRepository

        val testItem = DownloadEntity(
            originalUrl = "https://example.com/video",
            mediaUrl = "https://example.com/stream.m3u8",
            title = "Test Video Roboelectric",
            thumbnailUrl = null,
            quality = "1080p",
            format = "HLS",
            filePath = "/tmp/test.mp4",
            totalBytes = 1000L,
            state = DownloadState.PENDING,
            isHls = true,
            totalSegments = 10
        )

        val id = repo.insertDownload(testItem)
        val loaded = repo.getDownload(id)
        assertNotNull(loaded)
        assertEquals("Test Video Roboelectric", loaded?.title)
        assertEquals(DownloadState.PENDING, loaded?.state)

        repo.deleteDownload(id)
    }
}
