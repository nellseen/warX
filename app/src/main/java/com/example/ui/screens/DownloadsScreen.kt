package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.DownloadEntity
import com.example.data.DownloadState
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonBadge
import com.example.ui.components.VideoPlayerModal
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.WarXCyan
import com.example.ui.theme.WarXViolet
import com.example.ui.viewmodel.DownloadFilterTab
import com.example.ui.viewmodel.DownloadsViewModel
import java.io.File

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Daftar proses unduhan video aktif dan riwayat file tersimpan",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.65f)
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Segmented Tabs
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = Color(0xFF111827).copy(alpha = 0.6f),
            contentColor = WarXCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = WarXCyan,
                    height = 3.dp
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == DownloadFilterTab.ALL,
                onClick = { viewModel.selectTab(DownloadFilterTab.ALL) },
                text = { Text("Semua (${allDownloads.size})", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == DownloadFilterTab.ACTIVE,
                onClick = { viewModel.selectTab(DownloadFilterTab.ACTIVE) },
                text = { Text("Aktif (${activeDownloads.size})", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == DownloadFilterTab.COMPLETED,
                onClick = { viewModel.selectTab(DownloadFilterTab.COMPLETED) },
                text = { Text("Selesai (${completedDownloads.size})", fontSize = 12.sp) }
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
                            tint = WarXCyan.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tidak Ada Unduhan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (selectedTab) {
                                DownloadFilterTab.ACTIVE -> "Saat ini tidak ada video yang sedang diunduh."
                                DownloadFilterTab.COMPLETED -> "Belum ada video yang selesai diunduh."
                                else -> "Belum ada riwayat unduhan. Mulai dari tab Beranda."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 96.dp),
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
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = Color(0xFF0F172A),
            title = {
                Text("Hapus Unduhan?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus '${target.title}' beserta file fisiknya?",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDownload(target, deleteFile = true)
                        itemToDelete = null
                    }
                ) {
                    Text("Hapus", color = StatusError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Batal", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
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
    val progress = (item.progressPercent / 100f).coerceIn(0f, 1f)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        borderColor = when (item.state) {
            DownloadState.DOWNLOADING -> WarXCyan.copy(alpha = 0.7f)
            DownloadState.COMPLETED -> StatusSuccess.copy(alpha = 0.5f)
            DownloadState.FAILED -> StatusError.copy(alpha = 0.5f)
            DownloadState.PAUSED -> StatusWarning.copy(alpha = 0.5f)
            else -> GlassBorderSubtle
        }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Thumbnail/Icon, Title, State Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Video Poster / Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(10.dp)),
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
                            tint = WarXCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NeonBadge(
                            text = item.quality,
                            color = WarXCyan
                        )
                        NeonBadge(
                            text = item.format,
                            color = if (item.isHls) WarXViolet else WarXCyan
                        )
                        // Status badge
                        val (statusText, statusColor) = when (item.state) {
                            DownloadState.DOWNLOADING -> Pair("Mengunduh", WarXCyan)
                            DownloadState.PAUSED -> Pair("Dijeda", StatusWarning)
                            DownloadState.COMPLETED -> Pair("Selesai", StatusSuccess)
                            DownloadState.FAILED -> Pair("Gagal", StatusError)
                            DownloadState.CANCELLED -> Pair("Dibatalkan", Color.Gray)
                            DownloadState.PENDING -> Pair("Menunggu", Color.LightGray)
                        }
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }
                }

                // Delete Action
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar & Live Stats when in active states
            if (item.state == DownloadState.DOWNLOADING || item.state == DownloadState.PAUSED || item.state == DownloadState.PENDING) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (item.state == DownloadState.PAUSED) StatusWarning else WarXCyan,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Downloaded bytes / segments
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
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    // Speed & ETA
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
                            color = WarXCyan
                        )
                    }
                }
            }

            // Error message if failed
            if (item.state == DownloadState.FAILED && !item.errorMessage.isNullOrBlank()) {
                Text(
                    text = item.errorMessage,
                    fontSize = 11.sp,
                    color = StatusError,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Completed metadata
            if (item.state == DownloadState.COMPLETED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ukuran: ${formatBytes(item.downloadedBytes)}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Tersimpan",
                        fontSize = 11.sp,
                        color = StatusSuccess,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (item.state) {
                    DownloadState.DOWNLOADING -> {
                        TextButton(onClick = onPause) {
                            Icon(Icons.Default.Pause, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Jeda", color = StatusWarning, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = StatusError, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Batal", color = StatusError, fontSize = 12.sp)
                        }
                    }
                    DownloadState.PAUSED -> {
                        TextButton(onClick = onResume) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = WarXCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lanjutkan", color = WarXCyan, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = StatusError, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Batal", color = StatusError, fontSize = 12.sp)
                        }
                    }
                    DownloadState.FAILED -> {
                        TextButton(onClick = onRetry) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = WarXCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Coba Lagi", color = WarXCyan, fontSize = 12.sp)
                        }
                    }
                    DownloadState.COMPLETED -> {
                        TextButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bagi", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(onClick = onPlay) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = WarXCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Putar Video", color = WarXCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
