package com.example.data

import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val downloadDao: DownloadDao) {

    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()
    val activeDownloads: Flow<List<DownloadEntity>> = downloadDao.getActiveDownloads()
    val completedDownloads: Flow<List<DownloadEntity>> = downloadDao.getCompletedDownloads()

    fun observeDownload(id: Long): Flow<DownloadEntity?> = downloadDao.observeDownloadById(id)

    suspend fun getDownload(id: Long): DownloadEntity? = downloadDao.getDownloadById(id)

    suspend fun insertDownload(download: DownloadEntity): Long = downloadDao.insert(download)

    suspend fun updateDownload(download: DownloadEntity) = downloadDao.update(download)

    suspend fun deleteDownload(id: Long) = downloadDao.deleteById(id)

    suspend fun updateState(id: Long, state: DownloadState) = downloadDao.updateState(id, state)
}
