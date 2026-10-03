package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.DownloadEntity
import com.example.data.DownloadState
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassDialog
import com.example.ui.components.GlassProgress
import com.example.ui.components.VideoPlayerModal
import com.example.ui.theme.WarXTheme
import com.example.ui.viewmodel.DownloadFilterTab
import com.example.ui.viewmodel.DownloadsViewModel
import java.io.File

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = WarXTheme.colors

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val allDownloads by viewModel.allDownloads.collectAsStateWithLifecycle()
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
    val completedDownloads by viewModel.completedDownloads.collectAsStateWithLifecycle()
    val playerTarget by viewModel.playerTarget.collectAsStateWithLifecycle()

    var itemToDelete by remember { mutableStateOf<DownloadEntity?>(null) }

    val displayedList = when (selectedTab) {
        DownloadFilterTab.ALL -> allDownloads
        DownloadFilterTab.ACTIVE -> activeDownloads
        DownloadFilterTab.COMPLETED -> completedDownloads
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = "Pengelola Unduhan",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Daftar proses unduhan video aktif dan riwayat file tersimpan",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Segmented Tabs
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = colors.glassCard,
            contentColor = colors.accentCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = colors.accentCyan,
                    height = 3.dp
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == DownloadFilterTab.ALL,
                onClick = { viewModel.selectTab(DownloadFilterTab.ALL) },
                text = {
                    Text(
                        text = "Semua (${allDownloads.size})",
                        fontSize = 12.sp,
                        color = if (selectedTab == DownloadFilterTab.ALL) colors.accentCyan else colors.textMuted
                    )
                }
            )
            Tab(
                selected = selectedTab == DownloadFilterTab.ACTIVE,
                onClick = { viewModel.selectTab(DownloadFilterTab.ACTIVE) },
                text = {
                    Text(
                        text = "Aktif (${activeDownloads.size})",
                        fontSize = 12.sp,
                        color = if (selectedTab == DownloadFilterTab.ACTIVE) colors.accentCyan else colors.textMuted
                    )
                }
            )
            Tab(
                selected = selectedTab == DownloadFilterTab.COMPLETED,
                onClick = { viewModel.selectTab(DownloadFilterTab.COMPLETED) },
                text = {
                    Text(
                        text = "Selesai (${completedDownloads.size})",
                        fontSize = 12.sp,
                        color = if (selectedTab == DownloadFilterTab.COMPLETED) colors.accentCyan else colors.textMuted
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (displayedList.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = colors.accentCyan.copy(alpha = 0.7f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tidak Ada Unduhan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (selectedTab) {
                                DownloadFilterTab.ACTIVE -> "Saat ini tidak ada video yang sedang diunduh."
                                DownloadFilterTab.COMPLETED -> "Belum ada video yang selesai diunduh."
                                else -> "Belum ada riwayat unduhan. Mulai dari tab Beranda."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayedList, key = { it.id }) { item ->
                    DownloadItemCard(
                        item = item,
                        onPlay = { viewModel.playLocalVideo(item.filePath, item.title) },
                        onPause = { viewModel.pauseDownload(item.id) },
                        onResume = { viewModel.resumeDownload(item.id) },
                        onCancel = { viewModel.cancelDownload(item.id) },
                        onRetry = { viewModel.retryDownload(item.id) },
                        onDelete = { itemToDelete = item },
                        onShare = { shareVideoFile(context, item.filePath, item.title) }
                    )
                }
            }
        }
    }

    // Video Player Modal
    if (playerTarget != null) {
        val (pathOrUri, title) = playerTarget!!
        VideoPlayerModal(
            videoUrl = pathOrUri,
            title = title,
            onDismiss = { viewModel.closePlayer() }
        )
    }

    // Delete Confirmation Dialog
    if (itemToDelete != null) {
        val target = itemToDelete!!
        GlassDialog(
            onDismissRequest = { itemToDelete = null },
            title = "Hapus Unduhan?",
            confirmButton = {
                GlassButton(
                    text = "Hapus File",
                    onClick = {
                        viewModel.deleteDownload(target, deleteFile = true)
                        itemToDelete = null
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Batal", color = colors.textMuted)
                }
            }
        ) {
            Text(
                text = "Apakah Anda yakin ingin menghapus '${target.title}' beserta file fisiknya?",
                color = colors.textSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun DownloadItemCard(
    item: DownloadEntity,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val colors = WarXTheme.colors
    val progress = (item.progressPercent / 100f).coerceIn(0f, 1f)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        borderColor = when (item.state) {
            DownloadState.DOWNLOADING -> colors.accentCyan.copy(alpha = 0.7f)
            DownloadState.COMPLETED -> colors.statusSuccess.copy(alpha = 0.5f)
            DownloadState.FAILED -> colors.statusError.copy(alpha = 0.5f)
            DownloadState.PAUSED -> colors.statusWarning.copy(alpha = 0.5f)
            else -> colors.glassBorderSubtle
        }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Thumbnail/Icon, Title, Badges
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.glassSurface)
                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = when (item.state) {
                                DownloadState.COMPLETED -> Icons.Default.CheckCircle
                                DownloadState.FAILED -> Icons.Default.Error
                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = null,
                            tint = colors.accentCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GlassChip(
                            text = item.quality,
                            color = colors.accentCyan
                        )
                        GlassChip(
                            text = item.format,
                            color = if (item.isHls) colors.accentViolet else colors.accentCyan
                        )
                        val (statusText, statusColor) = when (item.state) {
                            DownloadState.DOWNLOADING -> Pair("Mengunduh", colors.accentCyan)
                            DownloadState.PAUSED -> Pair("Dijeda", colors.statusWarning)
                            DownloadState.COMPLETED -> Pair("Selesai", colors.statusSuccess)
                            DownloadState.FAILED -> Pair("Gagal", colors.statusError)
                            DownloadState.CANCELLED -> Pair("Dibatalkan", colors.textMuted)
                            DownloadState.PENDING -> Pair("Menunggu", colors.textMuted)
                        }
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar & Live Stats when Active
            if (item.state == DownloadState.DOWNLOADING || item.state == DownloadState.PAUSED || item.state == DownloadState.PENDING) {
                GlassProgress(
                    progress = progress,
                    height = 6.dp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sizeInfo = if (item.isHls && item.totalSegments > 0) {
                        "Segmen ${item.downloadedSegments}/${item.totalSegments} (${formatBytes(item.downloadedBytes)})"
                    } else if (item.totalBytes > 0) {
                        "${formatBytes(item.downloadedBytes)} / ${formatBytes(item.totalBytes)}"
                    } else {
                        formatBytes(item.downloadedBytes)
                    }

                    Text(
                        text = "$sizeInfo (${(item.progressPercent).toInt()}%)",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )

                    if (item.state == DownloadState.DOWNLOADING && item.speedBytesPerSec > 0) {
                        val speedStr = formatBytes(item.speedBytesPerSec) + "/s"
                        val etaStr = if (item.etaSeconds > 0) {
                            val m = item.etaSeconds / 60
                            val s = item.etaSeconds % 60
                            "ETA: ${if (m > 0) "${m}m " else ""}${s}s"
                        } else ""
                        Text(
                            text = "$speedStr ${if (etaStr.isNotEmpty()) "• $etaStr" else ""}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentCyan
                        )
                    }
                }
            }

            // Error info if failed
            if (item.state == DownloadState.FAILED && !item.errorMessage.isNullOrBlank()) {
                Text(
                    text = item.errorMessage,
                    fontSize = 11.sp,
                    color = colors.statusError,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Completed info
            if (item.state == DownloadState.COMPLETED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ukuran: ${formatBytes(item.downloadedBytes)}",
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                    Text(
                        text = "Tersimpan",
                        fontSize = 11.sp,
                        color = colors.statusSuccess,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (item.state) {
                    DownloadState.DOWNLOADING -> {
                        TextButton(onClick = onPause) {
                            Icon(Icons.Default.Pause, contentDescription = null, tint = colors.statusWarning, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Jeda", color = colors.statusWarning, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = colors.statusError, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Batal", color = colors.statusError, fontSize = 12.sp)
                        }
                    }
                    DownloadState.PAUSED -> {
                        TextButton(onClick = onResume) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = colors.accentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lanjutkan", color = colors.accentCyan, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = colors.statusError, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Batal", color = colors.statusError, fontSize = 12.sp)
                        }
                    }
                    DownloadState.FAILED -> {
                        TextButton(onClick = onRetry) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.accentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Coba Lagi", color = colors.accentCyan, fontSize = 12.sp)
                        }
                    }
                    DownloadState.COMPLETED -> {
                        TextButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bagi", color = colors.textSecondary, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(onClick = onPlay) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = colors.accentCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Putar Video", color = colors.accentCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

fun shareVideoFile(context: Context, filePath: String, title: String) {
    try {
        val file = File(filePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Video"))
    } catch (_: Exception) {}
}
