package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.WarXApp
import com.example.data.DownloadEntity
import com.example.data.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class DownloadFilterTab {
    ALL,
    ACTIVE,
    COMPLETED
}

class DownloadsViewModel : ViewModel() {

    private val repository: DownloadRepository = WarXApp.instance.downloadRepository

    val allDownloads: StateFlow<List<DownloadEntity>> = repository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloads: StateFlow<List<DownloadEntity>> = repository.activeDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedDownloads: StateFlow<List<DownloadEntity>> = repository.completedDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTab = MutableStateFlow(DownloadFilterTab.ALL)
    val selectedTab: StateFlow<DownloadFilterTab> = _selectedTab.asStateFlow()

    private val _playerTarget = MutableStateFlow<Pair<String, String>?>(null)
    val playerTarget: StateFlow<Pair<String, String>?> = _playerTarget.asStateFlow()

    fun selectTab(tab: DownloadFilterTab) {
        _selectedTab.value = tab
    }

    fun playLocalVideo(filePath: String, title: String) {
        val file = File(filePath)
        if (file.exists()) {
            _playerTarget.value = Pair(file.toURI().toString(), title)
        }
    }

    fun closePlayer() {
        _playerTarget.value = null
    }

    fun pauseDownload(id: Long) {
        WarXApp.instance.downloadManager.pauseDownload(id)
    }

    fun resumeDownload(id: Long) {
        WarXApp.instance.downloadManager.startDownload(id)
    }

    fun cancelDownload(id: Long) {
        WarXApp.instance.downloadManager.cancelDownload(id)
    }

    fun retryDownload(id: Long) {
        WarXApp.instance.downloadManager.retryDownload(id)
    }

    fun deleteDownload(download: DownloadEntity, deleteFile: Boolean = true) {
        viewModelScope.launch {
            if (deleteFile) {
                try {
                    val file = File(download.filePath)
                    if (file.exists()) file.delete()
                    val part = File("${download.filePath}.part")
                    if (part.exists()) part.delete()
                } catch (_: Exception) {}
            }
            repository.deleteDownload(download.id)
        }
    }
}
