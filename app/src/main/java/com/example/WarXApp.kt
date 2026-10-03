package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.DownloadRepository
import com.example.downloader.DownloadManager
import com.example.extractor.ExtractionEngine

class WarXApp : Application() {

    companion object {
        lateinit var instance: WarXApp
            private set
    }

    val database by lazy { AppDatabase.getInstance(this) }
    val downloadRepository by lazy { DownloadRepository(database.downloadDao()) }
    val downloadManager by lazy { DownloadManager.getInstance(this) }
    var extractionEngine: ExtractionEngine = ExtractionEngine()
        private set

    fun updateExtractionEngineUserAgent(userAgent: String) {
        extractionEngine = ExtractionEngine(userAgent)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
