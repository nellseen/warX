package com.example.ui.viewmodel

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.WarXApp
import com.example.data.DownloadEntity
import com.example.data.DownloadState
import com.example.downloader.DownloadManager
import com.example.model.ExtractionStage
import com.example.model.MediaSource
import com.example.model.VideoMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface ExtractionUiState {
    data object Idle : ExtractionUiState
    data class Extracting(val stage: ExtractionStage, val detail: String) : ExtractionUiState
    data class Success(val metadata: VideoMetadata) : ExtractionUiState
    data class Error(val message: String) : ExtractionUiState
}

class ExtractionViewModel : ViewModel() {

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _uiState = MutableStateFlow<ExtractionUiState>(ExtractionUiState.Idle)
    val uiState: StateFlow<ExtractionUiState> = _uiState.asStateFlow()

    private val _selectedSource = MutableStateFlow<MediaSource?>(null)
    val selectedSource: StateFlow<MediaSource?> = _selectedSource.asStateFlow()

    private val _previewVideoUrl = MutableStateFlow<Pair<String, String>?>(null)
    val previewVideoUrl: StateFlow<Pair<String, String>?> = _previewVideoUrl.asStateFlow()

    fun onUrlChanged(newUrl: String) {
        _urlInput.value = newUrl
    }

    fun clearUrl() {
        _urlInput.value = ""
        _uiState.value = ExtractionUiState.Idle
        _selectedSource.value = null
    }

    fun selectSource(source: MediaSource) {
        _selectedSource.value = source
    }

    fun openPreview(url: String, title: String) {
        _previewVideoUrl.value = Pair(url, title)
    }

    fun closePreview() {
        _previewVideoUrl.value = null
    }

    fun startExtraction() {
        val url = _urlInput.value.trim()
        if (url.isBlank()) {
            _uiState.value = ExtractionUiState.Error("Silakan masukkan URL halaman video terlebih dahulu.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ExtractionUiState.Extracting(ExtractionStage.INITIALIZING, "Memulai analisis...")
            val result = WarXApp.instance.extractionEngine.extract(url) { stage, detail ->
                _uiState.value = ExtractionUiState.Extracting(stage, detail)
            }

            result.onSuccess { metadata ->
                _uiState.value = ExtractionUiState.Success(metadata)
                // Default to best quality variant
                _selectedSource.value = metadata.sources.firstOrNull { !it.isMasterPlaylist }
                    ?: metadata.sources.firstOrNull()
            }.onFailure { err ->
                _uiState.value = ExtractionUiState.Error(
                    err.localizedMessage ?: "Gagal menganalisis halaman. Pastikan URL valid dan dapat diakses."
                )
            }
        }
    }

    fun startDownload(
        context: Context,
        metadata: VideoMetadata,
        source: MediaSource,
        customTitle: String? = null,
        onStarted: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val title = customTitle?.takeIf { it.isNotBlank() } ?: metadata.title
            val isHls = source.isHls
            val ext = if (isHls) ".ts" else ".mp4"
            val sanitizedName = DownloadManager.sanitizeFileName(title, ext)

            // Get target storage directory (app movies or external downloads)
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: File(context.filesDir, "downloads").apply { mkdirs() }

            val targetFile = File(downloadDir, sanitizedName)

            val downloadItem = DownloadEntity(
                originalUrl = metadata.pageUrl,
                mediaUrl = source.url,
                title = title,
                thumbnailUrl = metadata.thumbnailUrl,
                quality = source.quality,
                format = source.format,
                filePath = targetFile.absolutePath,
                totalBytes = source.sizeBytes,
                state = DownloadState.PENDING,
                isHls = isHls,
                totalSegments = source.totalSegments,
                durationSeconds = metadata.durationSeconds ?: 0.0
            )

            val id = WarXApp.instance.downloadRepository.insertDownload(downloadItem)
            WarXApp.instance.downloadManager.startDownload(id)
            onStarted(id)
        }
    }
}
